package MineTurtle.Robots.Types;

import MineTurtle.Robots.HQRobot;
import MineTurtle.Robots.HQRobot.HQState;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;
public class HQNormalType {
	
	
	public static void run(RobotController rc) throws GameActionException
	{
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		actionAllState(rc,alliedRobots);

		switch(HQRobot.getState())
		{
		case TURTLE: {
			turtleState(rc);
			break;
		}
		case PREPARE_ATTACK: {
			prepareAttackState(rc);
			break;
		}
		case ATTACK: {
			attackHQState(rc);
			break;
		}
		default:
			break;
			
		}
		
	}
	
	private static void actionAllState(RobotController rc,Robot[] allies) throws GameActionException {
		
		//Perform census
		if(Clock.getRoundNum()%CENSUS_INTERVAL == 0)
			HQRobot.mRadio.writeChannel(COUNT_MINERS_RAD_CHAN, 0);
		
		if (Clock.getRoundNum() == 0) {
			for (int i = ENC_CLAIM_RAD_CHAN_START; i < NUM_ENC_TO_CLAIM + ENC_CLAIM_RAD_CHAN_START; ++i) {
				HQRobot.mRadio.writeChannel(i, -1);				
			}
		}
		else if(Clock.getRoundNum()%CENSUS_INTERVAL == 1){
			int minerCount = HQRobot.mRadio.readChannel(COUNT_MINERS_RAD_CHAN); 					
			if(minerCount < NUM_MINERS) {
				print("HQ writing to spawn miners"); 
				HQRobot.mRadio.writeChannel(SPAWN_MINER_RAD_CHAN, NUM_MINERS-minerCount);
			}
		}

		if(rc.isActive()){
			if (allies.length <= NUM_ROBOT_TO_SPAWN) {
				HQRobot.spawnRobot();
			} else {
				if (rc.hasUpgrade(Upgrade.PICKAXE)) {
					rc.researchUpgrade(Upgrade.NUKE);
				} else {
					rc.researchUpgrade(Upgrade.PICKAXE);
				}
			}
		}
	}
	
	
	private static void turtleState(RobotController rc) throws GameActionException {
		HQRobot.setRallyPoint(new MapLocation((6*rc.getLocation().x + HQRobot.enemyHQLoc.x)/7, (6*rc.getLocation().y + HQRobot.enemyHQLoc.y)/7));
		if(rc.checkResearchProgress(Upgrade.NUKE) <= Upgrade.NUKE.numRounds/2 && rc.senseEnemyNukeHalfDone()) {
			HQRobot.switchState(HQState.PREPARE_ATTACK);
		}
	}

	private static void prepareAttackState(RobotController rc) throws GameActionException {
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		HQRobot.setRallyPoint(new MapLocation((4*rc.getLocation().x + HQRobot.enemyHQLoc.x)/5, (4*rc.getLocation().y + HQRobot.enemyHQLoc.y)/5));
		if(alliedRobots.length >= NUM_ROBOT_TO_SPAWN) {			
			HQRobot.switchState(HQState.ATTACK); //attack!
			HQRobot.mRadio.writeChannel(ARMY_MESSAGE_SIGNAL_CHAN, ATTACK_HQ_SIGNAL);
		}			
	}

	private static void attackHQState(RobotController rc) throws GameActionException {
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
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
		if(alliedRobots.length < NUM_ROBOT_TO_SPAWN/2) {
			HQRobot.switchState(HQState.PREPARE_ATTACK);			
			HQRobot.mRadio.writeChannel(ARMY_MESSAGE_SIGNAL_CHAN, RETREAT_SIGNAL);			
		}
	}
	
}


