

package BaseBot.Robots.Types;


import BaseBot.Robots.ARobot;
import BaseBot.Robots.SoldierRobot;
import BaseBot.Robots.SoldierRobot.SoldierState;
import BaseBot.Robots.SoldierRobot.SoldierType;
import BaseBot.Util.RadioChannels;
import battlecode.common.*;
import static BaseBot.Robots.ARobot.mRC;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.NonConstants.*;
import static BaseBot.Util.Util.*;
public class SoldierLayMineType {
	
	public static void run() throws GameActionException {
		
		if ( mRC.isActive() ) {
			switch(SoldierRobot.getState())
			{
			case MINE: {
				layMineState();
				break;
			}
			default:
				break;			
			}
		}
	}
	
	private static void layMineState() throws GameActionException {
		int HQInDanger = ARobot.mRadio.readChannel(RadioChannels.HQ_IN_DANGER) ^ FIRST_BYTE_KEY;
		int fasterNuke = ARobot.mRadio.readChannel(RadioChannels.ENEMY_FASTER_NUKE);
		if ( fasterNuke == 1 )
		{
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			SoldierRobot.switchType(SoldierType.ARMY);
			return;
		}
		// If we see an enemy, turn into an army robot
		if (mRC.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS,
				SoldierRobot.mEnemy).length > 0
				|| HQInDanger!=0){
			SoldierRobot.switchType(SoldierRobot.SoldierType.ARMY);
			SoldierRobot.switchState(SoldierRobot.SoldierState.GOTO_RALLY);
			return;
		}
		boolean hasPickaxe = mRC.hasUpgrade(Upgrade.PICKAXE);
		// it isn't safe before round 50ish to lay a mine, they might rally and then you're weak and you don't have a mine field
		if(Clock.getRoundNum() > 50){
			if (!hasPickaxe && mRC.senseMine(mRC.getLocation()) == null && (mRC.getLocation().x + mRC.getLocation().y)%2 == 0) {
				mRC.layMine();
				return;
			}
			else if(hasPickaxe && mRC.senseMine(mRC.getLocation()) == null && (2*mRC.getLocation().x + mRC.getLocation().y)%5 == 0) {
				mRC.layMine();
				return;
			}
			else if(ARobot.rand.nextFloat() < .1f && hasPickaxe && mRC.senseMine(mRC.getLocation()) == null){
				mRC.layMine();
				return;

			}
		}

		
		// Otherwise try to go towards the HQ and lay a mine
		Direction bestDir = null;
		Direction tempDir = null;
		Direction dirToDest = mRC.getLocation().directionTo(SoldierRobot.HQLoc);
		for (int i : testDirOrderAll) {
			if (!hasPickaxe
					&& mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
					&& !isMineDir(mRC.getLocation(), tempDir)
					&& (mRC.getLocation().add(tempDir).x + mRC.getLocation().add(tempDir).y)%2 == 0) {
				bestDir = tempDir;				
				break;
			}
			else if(hasPickaxe
					&& mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
					&& !isMineDir(mRC.getLocation(), tempDir)
					&& (2*mRC.getLocation().add(tempDir).x + mRC.getLocation().add(tempDir).y)%5 == 0
					&& mRC.getLocation().add(tempDir).distanceSquaredTo(SoldierRobot.HQLoc) < mRC.getLocation().distanceSquaredTo(SoldierRobot.HQLoc)) {
				bestDir = tempDir;				
				break;
			}
			else if(hasPickaxe
					&& mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
					&& !isMineDir(mRC.getLocation(), tempDir)
					&& (2*mRC.getLocation().add(tempDir).x + mRC.getLocation().add(tempDir).y)%5 == 0) {
				bestDir = tempDir;
			}
			else if(hasPickaxe
					&& bestDir == null
					&& mRC.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) 
					&& !isMineDir(mRC.getLocation(), tempDir)) {
				bestDir = tempDir;
			}
		}
		if(bestDir != null){
			mRC.move(bestDir);
		}
		else {
			/*
			//find closest ally
			Robot[] allies = mRC.senseNearbyGameObjects(Robot.class,RobotType.SOLDIER.sensorRadiusSquared,SoldierRobot.mTeam);
			
			
			if (allies.length > 0){
				int closestDistance = MAX_DIST_SQUARED;
				MapLocation closestLoc = null;
				int tempIndex;
				int tempDistance;
				for(tempIndex = allies.length;tempIndex-- > 0;){
					MapLocation tempLoc = mRC.senseRobotInfo(allies[tempIndex]).location;
					tempDistance = mRC.getLocation().distanceSquaredTo(tempLoc);
					if(tempDistance < closestDistance){
						closestDistance = tempDistance;
						closestLoc = tempLoc;
					}
				}
				goToLocation(mRC.getLocation().add(closestLoc.directionTo(mRC.getLocation())));
			}
			
			// Try going away from HQ
			else{
			*/
			//goToLocation(mRC.getLocation().add(SoldierRobot.HQLoc.directionTo(mRC.getLocation())));
			goToLocation(SoldierRobot.enemyHQLoc);
			//}
			
		}

	}
}