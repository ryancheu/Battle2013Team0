package MineTurtle.Robots.Types;

import MineTurtle.Robots.HQRobot;
import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.HQRobot.HQState;
import MineTurtle.Robots.SoldierRobot.SoldierType;
import battlecode.common.*;


import static MineTurtle.Robots.ARobot.mRC;
import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;
public class HQNormalType {
	
	
	private static int minerCount = 0;
	private static int scoutCount = 0;
	private static int armyCount = 0;
	private static double lastPower = 0;
	private static MapLocation[] waypointsToEnemyHQ;

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
		default:
			break;
			
		}
		
	}
	
	private static void preformCensus() throws GameActionException {
		//Perform census
		if(Clock.getRoundNum()%CENSUS_INTERVAL == 0) {
			HQRobot.mRadio.writeChannel(COUNT_MINERS_RAD_CHAN, 0);
			HQRobot.mRadio.writeChannel(CENSUS_RAD_CHAN_START + SoldierType.LAY_MINES.ordinal(),0);
			HQRobot.mRadio.writeChannel(CENSUS_RAD_CHAN_START + SoldierType.SCOUT.ordinal(),0);
			HQRobot.mRadio.writeChannel(CENSUS_RAD_CHAN_START + SoldierType.ARMY.ordinal(),0);
			
		}
		
		if (Clock.getRoundNum() == 0) {
			setNumberOfEncampments();
			setNumberOfPreFusionEnc();
			System.out.println("encampments: " + NUM_ENC_TO_CLAIM);
			for (int i = ENC_CLAIM_RAD_CHAN_START; i < NUM_ENC_TO_CLAIM + ENC_CLAIM_RAD_CHAN_START; ++i) {
				HQRobot.mRadio.writeChannel(i, -1);				
			}
		}
		else if(Clock.getRoundNum()%CENSUS_INTERVAL == 1){
			minerCount  = HQRobot.mRadio.readChannel(CENSUS_RAD_CHAN_START + SoldierType.LAY_MINES.ordinal());
			// Don't respawn scouts unless we have vision
			if(mRC.hasUpgrade(Upgrade.VISION)) {
				scoutCount  = HQRobot.mRadio.readChannel(CENSUS_RAD_CHAN_START + SoldierType.SCOUT.ordinal());
			}
				
			armyCount = HQRobot.mRadio.readChannel(CENSUS_RAD_CHAN_START + SoldierType.ARMY.ordinal());
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
		HQRobot.mRadio.writeChannel(ENEMY_AVG_POS_RAD_CHANNEL, locationToIndex(new MapLocation(avgX,avgY)));
		if (HQRobot.enemyLastSeenPosAvg != null) {
			HQRobot.mRadio.writeChannel(ENEMY_LOCATION_CHAN, locationToIndex(HQRobot.enemyLastSeenPosAvg));
		}
		else {
			HQRobot.mRadio.writeChannel(ENEMY_LOCATION_CHAN, locationToIndex(HQRobot.enemyHQLoc));
		}
	}
	
	private static void updateScoutWayPoints() throws GameActionException {
		// Check for waypoints from our scout
		int numScoutWaypoints = HQRobot.mRadio.readChannel(NUM_SCOUT_WAYPOINTS_RAD_CHAN);
		if(numScoutWaypoints > 0){
			waypointsToEnemyHQ = new MapLocation[numScoutWaypoints];
			for(int n=0; n<numScoutWaypoints; ++n)
				waypointsToEnemyHQ[n] = indexToLocation(HQRobot.mRadio.readChannel(SCOUT_WAYPOINTS_CHAN_START + n));
			HQRobot.mRadio.writeChannel(NUM_SCOUT_WAYPOINTS_RAD_CHAN, 0);
		}
	}
	
	private static void actionAllState(Robot[] allies) throws GameActionException {
		
		
		//Updates the number of each unit we have 
		preformCensus(); 
		//Broadcasts enemy position data to army
		updateEnemyLocationData();
		//Updates waypoints for scouts
		updateScoutWayPoints(); 
		//Check if the medbay is alive
		checkForMedbay();
		
		//TODO: comment why sometimes these return and some don't
		if(mRC.isActive()){
			if(mRC.checkResearchProgress(Upgrade.NUKE) > Upgrade.NUKE.numRounds - RUSH_NUKE_TIME) {
				// We're almost done with the nuke!
				mRC.researchUpgrade(Upgrade.NUKE);
				mRC.setIndicatorString(2, "Nuke almost done!");
				return;
			}
			if(NUM_ENC_TO_CLAIM > 0 && Clock.getRoundNum() < 10){
				HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
				return;
			}
			if(mRC.getTeamPower() < PREFUSION_POWER_RESERVE){
				pickResearch();
				return;
			}
			for (int i = ENC_CLAIM_RAD_CHAN_START;
					i < ENC_CLAIM_RAD_CHAN_START + Math.min(NUM_ENC_TO_CLAIM, NUM_PREFUSION_ENC); i++) {
				if (HQRobot.mRadio.readChannel(i) == -1) {
					HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
					return;
				}
			}
			if(minerCount < NUM_MINERS) { 
				++ minerCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.LAY_MINES);
			}
			else if(scoutCount < NUM_SCOUTS) {
				++ scoutCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.SCOUT);
			}
			else if(armyCount < NUM_ARMY_NO_FUSION){
				++ armyCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
			} else if (!mRC.hasUpgrade(Upgrade.FUSION)) {
				mRC.researchUpgrade(Upgrade.FUSION);
			} else {
				mRC.setIndicatorString(2, "Has Fusion!");
				for (int i = ENC_CLAIM_RAD_CHAN_START;
						i < ENC_CLAIM_RAD_CHAN_START + NUM_ENC_TO_CLAIM; i++) {
					if (HQRobot.mRadio.readChannel(i) == -1) {
						HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
						return;
					}
				}
				if(armyCount < NUM_ARMY_WITH_FUSION
						&& mRC.getTeamPower() > POWER_RESERVE) {
					++ armyCount;
					HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
				}
				else {
					print("researching because enough army or power" + armyCount);
					pickResearch();
				}
			}
		}
		
		lastPower  = mRC.getTeamPower();
		
	}
	
	private static void checkForMedbay() throws GameActionException {
		MapLocation medbay = indexToLocation(HQRobot.mRadio.readChannel(MEDBAY_LOCATION_CHAN));
		if(mRC.canSenseSquare(medbay)){
			GameObject o = mRC.senseObjectAtLocation(medbay);
			if(o != null && o.getTeam() == mRC.getTeam()
					&& mRC.senseRobotInfo((Robot) o).type == RobotType.MEDBAY)
				return;
		}
		// The medbay value was invalid, replace it with our location
		HQRobot.mRadio.writeChannel(MEDBAY_LOCATION_CHAN, locationToIndex(mRC.getLocation()));
		
		// If the location wasn't our location, unclaim the encampment so we try to reclaim it
		if(!medbay.equals(mRC.getLocation())){
			for (int i = ENC_CLAIM_RAD_CHAN_START;
					i < ENC_CLAIM_RAD_CHAN_START + NUM_ENC_TO_CLAIM; i++) { 
				if (HQRobot.mRadio.readChannel(i) == locationToIndex(medbay)) {
					HQRobot.mRadio.writeChannel(i, -1);
				}
			}
			HQRobot.mRadio.writeChannel(MEDBAY_CLAIMED_RAD_CHAN, 0);
		}
	}

	private static void pickResearch() throws GameActionException {
		if (!mRC.hasUpgrade(Upgrade.FUSION))
			mRC.researchUpgrade(Upgrade.FUSION);
		else
			mRC.researchUpgrade(Upgrade.NUKE);
	}
	
	private static void turtleState() throws GameActionException {
		HQRobot.setRallyPoint(new MapLocation(
				(6*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/7,
				(6*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/7));
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		if(mRC.checkResearchProgress(Upgrade.NUKE) <= Upgrade.NUKE.numRounds/2 && mRC.senseEnemyNukeHalfDone()) {
			HQRobot.enemyNukeSoon = true;
			HQRobot.switchState(HQState.ATTACK);
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
			HQRobot.setRallyPoints(waypointsToEnemyHQ, nextWaypointIndex+1);
			//HQRobot.setRallyPoints(waypointsToEnemyHQ);
			//mRC.setIndicatorString(2, findNextWaypoint(waypointsToEnemyHQ, new MapLocation(avgX, avgY)).toString());
		}

	}
	
}



