package BaseBot.Robots.Types;

import BaseBot.Robots.ARobot;
import BaseBot.Robots.HQRobot;
import BaseBot.Robots.SoldierRobot;
import BaseBot.Robots.HQRobot.HQState;
import BaseBot.Robots.SoldierRobot.SoldierType;
import BaseBot.Util.RadioChannels;
import battlecode.common.*;
import static BaseBot.Robots.ARobot.mRC;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.NonConstants.*;
import static BaseBot.Util.NukeConstants.*;
import static BaseBot.Util.Util.*;
public class HQNukeType {
	
	
	private static int minerCount = 0;
	private static int scoutCount = 0;
	private static int armyCount = 0;
	private static int generatorCount = 0;
	private static int supplierCount = 0;
	private static double lastPower = 0;
	private static long turnOfNuke = 0;
	private static MapLocation[] waypointsToEnemyHQ;
	private static int lastNextWaypointIndex;
	private static MapLocation encampmentInDanger;
	private static boolean HQInDanger = false;
	private static SoldierType[] soldierTypes = new SoldierType[MAX_POSSIBLE_SOLDIERS];

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
	public static void setConstants() throws GameActionException{
		CHANCE_OF_DEFUSING_ENEMY_MINE = CHANCE_OF_DEFUSING_ENEMY_MINE_CONST;
		CHANCE_OF_DEFUSING_NEUTRAL_MINE =CHANCE_OF_DEFUSING_NEUTRAL_MINE_CONST;

		LAST_ROUND_SHOT_DELAY = LAST_ROUND_SHOT_DELAY_CONST;

		SOLDIER_ENEMY_CHECK_RAD = SOLDIER_ENEMY_CHECK_RAD_CONST;
		
		SOLDIER_RALLY_RAD = 		SOLDIER_RALLY_RAD_CONST;
		
		SOLDIER_OUTNUMBER_MULTIPLIER =SOLDIER_OUTNUMBER_MULTIPLIER_CONST;
		SOLDIER_RUN_HEALTH = SOLDIER_RUN_HEALTH_CONST;
		SOLDIER_RUN_EVENTUALLY_HEALTH = SOLDIER_RUN_EVENTUALLY_HEALTH_CONST;
		SOLDIER_RETURN_HEALTH = SOLDIER_RETURN_HEALTH_CONST;
		SOLDIER_BATTLE_ENEMY_CHECK_RAD = SOLDIER_BATTLE_ENEMY_CHECK_RAD_CONST;

		RATIO_OF_SUPPLIERS_OVER_GENERATORS = RATIO_OF_SUPPLIERS_OVER_GENERATORS_CONST;

		SOLDIER_ATTACK_RAD = SOLDIER_ATTACK_RAD_CONST;
		SOLDIER_JOIN_ATTACK_RAD = SOLDIER_JOIN_ATTACK_RAD_CONST;

		SCOUT_RAD_SQUARED = SCOUT_RAD_SQUARED_CONST;
		SCOUT_DIST = SCOUT_DIST_CONST;
		
		
		SCOUT_RECOMPUTE_PATH_INTERVAL = SCOUT_RECOMPUTE_PATH_INTERVAL_CONST;
		
	}
	
