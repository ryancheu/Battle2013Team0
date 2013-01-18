package pickaxeNuke.Robots.Types;


import pickaxeNuke.Robots.ARobot;
import pickaxeNuke.Robots.SoldierRobot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;
import static pickaxeNuke.Robots.ARobot.mRC;
import static pickaxeNuke.Util.Constants.*;
import static pickaxeNuke.Util.Util.*;
public class SoldierLayMineType {
	
	public static void run() throws GameActionException {
		
		if ( mRC.isActive() ) {
			switch(SoldierRobot.getState())
			{
			case MINE: {
				layMineState();
				break;
			}
			default:
				break;			
			}
		}
	}
	
	private static void layMineState() throws GameActionException {
		Robot[] nearbyEnemies = mRC.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS, SoldierRobot.mEnemy);
		
		if(nearbyEnemies.length > 0) {
			int closestDist = MAX_DIST_SQUARED;
			int tempDist;
			RobotInfo tempRobotInfo;
			MapLocation closestEnemy=null;
			for (Robot arobot:nearbyEnemies) {
				tempRobotInfo = mRC.senseRobotInfo(arobot);
				if(tempRobotInfo.type != RobotType.SOLDIER)
					continue;
				tempDist = tempRobotInfo.location.distanceSquaredTo(mRC.getLocation());
				if (tempDist<closestDist) {
					closestDist = tempDist;
					closestEnemy = tempRobotInfo.location;
				}
			}
			if(closestEnemy != null){
				goToLocation(mRC.getLocation().add(mRC.getLocation().directionTo(closestEnemy).opposite()), false);
				mRC.setIndicatorString(2, "Run away!");
				return;
			}
		}
		else{
			// If we see an enemy, turn into an army robot
			if (mRC.senseNearbyGameObjects(Robot.class,
					RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS,
					SoldierRobot.mEnemy).length > 0){
				SoldierRobot.switchType(SoldierRobot.SoldierType.ARMY);
				SoldierRobot.switchState(SoldierRobot.SoldierState.GOTO_RALLY);
			}
			boolean hasPickaxe = mRC.hasUpgrade(Upgrade.PICKAXE);
			// If current location is blank, lay a mine there
			if (!hasPickaxe && mRC.senseMine(mRC.getLocation()) == null/* && (mRC.getLocation().x + mRC.getLocation().y)%2 == 0*/) {
				mRC.layMine();
				return;
			}
			else if(hasPickaxe && mRC.senseMine(mRC.getLocation()) == null && (2*mRC.getLocation().x + mRC.getLocation().y)%5 == 0) {
				mRC.layMine();
				return;
			}

			else if(ARobot.rand.nextFloat() < .3f && hasPickaxe && mRC.senseMine(mRC.getLocation()) == null){
				mRC.layMine();
				return;

			}

			// Otherwise try to go towards the HQ and lay a mine
			Direction bestDir = null;
			Direction tempDir = null;
			Direction dirToDest = mRC.getLocation().directionTo(SoldierRobot.HQLoc);

			if(ARobot.rand.nextFloat()>.5f){
				dirToDest = mRC.getLocation().directionTo(SoldierRobot.enemyHQLoc);	
			}

			for (int i : testDirOrderAll) {
				if (!hasPickaxe
						&& mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
						&& !isMineDir(mRC.getLocation(), tempDir)
						/*&& (mRC.getLocation().add(tempDir).x + mRC.getLocation().add(tempDir).y)%2 == 0*/) {
					bestDir = tempDir;				
					break;
				}
				else if(hasPickaxe
						&& mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
						&& !isMineDir(mRC.getLocation(), tempDir)
						&& (2*mRC.getLocation().add(tempDir).x + mRC.getLocation().add(tempDir).y)%5 == 0) {
					bestDir = tempDir;				
					break;
				}
				else if(hasPickaxe
						&& bestDir == null
						&& mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
						&& !isMineDir(mRC.getLocation(), tempDir)) {
					bestDir = tempDir;
				}
			}
			if(bestDir != null){
				mRC.move(bestDir);
			}
		}

		// Try going away from HQ
		goToLocation(SoldierRobot.enemyHQLoc);

	}
}