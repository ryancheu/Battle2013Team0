package pickaxeNuke.Robots.Types;

import static pickaxeNuke.Util.Constants.*;
import pickaxeNuke.Robots.ArtilleryRobot;
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
		MapLocation[] enemyRobotLocations = new MapLocation[enemyRobots.length];
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,RobotType.ARTILLERY.attackRadiusMaxSquared,ArtilleryRobot.mTeam);
		MapLocation[] alliedRobotLocations = new MapLocation[alliedRobots.length];
		RobotInfo tempRoboInfo;
		MapLocation tempMapLoc;		
		int i = 0;
		for(Robot enemyBot : enemyRobots) {
			enemyRobotLocations[i++] = rc.senseRobotInfo(enemyBot).location;			
		}
		for(Robot friendBot : alliedRobots) {
			alliedRobotLocations[i++] = rc.senseRobotInfo(friendBot).location;
		}
		int maxIndex = 0;
		int maxDamage = 40;
		for(int j = 0; j < enemyRobotLocations.length;++j) {
			int tempDamage = 40;
			for(int k = 0; k < enemyRobotLocations.length;++k) {
				if(enemyRobotLocations[j].isAdjacentTo(enemyRobotLocations[k])) {
					tempDamage+=20;
				}
			}
			for(int k = 0; k < alliedRobotLocations.length;++k) {
				if(enemyRobotLocations[j].isAdjacentTo(alliedRobotLocations[k])) {
					tempDamage-=20;
				}
			}
			if(tempDamage>maxDamage) {
				maxDamage = tempDamage;
				maxIndex = j;
			}
		
		}
		if((maxDamage > 40 
				|| ArtilleryRobot.lastRoundShot == 0 
				|| Clock.getRoundNum()-ArtilleryRobot.lastRoundShot > LAST_ROUND_SHOT_DELAY + rc.getType().attackDelay) 
				&& maxDamage > 0
				&& enemyRobotLocations.length > 0 
				&& rc.canAttackSquare(enemyRobotLocations[maxIndex])) {
			rc.attackSquare(enemyRobotLocations[maxIndex]);
			ArtilleryRobot.lastRoundShot = Clock.getRoundNum();
		}
	}
}
