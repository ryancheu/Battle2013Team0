package BaseBot.Robots.Types;


import BaseBot.Robots.ARobot;
import static BaseBot.Util.NonConstants.*;
import BaseBot.Robots.SoldierRobot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;
import static BaseBot.Robots.ARobot.mRC;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.Util.*;
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

		// If we see an enemy, turn into an army robot
		if (mRC.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS,
				SoldierRobot.mEnemy).length > 0){
			SoldierRobot.switchType(SoldierRobot.SoldierType.ARMY);
			SoldierRobot.switchState(SoldierRobot.SoldierState.GOTO_RALLY);
		}
		boolean hasPickaxe = mRC.hasUpgrade(Upgrade.PICKAXE);
		// If current location is blank, lay a mine there
		if (!hasPickaxe && mRC.senseMine(mRC.getLocation()) == null && (mRC.getLocation().x + mRC.getLocation().y)%2 == 0) {
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
		if(hasPickaxe){
			dirToDest = mRC.getLocation().directionTo(SoldierRobot.enemyHQLoc);	
		}
		for (int i : testDirOrderAll) {
			if (!hasPickaxe
					&& mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
					&& !isMineDir(mRC.getLocation(), tempDir)
					&& (mRC.getLocation().add(tempDir).x + mRC.getLocation().add(tempDir).y)%2 == 0) {
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
		else {
	
			// Try going away from HQ
			goToLocation(SoldierRobot.enemyHQLoc);
		}

	}
}