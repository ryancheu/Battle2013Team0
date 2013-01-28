package MicroTest4;

import MicroTest4.Robots.ARobot;
import MicroTest4.Robots.ArtilleryRobot;
import MicroTest4.Robots.GeneratorRobot;
import MicroTest4.Robots.HQRobot;
import MicroTest4.Robots.MedbayRobot;
import MicroTest4.Robots.ShieldRobot;
import MicroTest4.Robots.SoldierRobot;
import MicroTest4.Robots.SupplierRobot;
import battlecode.common.*;

/**
 * The example funcs player is a player meant to demonstrate basic usage of the
 * most common commands. Robots will move around randomly, occasionally mining
 * and writing useless messages. The HQ will spawn soldiers continuously.
 * 
 * jk this isn't example funcs player. This is baseBot, a combination of our three strategies
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
