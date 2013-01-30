package BaseBot.Robots.Types;

import static BaseBot.Util.Constants.*;
import static BaseBot.Util.NonConstants.*;
import BaseBot.Robots.ARobot;
import BaseBot.Robots.ArtilleryRobot;
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
		double[] enemyShields = new double[enemyRobots.length];
		boolean everyoneHasAboveSixtyShield = true;
		for(int enemyBotIndex = 0; enemyBotIndex<enemyRobots.length;enemyBotIndex++) {
			enemyRobotLocations[enemyBotIndex] = rc.senseRobotInfo(enemyRobots[enemyBotIndex]).location;
			enemyShields[enemyBotIndex] = rc.senseRobotInfo(enemyRobots[enemyBotIndex]).shields;
			if(enemyShields[enemyBotIndex] < 60){
				everyoneHasAboveSixtyShield = false;
			}
		}
		for(int alliedBotIndex = 0; alliedBotIndex<alliedRobots.length;alliedBotIndex++) {
			alliedRobotLocations[alliedBotIndex] = rc.senseRobotInfo(alliedRobots[alliedBotIndex]).location;
		}
		int maxIndex = 0;
		int maxDamage = 0;
		int tempDamage = 0;
		int maxShieldIndex = 0;
		int maxShieldDamage = 0;
		int tempShieldDamage = 0;
		for(int enemyIndex = 0; enemyIndex < enemyRobotLocations.length;++enemyIndex) {
			tempDamage = 0;
			tempShieldDamage = 60;
			if(enemyShields[enemyIndex] <= 20.0){
				tempDamage = 40;
			}
			else if(enemyShields[enemyIndex]>60.0){
				tempDamage = 0;
			}
			else{
				tempDamage = (int)(60 - enemyShields[enemyIndex]);
			}
			for(int adjacentEnemyIndex = 0; adjacentEnemyIndex < enemyRobotLocations.length;++adjacentEnemyIndex) {
				if(enemyRobotLocations[enemyIndex].isAdjacentTo(enemyRobotLocations[adjacentEnemyIndex])) {
					if(enemyShields[adjacentEnemyIndex] <= 1.0){
						tempDamage+=(RobotType.ARTILLERY.attackPower*GameConstants.ARTILLERY_SPLASH_RATIO);
					}
					else if(enemyShields[adjacentEnemyIndex] <= 15.0){
						tempDamage+=(int)((RobotType.ARTILLERY.attackPower*GameConstants.ARTILLERY_SPLASH_RATIO) - enemyShields[enemyIndex]);
					}
					tempShieldDamage+=(RobotType.ARTILLERY.attackPower*GameConstants.ARTILLERY_SPLASH_RATIO);
				}
			}
			for(int adjacentAllyIndex = 0; adjacentAllyIndex < alliedRobotLocations.length;++adjacentAllyIndex) {
				if(enemyRobotLocations[enemyIndex].isAdjacentTo(alliedRobotLocations[adjacentAllyIndex])) {
					tempDamage-=(RobotType.ARTILLERY.attackPower*GameConstants.ARTILLERY_SPLASH_RATIO);
				}
			}
			if(tempDamage > maxDamage) {
				maxDamage = tempDamage;
				maxIndex = enemyIndex;
			}
			if(tempShieldDamage > maxShieldDamage){
				maxShieldDamage = tempShieldDamage;
				maxShieldIndex = enemyIndex;
			}
		
		}
		if(maxDamage == 0 && maxShieldDamage > 0){
			maxIndex = maxShieldIndex;
			maxDamage = 1;
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
