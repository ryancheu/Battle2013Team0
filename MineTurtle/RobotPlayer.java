package MineTurtle;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.Encampment;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Robot;
import battlecode.common.Team;
import battlecode.common.Upgrade;

/**
 * The example funcs player is a player meant to demonstrate basic usage of the
 * most common commands. Robots will move around randomly, occasionally mining
 * and writing useless messages. The HQ will spawn soldiers continuously.
 */
public class RobotPlayer {

	public enum SoldierType {
		OCCUPY_ENCAMPMENT, 
		LAY_MINES,
		ARTILLERY
	}

	public enum SoldierState {

		// ENCAMPMENT SOLDIER
		FIND_ENCAMPMENT, 
		GOTO_ENCAMPMENT,		

		// MINE SOLDIER
		MINE,
		
		//ARTILLERY
		FIRE,
	}

	// Battlecode constants
	private static final int MAX_DIST = 200;
	private static final int NUM_DIR = 8;

	// Player specific

	// HQ Consts
	private static final int NUM_ROBOT_TO_SPAWN = 8;

	// Player Consts
	private static final int ENC_CLAIM_RAD_CHAN_START = 100;
	private static final int NUM_ENC_TO_CLAIM = 4;

	// Used by all
	private static Team ourTeam;
	private static Team enemyTeam;
	private static RobotType myType = null;
	private static MapLocation myLocation = null;
	private static final int[] testDirOrderAll = { 0, 1, -1, 2, -2, 3, -3, 4 };
	private static final int[] testDirOrderFront = { 0, 1, -1 };
	private static final int[] testDirOrderFrontSide = { 0, 1, -1, 2, -2 };
	private static int mapWidth;
	private static int mapHeight;

	// HQ Only Variables
	private static MapLocation enHQPos = null;
	private static Direction enHQDir = null;
	private static Robot[] alliedRobots = null;

	//Soldier Variables
	private static MapLocation curDest = null;
	private static SoldierType mySoldierType = null;
	private static SoldierState mySoldierState = null;
	
	//Artillery Variables
	private static int lastRoundShot = 0;
	private static final int LAST_ROUND_SHOT_DELAY = 5;

