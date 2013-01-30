package BaseBot.Robots.Types;

import static BaseBot.Robots.ARobot.mRC;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.NonConstants.*;
import static BaseBot.Util.Util.*;

import java.util.ArrayList;




import BaseBot.Robots.ARobot;
import BaseBot.Robots.HQRobot;
import BaseBot.Robots.HQRobot.HQType;
import BaseBot.Robots.SoldierRobot;
import BaseBot.Robots.HQRobot.HQState;
import BaseBot.Robots.SoldierRobot.SoldierState;
import BaseBot.Robots.SoldierRobot.SoldierType;
import BaseBot.Util.Constants;
import BaseBot.Util.NonConstants;
import BaseBot.Util.RadioChannels;
import battlecode.common.*;
public class SoldierArmyTypeOldSchool {
	
	private static MapLocation[] medbayWaypoints;
	private static boolean[][] enemyThere;
	private static MapLocation[] nextToLocations;
	private static MapLocation lastMedbayLoc;
	
	private static boolean isFirstRun = true;	
	private static boolean wasEnemyNukingFastWhenWeWereSpawned = false;

	public static void run() throws GameActionException {
		HQRobot.readTypeAndState();
		if ( !SoldierRobot.isSmallMap && HQRobot.mType != HQType.RUSH ) {
			SoldierRobot.switchType(SoldierType.ARMY);
			SoldierRobot.switchState(SoldierState.RETREAT);
			return;
		}
		if(mRC.isActive()) {
			allLogic();
			
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
			case GOTO_SHIELD: {
				gotoShieldLogic();
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
		SoldierRobot.mLastTurnPotentialDamage = mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mEnemy).length*6 + 6;
	}


	private static void allLogic() throws GameActionException {
		if(isFirstRun) {
			isFirstRun = false;
			wasEnemyNukingFastWhenWeWereSpawned = (SoldierRobot.mRadio.readChannel(RadioChannels.ENEMY_FASTER_NUKE) == 1);
		}
		
		int oldRadius = SoldierRobot.mRadio.readChannel(RadioChannels.ENEMY_MINE_RADIUS);
		if((oldRadius & FIRST_BYTE_KEY_MASK) != FIRST_BYTE_KEY) {
			oldRadius = 0;
		}
		else {
			oldRadius ^= FIRST_BYTE_KEY;
		}
		if(mRC.senseMine(mRC.getLocation()) == SoldierRobot.mEnemy && SoldierRobot.enemyNukingFast) {
			int radius = getRealDistance(mRC.getLocation(), SoldierRobot.enemyHQLoc) + 1;
			if(oldRadius < radius) {	
				SoldierRobot.mRadio.writeChannel(RadioChannels.ENEMY_MINE_RADIUS,
						radius | FIRST_BYTE_KEY);
			}
		}
		if(oldRadius > 0) {
			SoldierRobot.enemyMineRadius = oldRadius; //This used to have +1 but we'd rather hit more mines than be slow
		}
		else {
			SoldierRobot.enemyMineRadius = 0;
		}
		// SoldierRobot.enemyMineRadius = 25;
		mRC.setIndicatorString(0, "enemyMineRadius: " + SoldierRobot.enemyMineRadius);
	}


	private static void armyGotoRallyLogic() throws GameActionException {
		Robot[] nearbyAllies = mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mTeam);
		Robot[] nearbyEnemies = mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS, SoldierRobot.mEnemy);
		Robot[] nextToEnemies = mRC.senseNearbyGameObjects(Robot.class, 2, SoldierRobot.mEnemy);
		
		if (Math.abs( (Clock.getRoundNum() - SoldierRobot.mRadio.readChannel(RadioChannels.BATTLE_OCCURED))) <= 1) {
			SoldierRobot.mLastAttackTurn = Clock.getRoundNum();
			SoldierRobot.switchState(SoldierState.BATTLE);			
			return;
		}
		else {
			mRC.setIndicatorString(0,"read channel : " + SoldierRobot.mRadio.readChannel(RadioChannels.BATTLE_OCCURED));
		}
		if ( nextToEnemies.length > 0 
				||SoldierRobot.rand.nextFloat() < (nearbyAllies.length-nearbyEnemies.length)*BREAK_TWO_SQUARES_PROB_NO_NUKE) {
			SoldierRobot.mRadio.writeChannel(RadioChannels.BATTLE_OCCURED, Clock.getRoundNum());
			SoldierRobot.switchState(SoldierState.BATTLE);
			return;
		}
		RobotInfo tempRobotInfo;
		int badLocations = 0;
		int badLocsTwo = 0;
		int tempDist;
		int closestDist = 10; //larger than we'll ever see
		MapLocation closestEnemy = null;
		
