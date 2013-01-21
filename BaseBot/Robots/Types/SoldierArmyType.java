package BaseBot.Robots.Types;

import static BaseBot.Robots.ARobot.mRC;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.NonConstants.*;
import static BaseBot.Util.Util.*;

import java.util.ArrayList;

import BaseBot.Robots.ARobot;
import BaseBot.Robots.SoldierRobot;
import BaseBot.Robots.SoldierRobot.SoldierState;
import BaseBot.Robots.SoldierRobot.SoldierType;
import BaseBot.Util.RadioChannels;
import battlecode.common.*;
public class SoldierArmyType {
	
	private static MapLocation[] medbayWaypoints;
	private static boolean[][] enemyThere;
	private static MapLocation[] nextToLocations;
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
		//if we read our position on the BECOME ENCAMPMENT channel, AND we're on an encampment
		else if(SoldierRobot.mRadio.readChannel(RadioChannels.BECOME_ENCAMPMENT)  
				== ((mRC.getLocation().x+mRC.getLocation().y*mRC.getMapWidth()) | FIRST_BYTE_KEY) 
				&& mRC.senseEncampmentSquare(mRC.getLocation())) {
			//SWITCH to encampment robot, rewrite over the channel.
			SoldierRobot.switchState(SoldierState.FIND_ENCAMPMENT);
			SoldierRobot.switchType(SoldierType.OCCUPY_ENCAMPMENT);
			SoldierRobot.mRadio.writeChannel(RadioChannels.BECOME_ENCAMPMENT,-1);
			return;
		}
		else if(SoldierRobot.mRadio.readChannel(RadioChannels.ENTER_BATTLE_STATE) == 1
				&& closestDist < SOLDIER_JOIN_ATTACK_RAD) {
			SoldierRobot.switchState(SoldierState.BATTLE);
			return;
		}
		
