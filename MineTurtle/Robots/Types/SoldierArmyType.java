package MineTurtle.Robots.Types;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.SoldierRobot.SoldierState;
import battlecode.common.*;

public class SoldierArmyType {
	
	public static void run(RobotController rc) throws GameActionException {
		if(rc.isActive()) {
			switch(SoldierRobot.getState())
			{
			case GOTO_RALLY: {
				armyGotoRallyLogic(rc);
				break;
			}
			case BATTLE: { 
				battleLogic(rc);
				break;
			}
			case GOTO_MEDBAY: { 
				gotoMedbayLogic(rc);
				break;
			}
			default:
				break;			
			}
		}
	}
	
	private static void armyGotoRallyLogic(RobotController rc) throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mEnemy);
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam);
		Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, SOLDIER_ENEMY_CHECK_RAD, SoldierRobot.mEnemy);
		
		boolean shouldDefuseMines = (enemyRobots.length < alliedRobots.length/3) || (nearbyEnemies.length == 0);				
		
		//no enemies visible, just go to the next rally point
		if(enemyRobots.length==0) {
			goToLocation(rc, SoldierRobot.findRallyPoint(rc),shouldDefuseMines);
		}
		
		//someone spotted and allied robots outnumber enemy
		else if (enemyRobots.length < alliedRobots.length * SOLDIER_OUTNUMBER_MULTIPLIER) {
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
			SoldierRobot.switchState(SoldierState.BATTLE);
			goToLocation(rc, closestEnemy, shouldDefuseMines);
			
		}
		
		//We're outnumbered, run away!
		else {
			goToLocation(rc, rc.senseHQLocation(), shouldDefuseMines);
		}
	}
	private static void battleLogic(RobotController rc) throws GameActionException {
		
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mEnemy);
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam);	
				
		if ( rc.getEnergon() < SOLDIER_RUN_HEALTH ) {
			SoldierRobot.switchState(SoldierState.GOTO_MEDBAY);
			return;
		}
			
		//no enemies visible, just go to the next rally point
		if(enemyRobots.length == 0 ) {
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}
				
		//someone spotted and allied robots outnumber enemy
		else if (enemyRobots.length < alliedRobots.length * SOLDIER_OUTNUMBER_MULTIPLIER) {
			Direction tempDir; 
			if ((tempDir = determineBestBattleDirection(rc, getNeighborStats(rc))) != null) {
				if ( rc.canMove(tempDir) ) {
					rc.move(tempDir);
				}
			}
		}
		
	}
	
	//Returns least surrounded position or closest position to battle rally, or null if cannot move
	private static Direction determineBestBattleDirection(RobotController rc, int[] neighborData) throws GameActionException {
		Direction bestDir = null;
		float bestScore = 99999;
		float tempScore = 0;
		int tempNumEnemies = 0;
		int distSqrToBattleRally= 0;
		
		MapLocation botLoc = rc.getLocation();
		
		for ( int i = 0; i < NUM_DIR; ++i) {
			if ( neighborData[i] < 100 && !isMineDir(rc,rc.getLocation(),Direction.values()[i],true))
			{
				tempNumEnemies = neighborData[i]%10;
				distSqrToBattleRally = botLoc.add(Direction.values()[i]).distanceSquaredTo(SoldierRobot.getBattleRally());
				if ( tempNumEnemies == 0 ) {
					tempScore = NUM_DIR + distSqrToBattleRally;					
				}
				else {
					tempScore = (tempNumEnemies << 1) - (1f/distSqrToBattleRally); // multiply by 2 to make sure enemy # more important than rally dist
				}
				if ( tempScore < bestScore ) {
					bestDir = Direction.values()[i];
					bestScore = tempScore;
				}
			}
		}
		return bestDir;
		
	}
	
	private static void gotoMedbayLogic ( RobotController rc ) throws GameActionException {				
		if ( rc.getEnergon() < SOLDIER_RETURN_HEALTH) {
			goToLocation(rc, SoldierRobot.findNearestMedBay(rc));
		}
		else {			
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
		}
	}
	
}