		for (int i = nearbyEnemies.length; --i >= 0;) {
			tempRobotInfo = mRC.senseRobotInfo(nearbyEnemies[i]);
			int diffX = mRC.getLocation().x - tempRobotInfo.location.x;
			int diffY = mRC.getLocation().y - tempRobotInfo.location.y;
			tempDist = Math.max(Math.abs(diffX), Math.abs(diffY));
			
			if ( tempRobotInfo.type == RobotType.ARTILLERY ) {				
				SoldierRobot.mRadio.writeChannel(RadioChannels.ENEMY_HAS_ARTILLERY_NORMAL, 1);
				SoldierRobot.enemyHasArtillery = true;
			}
			
			if ( tempRobotInfo.type == RobotType.SOLDIER && tempRobotInfo.roundsUntilMovementIdle < 3) {
				if ( tempDist == 2 ) {
					badLocsTwo |= SoldierRobot.THREE_AWAY_BITS[6-(diffX + 3)][6-(diffY + 3)];
				}
				if (tempDist<closestDist ) {
					
					closestDist = tempDist;
					closestEnemy = tempRobotInfo.location;
				}
			}
		}
		if ( closestEnemy == null ) {
			goToLocation(mRC.senseEnemyHQLocation());
		}
		else {
			int distSqrToHQ;
			float bestScore = -10000;
			float tempScore;
			int bestDir = NUM_DIR;
			Direction tempDir;
			MapLocation tempLoc;
			MapLocation roboLoc = mRC.getLocation();
			boolean isBadLoc = false;
			int directionToHQ = roboLoc.directionTo(mRC.senseEnemyHQLocation()).ordinal();
			int maxDist = SoldierRobot.HQLoc.distanceSquaredTo(SoldierRobot.enemyHQLoc);
			for ( int i: testDirOrderAll) {
				tempDir = Direction.values()[(i+directionToHQ+NUM_DIR)%NUM_DIR];
				
				if (mRC.canMove(tempDir) && !isMineDirTrueDanger(roboLoc,tempDir) ) {
					tempLoc = roboLoc.add(tempDir);					
					tempScore = maxDist - tempLoc.distanceSquaredTo(SoldierRobot.HQLoc) - mRC.senseNearbyGameObjects(Robot.class, tempLoc, 2, SoldierRobot.mTeam).length;
					isBadLoc = (badLocsTwo >> (NUM_DIR-1 - i) & 1) == 1;
					if (isBadLoc) {
						tempScore -= 1000;
					}
					if ( tempScore > bestScore ) {
						bestScore = tempScore;
						bestDir = i; 
					}
				}
			}
			
			tempDir = Direction.values()[NUM_DIR];
			

				tempLoc = roboLoc.add(tempDir);					
				tempScore = maxDist - roboLoc.distanceSquaredTo(SoldierRobot.HQLoc) - mRC.senseNearbyGameObjects(Robot.class, tempLoc, 2, SoldierRobot.mTeam).length;
				if ( tempScore > bestScore ) {
					return;					
				}
			
			if ( bestScore != -10000 ) {
				mRC.setIndicatorString(0, "bestScore: " + bestScore + "D: " + bestDir);
				mRC.move(Direction.values()[(bestDir+directionToHQ+NUM_DIR)%NUM_DIR]);
			}
			
		}
		
	}
	
	private static void battleLogic() throws GameActionException {
		Robot[] enemyRobots= mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mEnemy);
		/*
		if ( SoldierRobot.enemyNukingFast ){
			enemyRobots = mRC.senseNearbyGameObjects(Robot.class, 9 + 9, SoldierRobot.mEnemy); //only 3 *3 
		}
		else {
			 enemyRobots 
		}
		*/
		//Robot[] nearbyEnemyRobots = mRC.senseNearbyGameObjects(Robot.class, SOLDIER_JOIN_ATTACK_RAD, SoldierRobot.mEnemy);
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam);
		
		Robot[] sensorRangeEnemies = mRC.senseNearbyGameObjects(Robot.class, 18, SoldierRobot.mEnemy);
		Robot[] sensorRangeAllies = mRC.senseNearbyGameObjects(Robot.class, 18, SoldierRobot.mTeam);
		int numSensorEnemies = sensorRangeEnemies.length;
		int numSensorAllies = sensorRangeAllies.length;
		
		Robot[] nextToEnemies = mRC.senseNearbyGameObjects(Robot.class, 2, SoldierRobot.mEnemy);
		
		if ( nextToEnemies.length > 0) {
			SoldierRobot.mRadio.writeChannel(RadioChannels.BATTLE_OCCURED, Clock.getRoundNum());
			SoldierRobot.mLastAttackTurn = Clock.getRoundNum();
		}
		else if (( Math.abs(Clock.getRoundNum() - SoldierRobot.mRadio.readChannel(RadioChannels.BATTLE_OCCURED)) <= 1 )) {
			SoldierRobot.mLastAttackTurn = Clock.getRoundNum();
		}
		
		if ( Clock.getRoundNum() - SoldierRobot.mLastAttackTurn > 2 )  {
			
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}
		
		if (SoldierRobot.enemyNukingFast && mRC.senseEncampmentSquare(mRC.getLocation())
				&& mRC.getTeamPower() > mRC.senseCaptureCost() ) {
			if(mRC.getLocation().distanceSquaredTo(SoldierRobot.enemyHQLoc)
					< mRC.getLocation().distanceSquaredTo(SoldierRobot.HQLoc)
					&& SoldierRobot.mRadio.readChannel(RadioChannels.SHIELD_LOCATION) == 0) {
				if ( mRC.getTeamPower() > mRC.senseCaptureCost() + 1 ) {
					mRC.captureEncampment(RobotType.SHIELDS);
					SoldierRobot.mRadio.writeChannel(RadioChannels.SHIELD_LOCATION, -2);
					SoldierRobot.mRadio.writeChannel(RadioChannels.SHIELDS_CLAIMED, Clock.getRoundNum());
				}
				return;
			}
			if(mRC.getLocation().distanceSquaredTo(SoldierRobot.enemyHQLoc)
					< mRC.getLocation().distanceSquaredTo(SoldierRobot.HQLoc)
					&& SoldierRobot.mRadio.readChannel(RadioChannels.SECOND_MEDBAY) == 0) {					
				if ( mRC.getTeamPower() > mRC.senseCaptureCost() + 1 ) {
					mRC.captureEncampment(RobotType.MEDBAY);
					SoldierRobot.mRadio.writeChannel(RadioChannels.SECOND_MEDBAY, locationToIndex(mRC.getLocation()));
					SoldierRobot.mRadio.writeChannel(RadioChannels.SECOND_MEDBAY_CLAIMED, Clock.getRoundNum());
					return;
				}
			}
			
		}
		
		
		
		int closestDist = MAX_DIST_SQUARED;
		int tempDist;
		int badLocations = 0;
		int badLocsTwo = 0;
		RobotInfo tempRobotInfo;
		MapLocation closestEnemy=null;
		
		for (int i = enemyRobots.length; --i >= 0;) {
			tempRobotInfo = mRC.senseRobotInfo(enemyRobots[i]);
			int diffX = mRC.getLocation().x - tempRobotInfo.location.x;
			int diffY = mRC.getLocation().y - tempRobotInfo.location.y;
			tempDist = Math.max(Math.abs(diffX), Math.abs(diffY));
			if ( tempRobotInfo.type == RobotType.ARTILLERY ) {				
				SoldierRobot.mRadio.writeChannel(RadioChannels.ENEMY_HAS_ARTILLERY_NORMAL, 1);
				SoldierRobot.enemyHasArtillery = true;
			}
			if(tempDist == 3 && tempRobotInfo.roundsUntilMovementIdle == 0
					&& tempRobotInfo.type == RobotType.SOLDIER ){
				if ( mRC.senseNearbyGameObjects(Robot.class, tempRobotInfo.location, 2, SoldierRobot.mTeam).length ==0 ) {
					badLocations |= SoldierRobot.THREE_AWAY_BITS[6-(diffX + 3)][6-(diffY + 3)];
				}
			}
			if ( tempDist == 2 && tempRobotInfo.roundsUntilMovementIdle == 0 && tempRobotInfo.type == RobotType.SOLDIER)  {
				badLocsTwo |= SoldierRobot.THREE_AWAY_BITS[6-(diffX + 3)][6-(diffY + 3)];
			}
			if (tempDist<closestDist ) {
				
				closestDist = tempDist;
				closestEnemy = tempRobotInfo.location;
			}
		}
		float randomNumber = ARobot.rand.nextFloat();
		
		if(closestDist < 3 ){			
			badLocations = 0;
		}
		else if ( SoldierRobot.rand.nextFloat() < (numSensorAllies-numSensorEnemies)*BREAK_TWO_SQUARES_PROB_NO_NUKE ) {
			badLocations = 0; 
		}
		else if ( SoldierRobot.enemyNukingFast && SoldierRobot.rand.nextFloat() < BREAK_TWO_SQUARES_PROB_NUKE ) { 
			badLocations = 0;
		}		
			
		//defuse mines if there's someone in front of us
		if ( closestEnemy == null ) {
			closestEnemy = SoldierRobot.enemyHQLoc;
		}
		if(Clock.getRoundNum() - SoldierRobot.mLastAttackTurn > 9 && (hasAllyInFront(closestEnemy) && hasAllyInFront(SoldierRobot.enemyHQLoc) && SoldierRobot.enemyNukingFast)) {
			mRC.setIndicatorString(0, "defuse");
			if(randomNumber < CHANCE_OF_DEFUSING_ENEMY_MINE && (enemyRobots.length < alliedRobots.length/3)){
				if(defuseMineNear(SoldierRobot.enemyHQLoc, SoldierRobot.mEnemy))
					return;
			}
			if(randomNumber < CHANCE_OF_DEFUSING_NEUTRAL_MINE && (enemyRobots.length < alliedRobots.length/3)){
				if(defuseMineNear(SoldierRobot.enemyHQLoc, null))
					return;
			}
		}

		if(closestDist >= SOLDIER_BATTLE_FORMATION_DIST && !SoldierRobot.enemyNukingFast) {
			MapLocation enemy = SoldierRobot.getEnemyPos();
			MapLocation avg = new MapLocation((enemy.x + mRC.getLocation().x)/2, (enemy.y + mRC.getLocation().y)/2);	
			MapLocation dest = SoldierRobot.adjustPointIntoFormation(avg, 0.5f);
			goToLocation(dest, false);
			mRC.setIndicatorString(0, "battle formation " + dest);
			return;
		}
		
		Direction tempDir; 
		if ((tempDir = determineBestBattleDirection(getNeighborStats(badLocations),closestEnemy,badLocsTwo)) != null) {
			if ( tempDir.ordinal() < NUM_DIR && mRC.canMove(tempDir) ) {
				mRC.setIndicatorString(0, "battle direction " + tempDir);
				mRC.move(tempDir);
				return;
			}
		}
		
		if(SoldierRobot.enemyNukingFast) {
			goToLocation(SoldierRobot.enemyHQLoc, true);
		}
		
	}
	
	
	//Neighbor data is reversed from normal ordinal direction for speed
		//Returns least surrounded position or closest position to battle rally, or null if cannot move
		//badlocsfortwo is for places that are within striking distance of a robot if we move there
		private static Direction determineBestBattleDirection(int[] neighborData,MapLocation closestEnemy, int badLocsForTwo) throws GameActionException {
			Direction bestDir = null;
			float bestScore = 99999;
			float tempScore = 0;
			int tempNumEnemies = 0;
			int distSqrToBattleRally= 0;

			MapLocation botLoc = mRC.getLocation();
			Robot[] nearbyEnemies =mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mEnemy);
			int numNearbyEnemies = nearbyEnemies.length;
			int numNearbyAllies = mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mTeam).length;
			if (numNearbyEnemies == 1) {
				if ( mRC.senseRobotInfo(nearbyEnemies[0]).type != RobotType.SOLDIER ) {
					numNearbyEnemies--;
				}
			}
			boolean locallyOutnumbered = (neighborData[NUM_DIR] == 0 && botLoc.distanceSquaredTo(SoldierRobot.HQLoc) > SOLDIER_HQ_DEFEND_RAD && 
					((numNearbyEnemies > (numNearbyAllies*1.1)) ));
			if ( !locallyOutnumbered ) { 								
				for ( int i = NUM_DIR; --i >= 0;) {
					
					tempNumEnemies = neighborData[NUM_DIR];
					distSqrToBattleRally = botLoc.distanceSquaredTo(closestEnemy);
					if ( tempNumEnemies == 0 ) {
						tempScore = NUM_DIR + distSqrToBattleRally;					
					}
					else {
						tempScore = (tempNumEnemies << 1) - (1f/distSqrToBattleRally);
					}
					if ( tempScore <= bestScore) {
						bestDir = Direction.values()[NUM_DIR];
						bestScore = tempScore;
					}
					
					if (neighborData[i] < 100)
					{
						tempNumEnemies = neighborData[i];
						distSqrToBattleRally = nextToLocations[i].distanceSquaredTo(closestEnemy);
						if ( tempNumEnemies == 0 ) {
							tempScore = NUM_DIR + distSqrToBattleRally;					
						}
						else {												
							tempScore = (tempNumEnemies << 1) - (1f/distSqrToBattleRally); // multiply by 2 to make sure enemy # more important than rally dist
							if ( checkForMineBlock(nextToLocations[i],DIRECTION_REVERSE[i]) ) {
								tempScore += 700;							
							}
						}
						if ( tempScore < bestScore ) {
							bestDir = DIRECTION_REVERSE[i];
							bestScore = tempScore;
						}
					}
				}
			}
			else {
				mRC.setIndicatorString(0, "bad2: " + badLocsForTwo );
				MapLocation HQLoc = mRC.senseHQLocation();
				tempNumEnemies = neighborData[NUM_DIR];
				distSqrToBattleRally = mRC.getLocation().distanceSquaredTo(HQLoc);
				if ( tempNumEnemies == 0 ) {
					tempScore = -NUM_DIR - (1f/distSqrToBattleRally);					
				}
				else {
					tempScore = (tempNumEnemies << 1) - (1f/distSqrToBattleRally);
				}
				if ( tempScore <= bestScore) {
					bestDir = Direction.values()[NUM_DIR];
					bestScore = tempScore;
				}
				for ( int i = NUM_DIR; --i >= 0;) {
					if (neighborData[i] < 100 && ((badLocsForTwo >> i) & 1) != 1)
					{
						tempNumEnemies = neighborData[i];
						distSqrToBattleRally = nextToLocations[i].distanceSquaredTo(HQLoc);
						if ( tempNumEnemies == 0 ) {
							//tempScore = -1*NUM_DIR + -1*distSqrToBattleRally;
							tempScore = -1*NUM_DIR - (1f/distSqrToBattleRally);
						}
						else {
							tempScore = (tempNumEnemies << 1) - (1f/distSqrToBattleRally); // multiply by 2 to make sure enemy # more important than rally dist
						}
						if ( tempScore < bestScore ) {
							bestDir = DIRECTION_REVERSE[i];
							bestScore = tempScore;
						}
					}
				}
			}
			mRC.setIndicatorString(1, "allies:  "  + numNearbyAllies + "outnubmered: " + locallyOutnumbered + "enemies " + numNearbyEnemies + "round" + Clock.getRoundNum());
			//mRC.setIndicatorString(1, "bytecode used for determine: " + (a - Clock.getBytecodesLeft()));
			return bestDir;

		}
		
		public static boolean checkForMineBlock(MapLocation mp, Direction fromDir) {
			
			MapLocation[] nonAllyMines = mRC.senseNonAlliedMineLocations(mp, 2);
			
			int tempDiff;
			MapLocation roboLoc  = mRC.getLocation();
			int diffXRobo = mp.x-roboLoc.x;
			int diffYRobo = mp.y-roboLoc.y;		
			
			int mostRight = -9, mostLeft = -9;
			int numNonAllyMines = nonAllyMines.length;
			
			for ( int i = numNonAllyMines; --i >= 0; ) {
				tempDiff = (mp.directionTo(nonAllyMines[i]).ordinal() - fromDir.ordinal());							
				if ( Math.abs(tempDiff) > NUM_DIR/2 || tempDiff > 0) {
					if ( mostRight == -9  || mostRight > tempDiff ) {
						mostRight = tempDiff; 
					}									
				}
				else if (tempDiff < 0 ){
					
					if ( mostLeft == -9 || mostLeft < tempDiff ) 
					{
						mostLeft = tempDiff;
					}
				}
				else { // two directions same 								
					if ( mostLeft == -9 ) {
						mostLeft = tempDiff;
					}
					else if ( mostRight == -9 || mostRight < tempDiff) {
						mostRight = tempDiff;
					}
					else if ( mostLeft < tempDiff ) {
						mostLeft = tempDiff; 
					}
					else if ( mostRight > tempDiff ) {
						mostRight = tempDiff;
					}
				}
			}
			int tempX; 
			int tempY;
			Direction tempDir;
			int boundLeft = (mostLeft +fromDir.ordinal() + 8)%8;
			int boundRight = (mostRight + fromDir.ordinal() + 8)%8;
			
			if (mostLeft != -9 && mostRight != -9 ) {
				for ( int i = boundLeft; i != boundRight; i = (i + 1) % 8 )  {
					tempDir = Direction.values()[i];
					tempX = (tempDir.dx + diffXRobo + 2); 
					tempY = (tempDir.dy + diffYRobo + 2);
									
					if ( enemyThere[tempX][tempY] ) {
						return true;
					}
				}
			}
			return false;
		}
			
		//returns the number of enemy/allied robots if a robot were to go in each direction.  
		//number of allied is in 10s place, number of enemies is in 1s, a 100 means the direction is blocked
		public static int[] getNeighborStats(int badLocs) throws GameActionException {

			
			enemyThere = new boolean[5][5]; //google tells me this is initialized to false 
			//TODO: Make this use a faster arraylist		

			Robot[] NearbyRobots =  mRC.senseNearbyGameObjects(Robot.class, 2*2 + 2*2,ARobot.mEnemy); //2 in either direction

			MapLocation roboLoc = mRC.getLocation();

			//This array is NUM_DIR + 1 0s, the +1 is for the not moving location
			int[] eachDirectionStats = new int [NUM_DIR + 1];
			
			//TODO: Mine and enemy list
			ArrayList<LocationAndIndex> directionLocs = new ArrayList<LocationAndIndex>();
			nextToLocations = new MapLocation[8];
			Direction tempDir;
			MapLocation tempLoc;

			//Initialize all the locations
			for (int i = NUM_DIR; --i >= 0;) {
				tempDir = DIRECTION_REVERSE[i];
				tempLoc = roboLoc.add(tempDir);
				nextToLocations[i] = tempLoc;
				if ( !isMineDirDanger(tempLoc) && mRC.canMove(tempDir) && ((badLocs >> (i)) & 1) != 1) {

					directionLocs.add(new LocationAndIndex(roboLoc.add(tempDir),i));
				}
				else {
					eachDirectionStats[i] = 100; //This signifies the spot is not movable
				}
			}

			//Go through all the robots and see if they're near any of the squares next to us
			MapLocation tempLocation = null;
			int nearbyRobotsLength = NearbyRobots.length;
			int directionLocsLength = directionLocs.size();
			int j;
			for ( int i = nearbyRobotsLength; --i >=0;) {
				tempLocation = mRC.senseRobotInfo(NearbyRobots[i]).location;
				
				//This is to be used later on with the mine sensing
				int diffX = roboLoc.x -tempLocation.x;
				int diffY = roboLoc.y -tempLocation.y;
				if ( Math.abs(diffX) <= 2 && Math.abs(diffY) <= 2 ) {
					enemyThere[4 - (diffX + 2 )][4-(diffY + 2)] = true;
				}		
				for ( j = directionLocsLength; --j >= 0; ) {
					if ( tempLocation.distanceSquaredTo(directionLocs.get(j).mp) <= 2 ) { // 2 means directly next to us					
						eachDirectionStats[directionLocs.get(j).i] += 1;
					}
				}
				if ( tempLocation.distanceSquaredTo(roboLoc) <= 2 ) {
					eachDirectionStats[NUM_DIR] += 1;				
				}
			}
			return eachDirectionStats;
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
	
	private static void gotoShieldLogic () throws GameActionException {
		int shieldsRead = SoldierRobot.mRadio.readChannel(RadioChannels.SHIELD_LOCATION);
		if ( shieldsRead == 0 ) {
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}		
		if ( mRC.getShields() < 70 ) {
			goToLocation(indexToLocation(shieldsRead));
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
		if(!goToLocation(SoldierRobot.enemyHQLoc, true)) {
			if(!defuseMineNear(SoldierRobot.enemyHQLoc)) {
				Direction dir = mRC.getLocation().directionTo(SoldierRobot.enemyHQLoc);
				for(int d:testDirOrderAll){
					Direction cur = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
					if(defuseMineNear(SoldierRobot.enemyHQLoc.add(cur))) {
						return;
					}
				}
			}
		}
	}
	
}