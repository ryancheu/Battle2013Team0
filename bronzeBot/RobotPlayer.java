package bronzeBot;

import battlecode.common.*;
import bronzeBot.Robots.ARobot;
import bronzeBot.Robots.ArtilleryRobot;
import bronzeBot.Robots.GeneratorRobot;
import bronzeBot.Robots.HQRobot;
import bronzeBot.Robots.MedbayRobot;
import bronzeBot.Robots.SoldierRobot;
import bronzeBot.Robots.SupplierRobot;

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
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			rc.yield();
		}
					
	}
	
	
}
