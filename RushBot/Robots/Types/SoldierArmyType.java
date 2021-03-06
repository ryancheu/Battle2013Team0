package RushBot.Robots.Types;

import static RushBot.Robots.ARobot.mRC;
import static RushBot.Util.Constants.*;
import static RushBot.Util.Util.*;

import RushBot.Robots.ARobot;
import RushBot.Robots.SoldierRobot;
import RushBot.Robots.SoldierRobot.SoldierState;
import RushBot.Util.Constants;
import RushBot.Util.RadioChannels;
import battlecode.common.*;

public class SoldierArmyType {
	
	private static MapLocation[] medbayWaypoints;
	private static MapLocation lastMedbayLoc;

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
			case ATTACK_HQ: {
				attackHQLogic();
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
		
		MapLocation rally = SoldierRobot.findRallyPoint();
		if ( mRC.getEnergon() < SOLDIER_RUN_EVENTUALLY_HEALTH && enemyRobots.length==0 &&
				!indexToLocation(SoldierRobot.mRadio.readChannel(RadioChannels.MEDBAY_LOCATION)).equals(SoldierRobot.HQLoc)) {
			SoldierRobot.switchState(SoldierState.GOTO_MEDBAY);
			return;
		}
		
		else if(SoldierRobot.mRadio.readChannel(RadioChannels.ENTER_BATTLE_STATE) == 1
				&& closestDist < SOLDIER_JOIN_ATTACK_RAD) {
			SoldierRobot.switchState(SoldierState.BATTLE);
			return;
		}
		
		// no enemies nearby, just go to the next rally point
		else if(enemyRobots.length==0 || closestDist > SOLDIER_ATTACK_RAD) {
			goToLocation(SoldierRobot.findRallyPoint(),shouldDefuseMines);
		}
		
		//someone spotted and allied robots outnumber enemy
		else if (enemyRobots.length < alliedRobots.length * SOLDIER_OUTNUMBER_MULTIPLIER) {			
			SoldierRobot.switchState(SoldierState.BATTLE);	
			SoldierRobot.mRadio.writeChannel(RadioChannels.ENTER_BATTLE_STATE, 1);
		}
		
		//We're outnumbered, run away!
		else {
			goToLocation(SoldierRobot.HQLoc, shouldDefuseMines);
		}
	}
	private static void battleLogic() throws GameActionException {
		if(SoldierRobot.mRadio.readChannel(RadioChannels.ENTER_BATTLE_STATE) == 0) {
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}
		
		Robot[] enemyRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mEnemy);				
		Robot[] nearbyEnemyRobots = mRC.senseNearbyGameObjects(Robot.class, SOLDIER_JOIN_ATTACK_RAD, SoldierRobot.mEnemy);
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam);	
		
		int closestDist = MAX_DIST_SQUARED;
		int tempDist;
		int badLocations = 0;
		RobotInfo tempRobotInfo;
		MapLocation closestEnemy=null;
		for (Robot arobot:enemyRobots) {
			tempRobotInfo = mRC.senseRobotInfo(arobot);
			int diffX = mRC.getLocation().x - tempRobotInfo.location.x;
			int diffY = mRC.getLocation().y - tempRobotInfo.location.y;
			tempDist = Math.max(Math.abs(diffX), Math.abs(diffY));
			if(tempDist == 3){
				badLocations |= SoldierRobot.THREE_AWAY_BITS[6-(diffX + 3)][diffY + 3];
			}
			if (tempDist<closestDist ) {
				
				closestDist = tempDist;
				closestEnemy = tempRobotInfo.location;
			}
		}
		mRC.setIndicatorString(0, "badLocs: " + badLocations);
		if(closestDist < 3){
			badLocations = 0;
		}
		float randomNumber = ARobot.rand.nextFloat();
		if ( mRC.getEnergon() < SOLDIER_RUN_HEALTH &&
				!indexToLocation(SoldierRobot.mRadio.readChannel(RadioChannels.MEDBAY_LOCATION)).equals(SoldierRobot.HQLoc)) {
			SoldierRobot.switchState(SoldierState.GOTO_MEDBAY );
			return;
		}
		
		//no enemies visible, just go to the next rally point
		if(enemyRobots.length == 0 ) {
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			SoldierRobot.mRadio.writeChannel(RadioChannels.ENTER_BATTLE_STATE, 0);
			return;
		}
		else if(nearbyEnemyRobots.length == 0) {
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}
		else if(mRC.getLocation().distanceSquaredTo(SoldierRobot.enemyHQLoc) < ATTACK_HQ_RAD) {
			SoldierRobot.switchState(SoldierState.ATTACK_HQ);
			return;
		}
		else if(randomNumber > CHANCE_OF_DEFUSING_ENEMY_MINE && (enemyRobots.length < alliedRobots.length/3)){
			Direction dir = mRC.getLocation().directionTo(SoldierRobot.enemyHQLoc);
			for (int d:Constants.testDirOrderFrontSide) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				MapLocation newLoc = mRC.getLocation().add(lookingAtCurrently);
				Team mineOwner = mRC.senseMine(newLoc);
				if(mRC.canMove(lookingAtCurrently) &&
						isMineDir(mRC.getLocation(),lookingAtCurrently,true) && 
						mineOwner == SoldierRobot.mEnemy ) {
					mRC.defuseMine(newLoc);
					return;
				}
			}
		}
		else if(randomNumber > CHANCE_OF_DEFUSING_NEUTRAL_MINE && (enemyRobots.length < alliedRobots.length/3)){
			Direction dir = mRC.getLocation().directionTo(SoldierRobot.enemyHQLoc);
			for (int d:Constants.testDirOrderFrontSide) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				MapLocation newLoc = mRC.getLocation().add(lookingAtCurrently);
				Team mineOwner = mRC.senseMine(newLoc);
				if(mRC.canMove(lookingAtCurrently) &&
						isMineDir(mRC.getLocation(),lookingAtCurrently,true) && 
						mineOwner == mRC.getTeam().NEUTRAL ) {
					mRC.defuseMine(newLoc);
					return;
				}
			}
		}
		Direction tempDir; 
		if ((tempDir = determineBestBattleDirection(getNeighborStats(badLocations),closestEnemy)) != null) {
			if ( tempDir.ordinal() < NUM_DIR && mRC.canMove(tempDir) ) {
				mRC.move(tempDir);
			}
		}
		
	}

	//Returns least surrounded position or closest position to battle rally, or null if cannot move
		private static Direction determineBestBattleDirection(int[] neighborData,MapLocation closestEnemy) throws GameActionException {
			int a = Clock.getBytecodesLeft();		
			Direction bestDir = null;
			float bestScore = 99999;
			float tempScore = 0;
			int tempNumEnemies = 0;
			int distSqrToBattleRally= 0;

			MapLocation botLoc = mRC.getLocation();
			float numNearbyEnemies = mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mEnemy).length;
			float numNearbyAllies = mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mTeam).length;
			boolean locallyOutnumbered = (numNearbyEnemies > (numNearbyAllies*.8)) && (neighborData[NUM_DIR] == 0);		
			
			float zeroMultiplier = locallyOutnumbered ? -1 : 1;
			float zeroMultiplierTwo = locallyOutnumbered ? -1 : 1; 

			for ( int i = 0; i < NUM_DIR; ++i) {
				if ( (neighborData[i] < 100 || i == NUM_DIR) && !isMineDir(mRC.getLocation(),Direction.values()[i],true))
				{
					tempNumEnemies = neighborData[i];
					distSqrToBattleRally = botLoc.add(Direction.values()[i]).distanceSquaredTo(closestEnemy);
					if ( tempNumEnemies == 0 ) {
						tempScore = zeroMultiplier*NUM_DIR + zeroMultiplierTwo*distSqrToBattleRally;					
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
			//Special logic for current spot, prefer to move
			tempNumEnemies = neighborData[NUM_DIR];
			if ( tempNumEnemies != 0 ) {
				distSqrToBattleRally = botLoc.distanceSquaredTo(closestEnemy);
				tempScore = (tempNumEnemies << 1) - (1f/distSqrToBattleRally);
				if ( tempScore < bestScore) {
					bestDir = Direction.values()[NUM_DIR];
					bestScore = tempScore;
				}
			}
			mRC.setIndicatorString(1, "choose dir:  "  + bestDir + "outnubmered: " + locallyOutnumbered + "neigh data " + neighborData[NUM_DIR] + "round" + Clock.getRoundNum());
			//mRC.setIndicatorString(1, "bytecode used for determine: " + (a - Clock.getBytecodesLeft()));
			return bestDir;

		}

	private static void gotoMedbayLogic () throws GameActionException {
		if ( SoldierRobot.getLastState() != SoldierState.GOTO_MEDBAY) {
			lastMedbayLoc = null;
		}
		if ( mRC.getEnergon() < SOLDIER_RETURN_HEALTH) {
			MapLocation medbay = SoldierRobot.findNearestMedBay();
			if(!medbay.equals(lastMedbayLoc)){
				lastMedbayLoc = medbay;
				if (SoldierRobot.wayPoints != null && SoldierRobot.wayPoints.size() > 1) {
					medbayWaypoints = convertWaypoints(SoldierRobot.wayPoints.toArray(new MapLocation[0]),
							mRC.getLocation(), SoldierRobot.findNearestMedBay());
				}
				else {
					medbayWaypoints = new MapLocation[1];
					medbayWaypoints[0] = medbay;
				}
			}
			goToLocation(findNextWaypoint(medbayWaypoints, mRC.getLocation()), true);
		}
		else {
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
		}
	}
	
	private static void attackHQLogic() throws GameActionException {
		/*if ( mRC.getEnergon() < SOLDIER_RUN_HEALTH ) {
			SoldierRobot.switchState(SoldierState.GOTO_MEDBAY);
			return;
		}*/
		goToLocation(SoldierRobot.enemyHQLoc, true);
	}
	
}