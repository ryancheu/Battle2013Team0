package bronzeBot.Robots.Types;

import bronzeBot.Robots.HQRobot;
import bronzeBot.Robots.SoldierRobot;
import bronzeBot.Robots.HQRobot.HQState;
import bronzeBot.Robots.SoldierRobot.SoldierType;
import bronzeBot.Util.RadioChannels;
import battlecode.common.*;
import static bronzeBot.Robots.ARobot.mRC;
import static bronzeBot.Util.Constants.*;
import static bronzeBot.Util.Util.*;
public class HQNukeType {
	
	
	private static int minerCount = 0;
	private static int scoutCount = 0;
	private static int armyCount = 0;
	private static int generatorCount = 0;
	private static int supplierCount = 0;
	private static double lastPower = 0;
	private static int turnOfNuke = 0;
	private static MapLocation[] waypointsToEnemyHQ;
	private static int lastNextWaypointIndex;
	private static MapLocation encampmentInDanger;

	public static void run() throws GameActionException
	{
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		actionAllState(alliedRobots);

		switch(HQRobot.getState())
		{
		case TURTLE: {
			turtleState();
			break;
		}
		case PREPARE_ATTACK: {
			prepareAttackState();
			break;
		}
		case ATTACK: {
			attackHQState();
			break;
		}
		case RUSH: {
			rushHQState();
			break;
		}
		default:
			break;
			
		}		
	}
	
	private static void initializeRadioChannels() throws GameActionException {
		setNumberOfEncampments();
		setNumberOfMidGameEnc();
		setNumberOfPreFusionEnc();
		System.out.println("encampments: " + numEncToClaim);
		for (int i = RadioChannels.ENC_CLAIM_START; i < numEncToClaim + RadioChannels.ENC_CLAIM_START; ++i) {
			HQRobot.mRadio.writeChannel(i, ENCAMPMENT_NOT_CLAIMED);
			HQRobot.mRadio.writeChannel(i-RadioChannels.ENC_CLAIM_START + RadioChannels.ENCAMPMENT_BUILDING_START, ENCAMPMENT_NOT_CLAIMED );
		}
	}
	
	private static void performCensus() throws GameActionException {
		//Perform census
		if(Clock.getRoundNum()%CENSUS_INTERVAL == 0) {
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.LAY_MINES.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.SCOUT.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.ARMY.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES,0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES,0);
			
			
		}
		
