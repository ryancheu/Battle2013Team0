package MineTurtle.Robots.Types;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Robots.ARobot.mRC;
import static MineTurtle.Util.Util.*;

import MineTurtle.Robots.ARobot;
import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.SoldierRobot.SoldierState;
import battlecode.common.*;

public class SoldierArmyType {
	
	public static void run() throws GameActionException {
		if(mRC.isActive()) {
			switch(SoldierRobot.getState())
			{
			case GOTO_RALLY: {
				armyGotoRallyLogic();
				break;
			}
			case BATTLE: { 
				battleLogic();
				break;
			}
			case GOTO_MEDBAY: { 
				gotoMedbayLogic();
				break;
			}
			default:
				break;			
			}
		}
	}
	
	private static void armyGotoRallyLogic() throws GameActionException {
		Robot[] enemyRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mEnemy);
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam);
		Robot[] nearbyEnemies = mRC.senseNearbyGameObjects(Robot.class, SOLDIER_ENEMY_CHECK_RAD, SoldierRobot.mEnemy);
		
		boolean shouldDefuseMines = (enemyRobots.length < alliedRobots.length/3) || (nearbyEnemies.length == 0);
		
		int closestDist = MAX_DIST_SQUARED;
		int tempDist;
		RobotInfo tempRobotInfo;
		MapLocation closestEnemy=null;
		for (Robot arobot:enemyRobots) {
			tempRobotInfo = mRC.senseRobotInfo(arobot);
			tempDist = tempRobotInfo.location.distanceSquaredTo(mRC.getLocation());
			if (tempDist<closestDist) {
				closestDist = tempDist;
				closestEnemy = tempRobotInfo.location;
			}
		}
		
		mRC.setIndicatorString(2, SoldierRobot.wayPoints.size()+"");
		
		//no enemies visible, just go to the next rally point
		if(enemyRobots.length==0 || closestDist > SOLDIER_ATTACK_RAD) {
			goToLocation(SoldierRobot.findRallyPoint(),shouldDefuseMines);
		}
		
		//someone spotted and allied robots outnumber enemy
		else if (enemyRobots.length < alliedRobots.length * SOLDIER_OUTNUMBER_MULTIPLIER) {			
			SoldierRobot.switchState(SoldierState.BATTLE);
			goToLocation(closestEnemy, shouldDefuseMines);
			
		}
		
		//We're outnumbered, run away!
		else {
			goToLocation(mRC.senseHQLocation(), shouldDefuseMines);
		}
	}
	private static void battleLogic() throws GameActionException {
		Robot[] enemyRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mEnemy);
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam);	
				
		if ( mRC.getEnergon() < SOLDIER_RUN_HEALTH ) {
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
			if ((tempDir = determineBestBattleDirection(getNeighborStats())) != null) {
				if ( mRC.canMove(tempDir) ) {
					mRC.move(tempDir);
				}
			}
		}
		
	}
	
	//Returns least surrounded position or closest position to battle rally, or null if cannot move
	private static Direction determineBestBattleDirection(int[] neighborData) throws GameActionException {
		Direction bestDir = null;
		float bestScore = 99999;
		float tempScore = 0;
		int tempNumEnemies = 0;
		int distSqrToBattleRally= 0;
		
		MapLocation botLoc = mRC.getLocation();
		
		for ( int i = 0; i < NUM_DIR; ++i) {
			if ( neighborData[i] < 100 && !isMineDir(mRC.getLocation(),Direction.values()[i],true))
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
	
	private static void gotoMedbayLogic () throws GameActionException {				
		if ( mRC.getEnergon() < SOLDIER_RETURN_HEALTH) {
			goToLocation(SoldierRobot.findNearestMedBay());
		}
		else {			
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
		}
	}
	
}