package MineTurtle.Robots.Types;

import static MineTurtle.Util.Constants.*;
import MineTurtle.Robots.ArtilleryRobot;
import MineTurtle.Robots.HQRobot;
import MineTurtle.Robots.HQRobot.HQState;
import battlecode.common.*;

public class ArtilleryNormalType {
	public static void run(RobotController rc) throws GameActionException
	{
		if ( rc.isActive() ) {
			switch(ArtilleryRobot.getState())
			{
				case FIRE: {
					artilleryFireState(rc);
					break;
				}
				default:
				{
					break;
				}
			}
		}
	}
	
	private static void artilleryFireState(RobotController rc) throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,RobotType.ARTILLERY.attackRadiusMaxSquared,ArtilleryRobot.mEnemy);
		MapLocation[] robotLocations = new MapLocation[enemyRobots.length];
		
		RobotInfo tempRoboInfo;
		MapLocation tempMapLoc;		
		int i = 0;
		for(Robot bot : enemyRobots) {
			tempRoboInfo = rc.senseRobotInfo(bot);
			tempMapLoc = tempRoboInfo.location;
			robotLocations[i++] = tempMapLoc;			
		}
		
		int maxIndex = 0;
		int maxAdjacent = 0;
		for(int j = 0; j < robotLocations.length;++j) {
			int numberOfAdjacent = 0;
			for(int k = 0; k < robotLocations.length;++k) {
				if(robotLocations[j].isAdjacentTo(robotLocations[k])) {
					++numberOfAdjacent;
				}
				if(numberOfAdjacent>maxAdjacent) {
					maxAdjacent = numberOfAdjacent;
					maxIndex = j;
				}
			}
		
		}
		if((maxAdjacent>0 || ArtilleryRobot.lastRoundShot == 0 || Clock.getRoundNum()-ArtilleryRobot.lastRoundShot > LAST_ROUND_SHOT_DELAY + rc.getType().attackDelay) 
				&& robotLocations.length > 0 && rc.canAttackSquare(robotLocations[maxIndex])) {
			rc.attackSquare(robotLocations[maxIndex]);
			ArtilleryRobot.lastRoundShot = Clock.getRoundNum();
		}
	}
}