		if (Clock.getRoundNum() == 0) {
			//TODO set behavior for game based on team memory
			initializeRadioChannels();
			
		}
		else if(Clock.getRoundNum()%CENSUS_INTERVAL == 1){
			minerCount  = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.LAY_MINES.ordinal());
			// Don't respawn scouts unless we have vision
			if(mRC.hasUpgrade(Upgrade.VISION)) {
				scoutCount  = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.SCOUT.ordinal());
			}
			armyCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.ARMY.ordinal());
			generatorCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + RobotType.GENERATOR.ordinal() + NUM_SOLDIERTYPES);
			supplierCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + RobotType.SUPPLIER.ordinal() + NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES);
			HQRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS,generatorCount);
			HQRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS,supplierCount);
		}
	}
	
	private static void updateEnemyLocationData() throws GameActionException {
		//Sense Enemy robots and broadcast average position to bots
		Robot[] enemyRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mEnemy);		
		int avgX = 0, avgY = 0, numSoldiers = 0;
		for(Robot bot:enemyRobots){
			RobotInfo info = mRC.senseRobotInfo(bot);
			if(info.type == RobotType.SOLDIER){
				numSoldiers ++;
				avgX += info.location.x;
				avgY += info.location.y;
			}
		}

		if ( numSoldiers > 0) {
			avgX /= numSoldiers;
			avgY /= numSoldiers;
			
			if ( HQRobot.enemyLastSeenPosAvg == null) {
				HQRobot.enemyLastSeenPosAvg = new MapLocation(avgX,avgY);				
			}
			else {
				int oldX = HQRobot.enemyLastSeenPosAvg.x;
				int oldY = HQRobot.enemyLastSeenPosAvg.y;
				HQRobot.enemyLastSeenPosAvg = new MapLocation((int)((avgX*AVG_POSITION_RECENT_WEIGHT + oldX)/(1f+AVG_POSITION_RECENT_WEIGHT)), 
															  (int)((avgY*AVG_POSITION_RECENT_WEIGHT + oldY)/(1f+AVG_POSITION_RECENT_WEIGHT)));
			}
		}
		else {
			avgX = HQRobot.enemyHQLoc.x;
			avgY = HQRobot.enemyHQLoc.y;
		}

		//Write the average enemy location to be used by battling units
		HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_AVG_POS, locationToIndex(new MapLocation(avgX,avgY)));
		if (HQRobot.enemyLastSeenPosAvg != null) {
			HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_LOCATION, locationToIndex(HQRobot.enemyLastSeenPosAvg));
		}
		else {
			HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_LOCATION, locationToIndex(HQRobot.enemyHQLoc));
		}
	}
	
	private static void updateScoutWayPoints() throws GameActionException {
		// Check for waypoints from our scout
		int numScoutWaypoints = HQRobot.mRadio.readChannel(RadioChannels.NUM_SCOUT_WAYPOINTS);
		if(numScoutWaypoints > 0){
			waypointsToEnemyHQ = new MapLocation[numScoutWaypoints];
			for(int n=0; n<numScoutWaypoints; ++n)
				waypointsToEnemyHQ[n] = indexToLocation(HQRobot.mRadio.readChannel(RadioChannels.SCOUT_WAYPOINTS_START + n));
			HQRobot.mRadio.writeChannel(RadioChannels.NUM_SCOUT_WAYPOINTS, 0);
		}
	}
	
	private static void actionAllState(Robot[] allies) throws GameActionException {
		
		
		//Updates the number of each unit we have 
		performCensus(); 
		//Broadcasts enemy position data to army
		updateEnemyLocationData();
		//Updates waypoints for scouts
		updateScoutWayPoints(); 
		//Check if the medbay is alive
		checkForMedbay();
		//Check for the rest of the encampments
		checkAllEncampments();
		//Check if an encampment is threatened
		checkEncampmentSafety();
		//Check if we should rush the enemy HQ
		checkShouldRush();
		
		//TODO: comment why sometimes these return and some don't
		if(mRC.isActive()){
			if ( Clock.getRoundNum()> mRC.getTeamMemory()[0] - 100) {
				mRC.researchUpgrade(Upgrade.NUKE);
				return;
			}
			if(mRC.checkResearchProgress(Upgrade.NUKE) > Upgrade.NUKE.numRounds - RUSH_NUKE_TIME) {
				// We're almost done with the nuke!
				mRC.researchUpgrade(Upgrade.NUKE);
				mRC.setIndicatorString(2, "Nuke almost done!");
				return;
			}
			if(numEncToClaim > 0 && Clock.getRoundNum() < 10){
				HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
				return;
			}
			
			if(mRC.getTeamPower() < PREFUSION_POWER_RESERVE){
				pickResearch();
				return;
			}
			for (int i = RadioChannels.ENC_CLAIM_START;
					i < RadioChannels.ENC_CLAIM_START + Math.min(numEncToClaim, NUM_PREFUSION_ENC); i++) {
				if (HQRobot.mRadio.readChannel(i) == -1) {
					HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
					return;
				}
			}
			if(minerCount < NUM_MINERS) { 
				++ minerCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.LAY_MINES);
				return;
			}
			else if(scoutCount < NUM_SCOUTS) {
				++ scoutCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.SCOUT);
				return;
			}
			else if(armyCount < NUM_ARMY_NO_FUSION){
				++ armyCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
				return;
			}
			else if (!mRC.hasUpgrade(Upgrade.FUSION)) {
				mRC.researchUpgrade(Upgrade.FUSION);
				return;
			} 
			else if (HQRobot.enemyNukeSoon && !mRC.hasUpgrade(Upgrade.DEFUSION)) {
				mRC.researchUpgrade(Upgrade.DEFUSION);
				return;
			}
			else {
				for (int i = RadioChannels.ENC_CLAIM_START;
						i < RadioChannels.ENC_CLAIM_START + midGameEncToClaim; i++) {
					if (HQRobot.mRadio.readChannel(i) == -1) {
						HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
						return;
					}
				}
				if(Clock.getRoundNum() > LATE_GAME){
					for (int i = RadioChannels.ENC_CLAIM_START;
							i < RadioChannels.ENC_CLAIM_START + numEncToClaim; i++) {
						if (HQRobot.mRadio.readChannel(i) == -1) {
							HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
							return;
						}
					}
				}
				if(armyCount < NUM_ARMY_WITH_FUSION
						&& mRC.getTeamPower() > POWER_RESERVE/* && mRC.getTeamPower() > lastPower*/) {
					++ armyCount;
					HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
					return;
				}								
				pickResearch();
			}
		}
		
		lastPower  = mRC.getTeamPower();
		
	}
	

	private static void checkShouldRush() {
		if(mRC.senseNearbyGameObjects(Robot.class, mRC.senseEnemyHQLocation(),
				HQ_ENTER_RUSH_RAD, HQRobot.mTeam).length > 0)
			HQRobot.switchState(HQState.RUSH);
	}

	private static void checkEncampmentSafety() throws GameActionException {
		int value = HQRobot.mRadio.readChannel(RadioChannels.ENCAMPMENT_IN_DANGER);
		if(value != -1) {
			encampmentInDanger = indexToLocation(value);
		}
		else {
			encampmentInDanger = null;
		}
		HQRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_IN_DANGER, -1);
	}

	private static void checkAllEncampments() throws GameActionException {
		MapLocation tempLocation;
		int tempInt;
		
		//Go through all the encampments that have been claimed and thought to be used
		//If they have been lost, change the channels to signify that
        for ( int i = RadioChannels.ENC_CLAIM_START; i < RadioChannels.ENC_CLAIM_START + numEncToClaim; i++ ) {
        	if ((tempInt = HQRobot.mRadio.readChannel(i)) != -1) {
        		tempLocation = indexToLocation(tempInt);
        		if (!mRC.canSenseSquare(tempLocation) )
        		{
        			//If we can't sense the square, check to see if the tower says it should have been built or not
        			tempInt = HQRobot.mRadio.readChannel(RadioChannels.ENCAMPMENT_BUILDING_START + i - RadioChannels.ENC_CLAIM_START);
        			if ( tempInt == ENCAMPMENT_CAPTURE_STARTED ) {
        				print ("overwriting channel: " + (RadioChannels.ENCAMPMENT_BUILDING_START + i - RadioChannels.ENC_CLAIM_START));
        				HQRobot.mRadio.writeChannel(i, ENCAMPMENT_NOT_CLAIMED);
        				HQRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START + i - RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
        			}
        		}
        	}
        }
    }
	private static void checkForMedbay() throws GameActionException {
		MapLocation medbay = indexToLocation(HQRobot.mRadio.readChannel(RadioChannels.MEDBAY_LOCATION));
		if(mRC.canSenseSquare(medbay)){
			GameObject o = mRC.senseObjectAtLocation(medbay);
			if(o != null && o.getTeam() == mRC.getTeam()
					&& mRC.senseRobotInfo((Robot) o).type == RobotType.MEDBAY)
				return;
		}
		// The medbay value was invalid, replace it with our location
		HQRobot.mRadio.writeChannel(RadioChannels.MEDBAY_LOCATION, locationToIndex(mRC.getLocation()));
		
		// If the location wasn't our location, unclaim the encampment so we try to reclaim it
		if(!medbay.equals(mRC.getLocation())){
			for (int i = RadioChannels.ENC_CLAIM_START;
					i < RadioChannels.ENC_CLAIM_START + numEncToClaim; i++) { 
				if (HQRobot.mRadio.readChannel(i) == locationToIndex(medbay)) {
					HQRobot.mRadio.writeChannel(i, -1);
				}
			}
			HQRobot.mRadio.writeChannel(RadioChannels.MEDBAY_CLAIMED, 0);
		}
	}

	private static void pickResearch() throws GameActionException {
		if (!mRC.hasUpgrade(Upgrade.FUSION)) {
			mRC.researchUpgrade(Upgrade.FUSION);
		}
		else if ( !mRC.hasUpgrade(Upgrade.DEFUSION) ) {
			mRC.researchUpgrade(Upgrade.DEFUSION);
		}
		else if ( !mRC.hasUpgrade(Upgrade.VISION)) {
			mRC.researchUpgrade(Upgrade.VISION);
		}
		else {
			mRC.researchUpgrade(Upgrade.NUKE);
		}
	}
	
	private static void turtleState() throws GameActionException {
		if (encampmentInDanger == null) {
			HQRobot.setRallyPoint(new MapLocation(
					(6*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/7,
					(6*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/7));
		}
		else {
			HQRobot.setRallyPoint(encampmentInDanger);
		}
		// Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		if(mRC.checkResearchProgress(Upgrade.NUKE) <= Upgrade.NUKE.numRounds/2 
           && mRC.senseEnemyNukeHalfDone()) {
			HQRobot.enemyNukeSoon = true;
			HQRobot.switchState(HQState.ATTACK);
		}
		else if (Clock.getRoundNum() >= ATTACK_ROUND ) {
			HQRobot.switchState(HQState.ATTACK);
		}
		else {
			HQRobot.mRadio.writeChannel(RadioChannels.SHOULD_LAY_MINES, 1);
		}
	}

	private static void prepareAttackState() throws GameActionException {
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		MapLocation preAttackRallyLocation = new MapLocation(
				(4*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/5,
				(4*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/5);
		if(Math.min(armyCount, alliedRobots.length) > NUM_ARMY_BEFORE_ATTACK)
			HQRobot.switchState(HQState.ATTACK); //attack!
		HQRobot.setRallyPoint(preAttackRallyLocation);
		
		HQRobot.mRadio.writeChannel(RadioChannels.SHOULD_LAY_MINES, 0);
	}

	private static void attackHQState() throws GameActionException {
		
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		/*
		int avgX = 0, avgY = 0, numSoldiers = 0;
		for(Robot bot:alliedRobots){
			RobotInfo info = mRC.senseRobotInfo(bot);
			if(info.type == RobotType.SOLDIER){
				numSoldiers ++;
				avgX += info.location.x;
				avgY += info.location.y;
			}
		}
		avgX /= numSoldiers;
		avgY /= numSoldiers;
		*/
		MapLocation avg = findMedianSoldier(alliedRobots);
		
		if((Math.min(armyCount, alliedRobots.length) < NUM_ARMY_BEFORE_RETREAT && (!HQRobot.enemyNukeSoon)) 
				|| (HQRobot.enemyNukeSoon && Math.min(armyCount, alliedRobots.length) < NUM_ARMY_BEFORE_ATTACK_WITH_NUKE)) 
			HQRobot.switchState(HQState.PREPARE_ATTACK);

		if(waypointsToEnemyHQ == null)
			HQRobot.setRallyPoint(mRC.senseEnemyHQLocation());
		else{
			//HQRobot.setRallyPoints(waypointsToEnemyHQ);
			int nextWaypointIndex = findNextWaypointIndex(waypointsToEnemyHQ, avg);
			if(HQRobot.enemyNukeSoon){
				if(nextWaypointIndex < waypointsToEnemyHQ.length - 1
						&& mRC.senseNearbyGameObjects(Robot.class, waypointsToEnemyHQ[nextWaypointIndex],
						32, HQRobot.mTeam).length >= NUM_ARMY_BEFORE_ATTACK_WITH_NUKE)
					++nextWaypointIndex;
			}
			if(lastNextWaypointIndex != nextWaypointIndex || HQRobot.getLastState()!=HQRobot.HQState.ATTACK) {
				HQRobot.setRallyPoints(waypointsToEnemyHQ, nextWaypointIndex+1);
				lastNextWaypointIndex = nextWaypointIndex;
			}
			//HQRobot.setRallyPoints(waypointsToEnemyHQ);
			//mRC.setIndicatorString(2, findNextWaypoint(waypointsToEnemyHQ, new MapLocation(avgX, avgY)).toString());
		}
		
		HQRobot.mRadio.writeChannel(RadioChannels.SHOULD_LAY_MINES, 0);

	}
	
	private static void rushHQState() throws GameActionException {
		if(waypointsToEnemyHQ == null)
			HQRobot.setRallyPoint(mRC.senseEnemyHQLocation());
		else {
			int nextWaypointIndex = waypointsToEnemyHQ.length - 1;
			if(lastNextWaypointIndex != nextWaypointIndex
					|| HQRobot.getLastState()!=HQRobot.HQState.RUSH) {
				HQRobot.setRallyPoints(waypointsToEnemyHQ, nextWaypointIndex+1);
				lastNextWaypointIndex = nextWaypointIndex;
			}
		}
	}
	
}