	public static void run(RobotController rc) {
		ourTeam = rc.getTeam();
		enemyTeam = ourTeam.opponent();
		myType = rc.getType();
		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();
		if (myType == RobotType.HQ) {
			enHQPos = rc.senseHQLocation();
			enHQDir = rc.getLocation().directionTo(enHQPos);
			myLocation = rc.getLocation();
		}
		while (true) {
			if (rc.isActive()) {
				try {
					if (myType == RobotType.HQ) {
						mainHQLogic(rc);
					} else if (myType == RobotType.SOLDIER) {
						mainSoldierLogic(rc);
						myLocation = rc.getLocation();
					} else if (myType == RobotType.ARTILLERY) {
						mainArtilleryLogic(rc);
					}
					
					// End turn
					rc.yield();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	private static void mainArtilleryLogic(RobotController rc) throws GameActionException
	{
		artilleryFireLogic(rc);
	}

	private static void mainHQLogic(RobotController rc) throws GameActionException {
		alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST, ourTeam);

		if (Clock.getRoundNum() == 0) {
			System.out.println("ran hq code");
			for (int i = ENC_CLAIM_RAD_CHAN_START; i < NUM_ENC_TO_CLAIM + ENC_CLAIM_RAD_CHAN_START; ++i) {
				rc.broadcast(i, -1);
			}
		}
		if (alliedRobots.length <= NUM_ROBOT_TO_SPAWN) {
			Direction tempDir = null;
			for (int i = 0; i < NUM_ROBOT_TO_SPAWN; ++i) {
				tempDir = Direction.values()[(enHQDir.ordinal() + i + NUM_DIR) % NUM_DIR];
				if (rc.canMove(tempDir)) {
					rc.spawn(tempDir);
					break;
				}
			}
		} else {
			if (rc.hasUpgrade(Upgrade.PICKAXE)) {
				rc.researchUpgrade(Upgrade.NUKE);
			} else {
				rc.researchUpgrade(Upgrade.PICKAXE);
			}
		}
	}

	private static void mainSoldierLogic(RobotController rc)
			throws GameActionException {

		// First run of soldier, assign type
		if (mySoldierType == null) {

			for (int i = ENC_CLAIM_RAD_CHAN_START; i < ENC_CLAIM_RAD_CHAN_START
					+ NUM_ENC_TO_CLAIM; i++) {
				if (rc.readBroadcast(i) == -1) {
					mySoldierType = SoldierType.OCCUPY_ENCAMPMENT;
					mySoldierState = SoldierState.FIND_ENCAMPMENT;
				}
			}

			// If there were not enough soldiers finding encampments yet,
			// soldierType is still null
			if (mySoldierType == null) {
				mySoldierType = SoldierType.LAY_MINES;
				mySoldierState = SoldierState.MINE;
			}
		}
		
		switch (mySoldierType) {
		case OCCUPY_ENCAMPMENT:
			encampmentSoldierLogic(rc);
			break;
		case LAY_MINES:
			layMineSoldierLogic(rc);
			break;
		default:
			// TODO: raise error
			break;
		}

	}

	private static void encampmentSoldierLogic(RobotController rc)
			throws GameActionException {
		switch (mySoldierState) {
			case FIND_ENCAMPMENT: {
				findEncampmentStateLogic(rc);
				break;
			}
			case GOTO_ENCAMPMENT: {
				gotoEncampmentLogic(rc);
				break;
			}
		}
	}
	
	private static void findEncampmentStateLogic(RobotController rc) throws GameActionException
	{
		System.out.println("running enc code");
		// Using the radio broadcasts, find the encampment locations already
		// claimed by other soldiers
		int tempRead, numFound;
		ArrayList<MapLocation> claimedEncampmentLocs = new ArrayList<MapLocation>();
		for (numFound = 0; numFound < NUM_ENC_TO_CLAIM; ++numFound) {
			if ((tempRead = rc.readBroadcast(numFound + ENC_CLAIM_RAD_CHAN_START)) == -1) {
				break;
			} else {
				claimedEncampmentLocs.add(indexToLocation(tempRead));
			}
		}

		MapLocation[] allEncampments = rc.senseEncampmentSquares(myLocation, MAX_DIST, Team.NEUTRAL);
		int closestDist = MAX_DIST;
		int closestIndex = -1;
		int tempDist;		
		MapLocation tempLocation;
		boolean alreadyClaimed = false;

		// Search through all encampments and find the closest one not
		// already claimed
		System.out.println("enc length: " + allEncampments.length);
		for (int i = 0; i < allEncampments.length; i++) {
			tempLocation = allEncampments[i];

			for (MapLocation l : claimedEncampmentLocs) {
				if (l.equals(tempLocation)) {
					alreadyClaimed = true;
					System.out.println("location: " + locationToIndex(tempLocation));
					break;
				}
			}

			if (alreadyClaimed) {
				alreadyClaimed = false;
				continue;
			}

			if ((tempDist = tempLocation.distanceSquaredTo(myLocation)) < closestDist) {
				closestDist = tempDist;
				closestIndex = i;
			} else {
				System.out.println("temp dist" + tempDist);
			}
		}

		// Set the destination to the closest non-claimed encampment, and
		// claim the encampment
		if (closestIndex != -1) {
			curDest = allEncampments[closestIndex];
			rc.broadcast(ENC_CLAIM_RAD_CHAN_START + numFound, locationToIndex(curDest));
			mySoldierState = SoldierState.GOTO_ENCAMPMENT;
		} else { // There were no unclaimed encampments
			// TODO: Broadcast error message
			mySoldierType = SoldierType.LAY_MINES;
			mySoldierState = SoldierState.MINE;
		}
		return;
	}
	
	private static void gotoEncampmentLogic(RobotController rc) throws GameActionException
	{
		if (curDest.equals(myLocation)) {
			rc.captureEncampment(RobotType.ARTILLERY);
			return;
		}

		// Try to head towards the target position, no turning back (NO
		// PATHFINDING)
		// TODO: Pathfinding, mine defusion
		Direction tempDir;
		Direction dirToDest = myLocation.directionTo(curDest);
		boolean foundDir = false;
		for (int i : testDirOrderFrontSide) {
			if (rc.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR])) {
				rc.move(tempDir);
				foundDir = true;
				break;
			}
		}
		if (!foundDir) { // Blocked on all sides
			// TODO: Do something
		}
	}

	private static void layMineSoldierLogic(RobotController rc) throws GameActionException {

		// If current location is blank, lay a mine there
		if (rc.senseMine(myLocation) == null) {
			rc.layMine();
			return;
		}

		// Otherwise try to go towards the HQ and lay a mine
		Direction tempDir = null;
		Direction dirToDest = myLocation.directionTo(rc.senseHQLocation());
		boolean foundDir = false;
		for (int i : testDirOrderAll) {
			if (rc.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) && !isMineDir(rc, myLocation, tempDir)) {
				rc.move(tempDir);
				foundDir = true;
				break;
			}
		}

		// Try going away from HQ
		if (!foundDir) {
			for (int i = NUM_DIR - 1; i >= 0; --i) {
				if (rc.canMove(tempDir = Direction.values()[(i+ dirToDest.ordinal() + NUM_DIR) % NUM_DIR])) {
					rc.move(tempDir);
					foundDir = true;
					break;
				}
			}
		}

		if (!foundDir) {
			// TODO: can't move, do something
		}
	}
	
	private static void artilleryFireLogic(RobotController rc) throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,RobotType.ARTILLERY.attackRadiusMaxSquared,enemyTeam);
		MapLocation[] robotLocations = new MapLocation[enemyRobots.length];
		int i = 0;
		RobotInfo tempRoboInfo;
		MapLocation tempMapLoc;
		for(Robot bot : enemyRobots) {
			tempRoboInfo = rc.senseRobotInfo(bot);
			tempMapLoc = tempRoboInfo.location;
			robotLocations[i++] = tempMapLoc;			
		}
		int maxIndex = 0;
		int maxAdjacent = 0;
		for(int j = 0; j < robotLocations.length;++j){
			int numberOfAdjacent = 0;
			for(int k = 0; k < robotLocations.length;++k){
				if(robotLocations[j].isAdjacentTo(robotLocations[k])){
					++numberOfAdjacent;
				}
				if(numberOfAdjacent>maxAdjacent){
					maxAdjacent = numberOfAdjacent;
					maxIndex = j;
				}
			}
		
		}
		if((maxAdjacent>0 || Clock.getRoundNum()-lastRoundShot > LAST_ROUND_SHOT_DELAY + rc.getType().attackDelay) 
				&& robotLocations.length > 0 && rc.canAttackSquare(robotLocations[maxIndex])){
			rc.attackSquare(robotLocations[maxIndex]);
			lastRoundShot = Clock.getRoundNum();
		}
	}

	//UTIL FUNCTIONS 
	private static int locationToIndex(MapLocation l) {
		return mapWidth * l.y + l.x;
	}

	private static MapLocation indexToLocation(int index) {
		return new MapLocation(index % mapWidth, index / mapWidth);
	}
	//Tests for mine in direction from a location
	private static boolean isMineDir(RobotController rc, MapLocation mp, Direction d) {
		return (rc.senseMine(mp.add(d)) != null);
	}
}