	private static void initializeRadioChannels() throws GameActionException {
		setConstants();
		setNumberOfEncampments();
		setNumberOfMidGameEnc();
		setNumberOfPreFusionEnc();
		setMapWidthAndHeight();
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
			mRC.setIndicatorString(0,""+mRC.getTeamMemory()[0]);
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
		//Check if THE HQ is in danger
		checkHQSafety();
		//Check if an encampment is threatened
		checkEncampmentSafety();
		//Check if we should rush the enemy HQ
		checkShouldRush();
		//Check if we spawned a new unit
	    checkNewUnitType();

		if(mRC.senseEnemyNukeHalfDone() && turnOfNuke == 0){
			turnOfNuke = Clock.getRoundNum()-200;
		}
		
		if(mRC.getEnergon()<=1 && Clock.getRoundNum()>2000){
			mRC.setTeamMemory(0,Clock.getRoundNum());
			mRC.setTeamMemory(1, 5);
		}
		else if(mRC.getEnergon()>48 && Clock.getRoundNum()>=400){
			//48 is the amount of health damage 8 guys surrounding your HQ does
			mRC.setTeamMemory(0,turnOfNuke);
			MapLocation enemyHQ = mRC.senseEnemyHQLocation();
			if(mRC.canSenseSquare(enemyHQ) 
					&& mRC.senseRobotInfo((Robot)mRC.senseObjectAtLocation(enemyHQ)).energon <= 48){
				mRC.setTeamMemory(1, 2);
				// We killed them
			}
			else if(mRC.checkResearchProgress(Upgrade.NUKE) < 399) {
				// Died to nuke
				mRC.setTeamMemory(1, 1);
			}
			else {
				// We nuked them
				mRC.setTeamMemory(1, 0);
			}
		}
		else if(mRC.getEnergon()<=48 && Clock.getRoundNum() < 400){
			mRC.setTeamMemory(0,Clock.getRoundNum());
			mRC.setTeamMemory(1, 3);
			//died to rush
		}
		else{
			mRC.setTeamMemory(0,Clock.getRoundNum());
			//died to econ
			mRC.setTeamMemory(2, 4);
		}
		//TODO: comment why sometimes these return and some don't
		if(mRC.isActive()){
			if(Clock.getRoundNum()<2000){
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
				/*

				else if (!mRC.hasUpgrade(Upgrade.FUSION)) {
					mRC.researchUpgrade(Upgrade.FUSION);
					return;
				} 
				*/
				/*
				else if (HQRobot.enemyNukeSoon && !mRC.hasUpgrade(Upgrade.DEFUSION)) {
					mRC.researchUpgrade(Upgrade.DEFUSION);
					return;
				}
				*/
				else if (mRC.hasUpgrade(Upgrade.PICKAXE) && minerCount < NUM_MINERS_WITH_PICKAXE
						&& mRC.getTeamPower() > PREFUSION_POWER_RESERVE){
					++ minerCount;
					HQRobot.spawnRobot(SoldierRobot.SoldierType.LAY_MINES);
					return;	
				}
				else {
					/*
					for (int i = RadioChannels.ENC_CLAIM_START;
							i < RadioChannels.ENC_CLAIM_START + midGameEncToClaim; i++) {
						if (HQRobot.mRadio.readChannel(i) == -1) {
							HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
							return;
						}
					}
					*/
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
			else{
				pickResearch();
			}
		}
		
		lastPower  = mRC.getTeamPower();
		
	}
	
	private static void checkNewUnitType() throws GameActionException {
		if(Clock.getRoundNum() == 0)
			HQRobot.mRadio.writeChannel(RadioChannels.NEW_UNIT_ID, -1);
		
		int value;
		if((value = HQRobot.mRadio.readChannel(RadioChannels.NEW_UNIT_ID)) != -1) {
			soldierTypes[value/SoldierType.values().length]
					= SoldierType.values()[value%SoldierType.values().length];
			HQRobot.mRadio.writeChannel(RadioChannels.NEW_UNIT_ID, -1);
		}
	}
	private static void checkShouldRush() {
		if(mRC.senseNearbyGameObjects(Robot.class, mRC.senseEnemyHQLocation(),
				HQ_ENTER_RUSH_RAD, HQRobot.mTeam).length > 0)
			HQRobot.switchState(HQState.RUSH);
	}
	
	private static void checkHQSafety() throws GameActionException {
		
		if(mRC.senseNearbyGameObjects(Robot.class,HQ_PROTECT_RAD_SQUARED,ARobot.mEnemy).length > 0){
			HQInDanger = true;
			//the only reason this is being written is to change everyone who is not already a soldier to soldier type
			HQRobot.mRadio.writeChannel(RadioChannels.HQ_IN_DANGER, 1);
		}
		else{
			HQInDanger = false;
			HQRobot.mRadio.writeChannel(RadioChannels.HQ_IN_DANGER, 0);
		}
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
		
		if(Clock.getRoundNum()%CENSUS_INTERVAL != 2) {
			return;
		}
		
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
		
		if ( !mRC.hasUpgrade(Upgrade.PICKAXE) ) {
			mRC.researchUpgrade(Upgrade.PICKAXE);
		}
		
		else {
			mRC.researchUpgrade(Upgrade.NUKE);
		}
	}
	
	private static void turtleState() throws GameActionException {

		if (encampmentInDanger == null  && !HQInDanger) {
			
			//Get all our encampment squares
			MapLocation encampmentSquares[] = mRC.senseAlliedEncampmentSquares();
			if(encampmentSquares.length>0){
				//store the furthest distance from our base
				int distSquared =0;
				//store our encampment closest to enemy base
				int leastDist=0;
				//loop through each encampment. if its distance is shorter than current least dist, replace it
				for(int i = 0;i<encampmentSquares.length;i++)
				{ 
					int temp = encampmentSquares[i].distanceSquaredTo(HQRobot.enemyHQLoc);
					if( temp<leastDist)
					{
						leastDist = temp;
						//store the location of the furthest encampment
						distSquared = i;
					}
				}
				//get distance from us to furthest encampment
				distSquared = mRC.getLocation().distanceSquaredTo(encampmentSquares[distSquared]);
				
				MapLocation rallyLoc = new MapLocation(
						(6*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/7,
						(6*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/7);
				//move our wall to a point on the line between us and the enemy base.
				//That point should be the as far from us as our farthest encampment
				if(distSquared> rallyLoc.distanceSquaredTo(mRC.getLocation()))
				{
					//get distance from us to enemy HQ
					int dist = mRC.getLocation().distanceSquaredTo(HQRobot.enemyHQLoc);
					//How far along that vector should we go?
					float move =  (float)Math.sqrt((float)distSquared/dist);
					
					HQRobot.setRallyPoint( new MapLocation(
							(int)(mRC.getLocation().x +move*(HQRobot.enemyHQLoc.x-mRC.getLocation().x) ),
							(int)(mRC.getLocation().y + move*(HQRobot.enemyHQLoc.y-mRC.getLocation().y))));
				}
				//if that distance is too short, use our old code!
				else
				{
					HQRobot.setRallyPoint(rallyLoc);
				}
			}
			else
			{
				HQRobot.setRallyPoint(new MapLocation(
						(6*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/7,
						(6*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/7));
			}
		}
		else if(HQInDanger) {
			//TODO:change everyone to army type and send them to HQ
			HQRobot.setRallyPoint(mRC.getLocation());
		}
		else if(encampmentInDanger != null){
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
		MapLocation avg = findMedianSoldier(alliedRobots, soldierTypes);
		
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



