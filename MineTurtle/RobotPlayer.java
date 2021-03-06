package MineTurtle;

import MineTurtle.Robots.ARobot;
import MineTurtle.Robots.ArtilleryRobot;
import MineTurtle.Robots.GeneratorRobot;
import MineTurtle.Robots.HQRobot;
import MineTurtle.Robots.MedbayRobot;
import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.SupplierRobot;
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
			case MEDBAY: {
				theRobot = new MedbayRobot(rc);
				break;
			}
			case SUPPLIER: {
				theRobot = new SupplierRobot(rc);
				break;
			}
			case GENERATOR: {
				theRobot = new GeneratorRobot(rc);
				break;
			}
			default: {
				theRobot = new ARobot(rc);
				break;
			}
		}
		
		while(true) {
			try {
				theRobot.takeTurn();
			} catch (Exception e) {
				e.printStackTrace();
				rc.setIndicatorString(2, e.toString());
			}
			rc.yield();
		}
					
	}
	
	
}
