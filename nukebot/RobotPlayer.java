package nukebot;

import java.util.ArrayList;

import battlecode.common.*;

/**
 * The example funcs player is a player meant to demonstrate basic usage of the
 * most common commands. Robots will move around randomly, occasionally mining
 * and writing useless messages. The HQ will spawn soldiers continuously.
 */
public class RobotPlayer {

	public enum SoldierType {
		
	}

	public enum SoldierState {
	}

	// Battlecode constants
	private static final int MAX_DIST_SQUARED = 70 * 70;
	private static final int NUM_DIR = 8;

	// Player specific
	
	// Player Consts

	// HQ Consts

	// Radio Consts

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
		enHQPos = rc.senseEnemyHQLocation();
		
		
		if (myType == RobotType.HQ) {
			enHQDir = rc.getLocation().directionTo(enHQPos);
			myLocation = rc.getLocation();
		}
		while (true) {
			int before = Clock.getBytecodeNum();
			int constantMapWidth = 70;
			int after = Clock.getBytecodeNum();
			//System.out.println(constantMapWidth);
			//System.out.println(after - before);
			try {
				if (rc.isActive() && myType == RobotType.HQ) {				
					rc.researchUpgrade(Upgrade.NUKE);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// End turn
			rc.yield();
			
		}
	}
	
}