		// no enemies nearby, just go to the next rally point
		else if(enemyRobots.length==0 || closestDist > SOLDIER_ATTACK_RAD) {
			goToLocation(rally, shouldDefuseMines);
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
		Robot[] enemyRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mEnemy);				
		Robot[] nearbyEnemyRobots = mRC.senseNearbyGameObjects(Robot.class, SOLDIER_JOIN_ATTACK_RAD, SoldierRobot.mEnemy);
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam);
		
		if(SoldierRobot.mRadio.readChannel(RadioChannels.ENTER_BATTLE_STATE) == 0 && enemyRobots.length == 0) {
			mRC.setIndicatorString(0, "switched to rally state");
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}
		
		
		int closestDist = MAX_DIST_SQUARED;
		int tempDist;
		int badLocations = 0;
		int badLocsTwo = 0;
		RobotInfo tempRobotInfo;
		MapLocation closestEnemy=null;
		int numEnemies = enemyRobots.length;
		for (int i = numEnemies; --i >= 0;) {
			tempRobotInfo = mRC.senseRobotInfo(enemyRobots[i]);
			int diffX = mRC.getLocation().x - tempRobotInfo.location.x;
			int diffY = mRC.getLocation().y - tempRobotInfo.location.y;
			tempDist = Math.max(Math.abs(diffX), Math.abs(diffY));
			if(tempDist == 3 && (mRC.senseEncampmentSquare(tempRobotInfo.location) == false 
					|| mRC.senseRobotInfo(enemyRobots[i]).type == RobotType.SOLDIER)){
				if ( mRC.senseNearbyGameObjects(Robot.class, tempRobotInfo.location, 2, SoldierRobot.mTeam).length ==0 ) {
					badLocations |= SoldierRobot.THREE_AWAY_BITS[6-(diffX + 3)][6-(diffY + 3)];
				}
			}
			else if ( tempDist == 2 && (mRC.senseEncampmentSquare(tempRobotInfo.location) == false 
					|| mRC.senseRobotInfo(enemyRobots[i]).type == RobotType.SOLDIER) ) {
				badLocsTwo |= SoldierRobot.THREE_AWAY_BITS[6-(diffX + 3)][6-(diffY + 3)];
			}
			if (tempDist<closestDist ) {
				
				closestDist = tempDist;
				closestEnemy = tempRobotInfo.location;
			}
		}
		mRC.setIndicatorString(0, "badLocs: " + badLocations);
		if(closestDist < 3 ){			
			badLocations = 0;
		}
		else if ( !SoldierRobot.enemyNukingFast && SoldierRobot.rand.nextFloat() < BREAK_TWO_SQUARES_PROB_NO_NUKE ) {
			badLocations = 0; 
		}
		else if ( SoldierRobot.enemyNukingFast && SoldierRobot.rand.nextFloat() < BREAK_TWO_SQUARES_PROB_NUKE ) { 
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
		
		//charge the enemy HQ if we're near it
		if(mRC.getLocation().distanceSquaredTo(SoldierRobot.enemyHQLoc) < ATTACK_HQ_RAD) {
			SoldierRobot.switchState(SoldierState.ATTACK_HQ);
			return;
		}
		
		//defuse mines if there's someone in front of us
		if(hasAllyInFront(closestEnemy) && hasAllyInFront(SoldierRobot.enemyHQLoc)) {
			mRC.setIndicatorString(0, "defuse");
			if(randomNumber < CHANCE_OF_DEFUSING_ENEMY_MINE && (enemyRobots.length < alliedRobots.length/3)){
				if(defuseMineNear(SoldierRobot.enemyHQLoc, SoldierRobot.mEnemy))
					return;
			}
			if(randomNumber < CHANCE_OF_DEFUSING_NEUTRAL_MINE && (enemyRobots.length < alliedRobots.length/3)){
				if(defuseMineNear(SoldierRobot.enemyHQLoc, Team.NEUTRAL))
					return;
			}
		}

		if(closestDist >= SOLDIER_BATTLE_FORMATION_DIST) {
			MapLocation enemy = SoldierRobot.getEnemyPos();
			MapLocation avg = new MapLocation((enemy.x + mRC.getLocation().x)/2, (enemy.y + mRC.getLocation().y)/2);
			MapLocation dest = SoldierRobot.adjustPointIntoFormation(avg, 0.5f);
			goToLocation(dest, false);
			mRC.setIndicatorString(1, "battle formation " + dest);
			return;
		}
		
		Direction tempDir; 
		if ((tempDir = determineBestBattleDirection(getNeighborStats(badLocations),closestEnemy,badLocsTwo)) != null) {
			if ( tempDir.ordinal() < NUM_DIR && mRC.canMove(tempDir) ) {
				mRC.move(tempDir);
			}
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
		float numNearbyEnemies = mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mEnemy).length;
		float numNearbyAllies = mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mTeam).length;
		boolean locallyOutnumbered = (numNearbyEnemies > (numNearbyAllies*.85)) && (neighborData[NUM_DIR] == 0);
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
							tempScore += 8;
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
		mRC.setIndicatorString(1, "choose dir:  "  + bestDir + "outnubmered: " + locallyOutnumbered + "neigh data " + neighborData[NUM_DIR] + "round" + Clock.getRoundNum());
		//mRC.setIndicatorString(1, "bytecode used for determine: " + (a - Clock.getBytecodesLeft()));
		return bestDir;

	}
	
	public static boolean checkForMineBlock(MapLocation mp, Direction fromDir) {
		
		MapLocation[] nonAllyMines = mRC.senseNonAlliedMineLocations(mp, 2);
		
		int tempDiff;
		MapLocation roboLoc  = mRC.getLocation();
		int diffXRobo = roboLoc.x - mp.x;
		int diffYRobo = roboLoc.y - mp.y;		
		
		int mostRight = -1, mostLeft = -1;
		int numNonAllyMines = nonAllyMines.length;
		
		for ( int i = numNonAllyMines; --i >= 0; ) {
			tempDiff = (nonAllyMines[i].directionTo(mp).ordinal() - fromDir.ordinal());
			if ( Math.abs(tempDiff) > NUM_DIR/2 || tempDiff > 0) {
				if ( mostRight == -1  || mostRight > tempDiff ) {
					mostRight = tempDiff; 
				}									
			}
			else if (tempDiff < 0 ){
				
				if ( mostLeft == -1 || mostLeft < tempDiff ) 
				{
					mostLeft = tempDiff;
				}
			}
			else { // two directions same 								
				if ( mostLeft == -1 ) {
					mostLeft = tempDiff;
				}
				else if ( mostRight == -1 || mostRight < tempDiff) {
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
		int boundRight = (mostRight + fromDir.ordinal() + 7)%8;
		if (mostLeft != -1 && mostRight != -1 ) {
			for ( int i = boundLeft; i != boundRight; i = (i + 1) % 8 )  {
				tempDir = Direction.values()[i];
				tempX = 4 - (tempDir.dx + diffXRobo + 2); 
				tempY = 4 - (tempDir.dy + diffYRobo + 2);
				
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
			int diffX = tempLocation.x - roboLoc.x;
			int diffY = tempLocation.y - roboLoc.y;
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
	
	private static void attackHQLogic() throws GameActionException {
		/*if ( mRC.getEnergon() < SOLDIER_RUN_HEALTH ) {
			SoldierRobot.switchState(SoldierState.GOTO_MEDBAY);
			return;
		}*/
		goToLocation(SoldierRobot.enemyHQLoc, true);
	}
	
}

class LocationAndIndex {
	public MapLocation mp;
	public int i;
	public LocationAndIndex(MapLocation aMp, int index) {
		this.mp = aMp; 
		this.i = index;
	}
}