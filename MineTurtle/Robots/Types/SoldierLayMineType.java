package MineTurtle.Robots.Types;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

import MineTurtle.Robots.SoldierRobot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class SoldierLayMineType {
	
	public static void run(RobotController rc) throws GameActionException {
		
		//Perfrom census
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0) {
			int count = SoldierRobot.mRadio.readChannel(COUNT_MINERS_RAD_CHAN);
			SoldierRobot.mRadio.writeChannel(COUNT_MINERS_RAD_CHAN, count + 1);
		}
		
		if ( rc.isActive() ) {
			switch(SoldierRobot.getState())
			{
			case MINE: {
				layMineState(rc);
				break;
			}
			default:
				break;			
			}
		}
	}
	
	private static void layMineState(RobotController rc) throws GameActionException {

		// If current location is blank, lay a mine there
		if (rc.senseMine(rc.getLocation()) == null) {
			rc.layMine();
			return;
		}

		// Otherwise try to go towards the HQ and lay a mine
		Direction tempDir = null;
		Direction dirToDest = rc.getLocation().directionTo(rc.senseHQLocation());		
		for (int i : testDirOrderAll) {
			if (rc.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
					&& !isMineDir(rc, rc.getLocation(), tempDir)) {
				rc.move(tempDir);				
				break;
			}
		}

		// Try going away from HQ
		goToLocation(rc, SoldierRobot.enemyHQLoc);

	}
}