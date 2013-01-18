package pickaxeNuke.Robots.Types;

import static pickaxeNuke.Util.Constants.*;
import pickaxeNuke.Robots.ArtilleryRobot;
import pickaxeNuke.Robots.ARobot;
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

		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,RobotType.ARTILLERY.attackRadiusMaxSquared,ARobot.mEnemy);
		MapLocation[] enemyRobotLocations = new MapLocation[enemyRobots.length];
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,RobotType.ARTILLERY.attackRadiusMaxSquared,ARobot.mTeam);
		MapLocation[] alliedRobotLocations = new MapLocation[alliedRobots.length];
		int enemyBotIndex = 0;
		for(Robot enemyBot : enemyRobots) {
			enemyRobotLocations[enemyBotIndex] = rc.senseRobotInfo(enemyBot).location;
			enemyBotIndex++;
		}
		int alliedBotIndex = 0;
		for(Robot friendBot : alliedRobots) {
			alliedRobotLocations[alliedBotIndex] = rc.senseRobotInfo(friendBot).location;
			alliedBotIndex++;
		}
		int maxIndex = 0;
		int maxDamage = 40;
		for(int enemyIndex = 0; enemyIndex < enemyRobotLocations.length;++enemyIndex) {
			int tempDamage = 40;
			for(int adjacentEnemyIndex = 0; adjacentEnemyIndex < enemyRobotLocations.length;++adjacentEnemyIndex) {
				if(enemyRobotLocations[enemyIndex].isAdjacentTo(enemyRobotLocations[adjacentEnemyIndex])) {
					tempDamage+=20;
				}
			}
			for(int adjacentAllyIndex = 0; adjacentAllyIndex < alliedRobotLocations.length;++adjacentAllyIndex) {
				if(enemyRobotLocations[enemyIndex].isAdjacentTo(alliedRobotLocations[adjacentAllyIndex])) {
					tempDamage-=20;
				}
			}
			if(tempDamage>maxDamage) {
				maxDamage = tempDamage;
				maxIndex = enemyIndex;
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
