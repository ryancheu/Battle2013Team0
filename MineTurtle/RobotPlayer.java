package MineTurtle;

import MineTurtle.Robots.ARobot;
import MineTurtle.Robots.ArtilleryRobot;
import MineTurtle.Robots.HQRobot;
import MineTurtle.Robots.SoldierRobot;
import battlecode.common.*;

/**
 * The example funcs player is a player meant to demonstrate basic usage of the
 * most common commands. Robots will move around randomly, occasionally mining
 * and writing useless messages. The HQ will spawn soldiers continuously.
 */
public class RobotPlayer {

	static ARobot theRobot = null;

	public static void run(RobotController rc) {
		switch(rc.getType()) {
			case SOLDIER: {
				theRobot = new SoldierRobot(rc);
				break;
			}
			case HQ: {
				theRobot = new HQRobot(rc); 
				break;
			}
			case ARTILLERY: {
				theRobot = new ArtilleryRobot(rc);
				break;
			}
			default: {
				//TODO: raise some error
				break;
			}
		}
		
		while(true) {
			try {
				theRobot.takeTurn();
			} catch (GameActionException e) {
				//Do nothing
			}
			rc.yield();
		}
					
	}
	
	
}
