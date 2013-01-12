package MineTurtle.Robots.Types;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

import MineTurtle.Robots.ARobot;
import MineTurtle.Robots.SoldierRobot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import static MineTurtle.Robots.ARobot.mRC;
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

		// If current location is blank, lay a mine there
		if (mRC.senseMine(mRC.getLocation()) == null) {
			mRC.layMine();
			return;
		}

		// Otherwise try to go towards the HQ and lay a mine
		Direction tempDir = null;
		Direction dirToDest = mRC.getLocation().directionTo(mRC.senseHQLocation());		
		for (int i : testDirOrderAll) {
			if (mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
					&& !isMineDir(mRC.getLocation(), tempDir)) {
				mRC.move(tempDir);				
				break;
			}
		}

		// Try going away from HQ
		goToLocation(SoldierRobot.enemyHQLoc);

	}
}