package AttackingTest2.Robots.Types;

import static AttackingTest2.Robots.ARobot.mRC;
import static AttackingTest2.Util.Util.getNeighborStats;
import static AttackingTest2.Util.Constants.*;
import static AttackingTest2.Util.Util.*;

import AttackingTest2.Robots.ARobot;
import AttackingTest2.Robots.SoldierRobot;
import AttackingTest2.Robots.SoldierRobot.SoldierState;
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
		
		
		//no enemies visible, just go to the next rally point
		if(enemyRobots.length==0 || closestDist > SOLDIER_ATTACK_RAD && SoldierRobot.mRadio.readChannel(RUNAWAY_RADIO_CHANNEL) !=1) {
			goToLocation(SoldierRobot.findRallyPoint(),shouldDefuseMines);
		}
		
		//someone spotted and allied robots outnumber enemy
		else if (true) {			
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
			//SoldierRobot.switchState(SoldierState.GOTO_MEDBAY);
			//goToLocation(SoldierRobot.findNearestMedBay());
			//return;
		}
			
		//no enemies visible, just go to the next rally point
		if(enemyRobots.length == 0 && SoldierRobot.mRadio.readChannel(RUNAWAY_RADIO_CHANNEL) !=1) {
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}
		else if(SoldierRobot.mBattleRunaway < 3  && SoldierRobot.mRadio.readChannel(RUNAWAY_RADIO_CHANNEL) != -1){
			mRC.setIndicatorString(0, ""+enemyRobots.length);
			SoldierRobot.mRadio.writeChannel(RUNAWAY_RADIO_CHANNEL, 1);
			MapLocation myLoc = mRC.getLocation();
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
			goToLocation(myLoc.add(closestEnemy.directionTo(myLoc),3));
			SoldierRobot.mBattleRunaway += 1;
		}
		else{
			//TODO should probably be attack nearest enemy
			mRC.setIndicatorString(0, ""+enemyRobots.length);
			SoldierRobot.mRadio.writeChannel(RUNAWAY_RADIO_CHANNEL, -1);
			Direction tempDir; 
			if ((tempDir = determineBestBattleDirection(getNeighborStats())) != null) {
				if ( mRC.canMove(tempDir) ) {
					mRC.move(tempDir);
				}
			}
			/*int closestDist = MAX_DIST_SQUARED;
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
			goToLocation(closestEnemy);
			*/
		}
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
			
			MapLocation oneSplitLocation = new MapLocation(avgX-4,avgY+4);
			MapLocation otherSplitLocation = new MapLocation(avgX-4,avgY-4);
			if(mRC.getLocation().y > avgY){
				goToLocation(oneSplitLocation, false);

			}
			else{
				goToLocation(otherSplitLocation, false);

			}
			*/
		
		//someone spotted and allied robots outnumber enemy
		/*
		else if (enemyRobots.length < alliedRobots.length * SOLDIER_OUTNUMBER_MULTIPLIER) {
			Direction tempDir; 
			if ((tempDir = determineBestBattleDirection(getNeighborStats())) != null) {
				if ( mRC.canMove(tempDir) ) {
					print("battle logic working");
					mRC.move(tempDir);
				}
				else {
					print("couldn't move: " + Clock.getBytecodesLeft());
				}
			}
		}
		*/
		
	}
	
	//Returns least surrounded position or closest position to battle rally, or null if cannot move
	private static Direction determineBestBattleDirection(int[] neighborData) throws GameActionException {
		Direction bestDir = null;
		float bestScore = 99999;
		float tempScore = 0;
		int tempNumEnemies = 0;
		int distSqrToBattleRally= 0;
		
		MapLocation botLoc = mRC.getLocation();
		
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
		
		
		for ( int i = 0; i < NUM_DIR; ++i) {
			if ( neighborData[i] < 100 && !isMineDir(mRC.getLocation(),Direction.values()[i],true))
			{
				tempNumEnemies = neighborData[i]%10;
				distSqrToBattleRally = botLoc.add(Direction.values()[i]).distanceSquaredTo(SoldierRobot.getBattleRally());
				
				
				if ( tempNumEnemies == 0 ) {
					tempScore = 3*2 - (1f/distSqrToBattleRally);					
				}
				else {
					tempScore = (tempNumEnemies << 1) - (1f/distSqrToBattleRally); // multiply by 2 to make sure enemy # more important than rally dist
				}
				/*
				if ( Clock.getRoundNum() > 315 ) {
					tempScore = -(1f/distSqrToBattleRally);
				}
				else {
					tempScore = (1/distSqrToBattleRally);
				}
				*/
				/*
				if ( distSqrToBattleRally != 0 )
				{
					tempScore = distSqrToBattleRally;
				}
				else { 
					tempScore = 9999999;
				}
				*/
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