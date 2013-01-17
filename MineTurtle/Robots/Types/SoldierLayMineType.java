package MineTurtle.Robots.Types;


import MineTurtle.Robots.ARobot;
import MineTurtle.Robots.SoldierRobot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import static MineTurtle.Robots.ARobot.mRC;
import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;
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
		
		// If current location is blank, lay a mine there
		if (mRC.senseMine(mRC.getLocation()) == null && (mRC.getLocation().x + mRC.getLocation().y)%2 == 0) {
			mRC.layMine();
			return;
		}

		// Otherwise try to go towards the HQ and lay a mine
		Direction tempDir = null;
		Direction dirToDest = mRC.getLocation().directionTo(SoldierRobot.HQLoc);		
		for (int i : testDirOrderAll) {
			if (mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
					&& !isMineDir(mRC.getLocation(), tempDir)
					&& (mRC.getLocation().add(tempDir).x + mRC.getLocation().add(tempDir).y)%2 == 0) {
				mRC.move(tempDir);				
				break;
			}
		}

		// Try going away from HQ
		goToLocation(SoldierRobot.enemyHQLoc);

	}
}