package BaseBot;

import BaseBot.Robots.ARobot;
import BaseBot.Robots.ArtilleryRobot;
import BaseBot.Robots.GeneratorRobot;
import BaseBot.Robots.HQRobot;
import BaseBot.Robots.MedbayRobot;
import BaseBot.Robots.ShieldRobot;
import BaseBot.Robots.SoldierRobot;
import BaseBot.Robots.SupplierRobot;
import battlecode.common.*;

/**
 * The example funcs player is a player meant to demonstrate basic usage of the
 * most common commands. Robots will move around randomly, occasionally mining
 * and writing useless messages. The HQ will spawn soldiers continuously.
 */
public class RobotPlayer {

	static ARobot theRobot = null;

	public static void run(RobotController rc) throws GameActionException {
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
			case SHIELDS: {
				theRobot = new ShieldRobot(rc);
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
