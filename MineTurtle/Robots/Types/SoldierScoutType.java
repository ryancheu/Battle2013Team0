package MineTurtle.Robots.Types;


import static MineTurtle.Robots.ARobot.mRC;
import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

import MineTurtle.Robots.ARobot;
import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.SoldierRobot.SoldierState;
import MineTurtle.Util.RadioChannels;
import battlecode.common.*;

public class SoldierScoutType {
	
	private static MapLocation[] waypoints;
	
	public static void run() throws GameActionException {
		
		if ( mRC.isActive() ) {
			switch(SoldierRobot.getState())
			{
			case COMPUTE_SCOUT_PATH: {
				computeScoutPath();
				break;
			}
			case SCOUT: {
				scoutState();
				break;
			}
			default:
				break;			
			}
		}
		
		if(waypoints == null)
			waypoints = findWaypoints(mRC.getLocation(), mRC.senseEnemyHQLocation());
	}
	
	private static void computeScoutPath() throws GameActionException {
		if(waypoints != null){
			SoldierRobot.switchState(SoldierState.SCOUT);
			SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SCOUT_WAYPOINTS, waypoints.length);
			for(int n=0; n<waypoints.length; ++n){
				SoldierRobot.mRadio.writeChannel(RadioChannels.SCOUT_WAYPOINTS_START + n, locationToIndex(waypoints[n]));
			}
			return;
		}
		
		// Lay mines until we find the waypoints
		if (mRC.senseMine(mRC.getLocation()) == null) {
			mRC.layMine();
			return;
		}

		// Try going away from HQ
		goToLocation(SoldierRobot.enemyHQLoc);
	}

	private static void scoutState() throws GameActionException {
		
		Robot[] nearbyEnemies = mRC.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS, SoldierRobot.mEnemy);
		
		if(nearbyEnemies.length > 0) {
			int closestDist = MAX_DIST_SQUARED;
			int tempDist;
			RobotInfo tempRobotInfo;
			MapLocation closestEnemy=null;
			for (Robot arobot:nearbyEnemies) {
				tempRobotInfo = mRC.senseRobotInfo(arobot);
				if(tempRobotInfo.type != RobotType.SOLDIER)
					continue;
				tempDist = tempRobotInfo.location.distanceSquaredTo(mRC.getLocation());
				if (tempDist<closestDist) {
					closestDist = tempDist;
					closestEnemy = tempRobotInfo.location;
				}
			}
			if(closestEnemy != null){
				goToLocation(mRC.getLocation().add(mRC.getLocation().directionTo(closestEnemy).opposite()), false);
				mRC.setIndicatorString(2, "Run away!");
				return;
			}
		}
		goToLocation(findNextWaypoint(waypoints));
		mRC.setIndicatorString(2, findNextWaypoint(waypoints).toString());

	}
}
