package MineTurtle.Robots.Types;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.SoldierRobot.SoldierState;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class SoldierArmyType {
	
	public static void run(RobotController rc) throws GameActionException {
		if(rc.isActive()) {
			switch(SoldierRobot.getState())
			{
			case GOTO_RALLY: {
				armyGotoRallyLogic(rc);
				break;
			}
			case ATTACK_HQ: {
				armyAttackHQLogic(rc);
				break;
			}
			default:
				break;			
			}
		}
	}
	
	private static void armyAttackHQLogic(RobotController rc) throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, rc.getTeam().opponent());
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam);				
		//Check if should go into hq attack state
		if ( SoldierRobot.mRadio.readChannel(ARMY_MESSAGE_SIGNAL_CHAN) == RETREAT_SIGNAL) {
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}

		int distanceToRally;
		MapLocation rallyLocation;
		if((distanceToRally = rc.getLocation().distanceSquaredTo(rallyLocation = SoldierRobot.findRallyPoint(rc))) >RALLY_RAD_SQUARED 
				&& enemyRobots.length==0  ) {//no enemies visible and not at rally yet			
			goToLocation(rc, rallyLocation);
		}
		else if ( enemyRobots.length==0 && distanceToRally < RALLY_RAD_SQUARED) {
			goToLocation(rc,SoldierRobot.enemyHQLoc);
		}
		else if (enemyRobots.length > 0 && enemyRobots.length < alliedRobots.length) { //someone spotted and allied robots outnumber enemy
			int closestDist = MAX_DIST_SQUARED;
			int tempDist;
			RobotInfo tempRobotInfo;
			MapLocation closestEnemy=null;
			for (Robot arobot:enemyRobots) {
				tempRobotInfo = rc.senseRobotInfo(arobot);
				tempDist = tempRobotInfo.location.distanceSquaredTo(rc.getLocation());
				if (tempDist<closestDist) {
					closestDist = tempDist;
					closestEnemy = tempRobotInfo.location;
				}
			}
			goToLocation(rc, closestEnemy);
		}
		else {
			goToLocation(rc, SoldierRobot.enemyHQLoc);
		}		
	}
	private static void armyGotoRallyLogic(RobotController rc) throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mEnemy);
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam);
				
		//Check if should go into hq attack state
		if ( SoldierRobot.mRadio.readChannel(ARMY_MESSAGE_SIGNAL_CHAN) == ATTACK_HQ_SIGNAL) {
			SoldierRobot.switchState(SoldierState.ATTACK_HQ);
			return;
		}
		
		if(enemyRobots.length==0) {//no enemies visible
			goToLocation(rc, SoldierRobot.findRallyPoint(rc));
		} 
		
		else if (enemyRobots.length < alliedRobots.length) { //someone spotted and allied robots outnumber enemy
			int closestDist = MAX_DIST_SQUARED;
			int tempDist;
			RobotInfo tempRobotInfo;
			MapLocation closestEnemy=null;
			for (Robot arobot:enemyRobots) {
				tempRobotInfo = rc.senseRobotInfo(arobot);
				tempDist = tempRobotInfo.location.distanceSquaredTo(rc.getLocation());
				if (tempDist<closestDist) {
					closestDist = tempDist;
					closestEnemy = tempRobotInfo.location;
				}
			}
			goToLocation(rc, closestEnemy);
		}
		else {
			goToLocation(rc, rc.senseHQLocation());
		}
	}
	
}