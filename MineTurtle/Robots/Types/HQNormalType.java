package MineTurtle.Robots.Types;

import MineTurtle.Robots.HQRobot;
import MineTurtle.Robots.HQRobot.HQState;
import MineTurtle.Robots.SoldierRobot;
import battlecode.common.*;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

import static MineTurtle.Robots.ARobot.mRC;
public class HQNormalType {
	
	
	private static int minerCount = 0;
	private static int scoutCount = 0;

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
	
	private static void actionAllState(Robot[] allies) throws GameActionException {
		
		//Perform census
		if(Clock.getRoundNum()%CENSUS_INTERVAL == 0)
			HQRobot.mRadio.writeChannel(COUNT_MINERS_RAD_CHAN, 0);
		
		if (Clock.getRoundNum() == 0) {
			setNumberOfEncampments();
			System.out.println("encampments: " + NUM_ENC_TO_CLAIM);
			for (int i = ENC_CLAIM_RAD_CHAN_START; i < NUM_ENC_TO_CLAIM + ENC_CLAIM_RAD_CHAN_START; ++i) {
				HQRobot.mRadio.writeChannel(i, -1);				
			}
		}
		else if(Clock.getRoundNum()%CENSUS_INTERVAL == 1){
			minerCount  = HQRobot.mRadio.readChannel(COUNT_MINERS_RAD_CHAN);
		}
		
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
		}
		else {
			avgX = HQRobot.enemyHQLoc.x;
			avgY = HQRobot.enemyHQLoc.y;
		}
			
		
		//Write the average enemy location to be used by battling units
		HQRobot.mRadio.writeChannel(ENEMY_AVG_POS_RAD_CHANNEL, locationToIndex(new MapLocation(avgX,avgY)));

		if(mRC.isActive()){
			if (allies.length <= NUM_ROBOT_TO_SPAWN) {
				HQRobot.mRadio.writeChannel(NEXT_SOLDIER_TYPE_CHAN, pickSoldierTypeToSpawn().ordinal());
				HQRobot.spawnRobot();
			} else {
				if (mRC.hasUpgrade(Upgrade.FUSION)) {
					mRC.researchUpgrade(Upgrade.NUKE);
				} else {
					mRC.researchUpgrade(Upgrade.FUSION);
				}
			}
		}
	}
	
	private static SoldierRobot.SoldierType pickSoldierTypeToSpawn() throws GameActionException{
		setNumberOfEncampments();
		if(NUM_ENC_TO_CLAIM > 0 && Clock.getRoundNum() == 0)
			return SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT;
		for (int i = ENC_CLAIM_RAD_CHAN_START; i < ENC_CLAIM_RAD_CHAN_START + NUM_ENC_TO_CLAIM; i++) {
			if (HQRobot.mRadio.readChannel(i) == -1) {
				return SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT;
			}
		}
		if(minerCount < NUM_MINERS) { 
			++ minerCount;
			return SoldierRobot.SoldierType.LAY_MINES;
		}
		if(scoutCount  < NUM_SCOUTS) {
			++ scoutCount;
			return SoldierRobot.SoldierType.SCOUT;
		}
		return SoldierRobot.SoldierType.ARMY;
	}
	
	
	private static void turtleState() throws GameActionException {
		
		MapLocation rallyPoint  = new MapLocation((6*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/7, (6*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/7);
		
		int message = Clock.getRoundNum() 
				| (HQ_ATTACK_RALLY_CHAN_START << WAYPOINT_ROUND_BITS) 
				| (1 << (WAYPOINT_ROUND_BITS + WAYPOINT_START_CHAN_BITS));
		HQRobot.mRadio.writeChannel(SOLDIER_WAYPOINT_RALLY_CHAN, message);
		HQRobot.mRadio.writeChannel(HQ_ATTACK_RALLY_CHAN_START, locationToIndex(rallyPoint));
		
		;
		if(mRC.checkResearchProgress(Upgrade.NUKE) <= Upgrade.NUKE.numRounds/2 && mRC.senseEnemyNukeHalfDone()) {
			HQRobot.switchState(HQState.PREPARE_ATTACK);
		}
	}

	private static void prepareAttackState() throws GameActionException {
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		MapLocation preAttackRallyLocation = new MapLocation(
				(4*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/5,
				(4*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/5);
		if(alliedRobots.length >= NUM_ROBOT_TO_SPAWN)
			HQRobot.switchState(HQState.ATTACK); //attack!
		int message = Clock.getRoundNum() 
				| (HQ_ATTACK_RALLY_CHAN_START << WAYPOINT_ROUND_BITS) 
				| (1 << (WAYPOINT_ROUND_BITS + WAYPOINT_START_CHAN_BITS));
		HQRobot.mRadio.writeChannel(SOLDIER_WAYPOINT_RALLY_CHAN, message);
		HQRobot.mRadio.writeChannel(HQ_ATTACK_RALLY_CHAN_START, locationToIndex(preAttackRallyLocation));
	}

	private static void attackHQState() throws GameActionException {
		
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		/*
		int avgX = 0, avgY = 0, numSoldiers = 0;
		for(Robot bot:alliedRobots){
			RobotInfo info = rc.senseRobotInfo(bot);
			if(info.type == RobotType.SOLDIER){
				numSoldiers ++;
				avgX += info.location.x;
				avgY += info.location.y;
			}
		}
		avgX /= numSoldiers;
		avgY /= numSoldiers;
		HQRobot.setRallyPoint(new MapLocation((4*avgX + HQRobot.enemyHQLoc.x)/5, (4*avgY + HQRobot.enemyHQLoc.y)/5));
		*/
		
		//TODO: Re add in clumping!
		if(alliedRobots.length < NUM_ROBOT_TO_SPAWN/2) 
			HQRobot.switchState(HQState.PREPARE_ATTACK);

		int message = Clock.getRoundNum() 
				| (HQ_ATTACK_RALLY_CHAN_START << WAYPOINT_ROUND_BITS) 
				| (1 << (WAYPOINT_ROUND_BITS + WAYPOINT_START_CHAN_BITS));
		HQRobot.mRadio.writeChannel(SOLDIER_WAYPOINT_RALLY_CHAN, message);
		HQRobot.mRadio.writeChannel(HQ_ATTACK_RALLY_CHAN_START, locationToIndex(HQRobot.enemyHQLoc));

	}
	
}



