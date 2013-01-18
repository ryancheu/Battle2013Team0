package RushBot;

import RushBot.Robots.ARobot;
import RushBot.Robots.ArtilleryRobot;
import RushBot.Robots.GeneratorRobot;
import RushBot.Robots.HQRobot;
import RushBot.Robots.MedbayRobot;
import RushBot.Robots.SoldierRobot;
import RushBot.Robots.SupplierRobot;
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
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			rc.yield();
		}
					
	}
	
	
}
