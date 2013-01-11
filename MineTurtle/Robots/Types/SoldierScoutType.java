package MineTurtle.Robots.Types;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.SoldierRobot.SoldierState;
import battlecode.common.*;

public class SoldierScoutType {
	
	private static MapLocation[] waypoints;
	
	public static void run(RobotController rc) throws GameActionException {
		
		/*
		//Perfrom census
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0) {
			int count = SoldierRobot.mRadio.readChannel(COUNT_MINERS_RAD_CHAN);
			SoldierRobot.mRadio.writeChannel(COUNT_MINERS_RAD_CHAN, count + 1);
		}
		*/
		if ( rc.isActive() ) {
			switch(SoldierRobot.getState())
			{
			case COMPUTE_SCOUT_PATH: {
				computeScoutPath(rc);
				break;
			}
			case SCOUT: {
				scoutState(rc);
				break;
			}
			default:
				break;			
			}
		}
		
		if(waypoints == null)
			waypoints = findWaypoints(rc, rc.getLocation(), rc.senseEnemyHQLocation());
	}
	
	private static void computeScoutPath(RobotController rc) throws GameActionException {
		if(waypoints != null){
			SoldierRobot.switchState(SoldierState.SCOUT);
			return;
		}
		
		// If current location is blank, lay a mine there
		if (rc.senseMine(rc.getLocation()) == null) {
			rc.layMine();
			return;
		}

		// Try going away from HQ
		goToLocation(rc, SoldierRobot.enemyHQLoc);
	}

	private static void scoutState(RobotController rc) throws GameActionException {
		
		Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mEnemy);
		
		if(nearbyEnemies.length > 0) {
			int closestDist = MAX_DIST_SQUARED;
			int tempDist;
			RobotInfo tempRobotInfo;
			MapLocation closestEnemy=null;
			for (Robot arobot:nearbyEnemies) {
				tempRobotInfo = rc.senseRobotInfo(arobot);
				if(tempRobotInfo.type != RobotType.SOLDIER)
					continue;
				tempDist = tempRobotInfo.location.distanceSquaredTo(rc.getLocation());
				if (tempDist<closestDist) {
					closestDist = tempDist;
					closestEnemy = tempRobotInfo.location;
				}
			}
			if(closestEnemy != null)
				goToLocation(rc, rc.getLocation().add(rc.getLocation().directionTo(closestEnemy).opposite()), false);
			return;
		}
		goToLocation(rc, findNextWaypoint(rc, waypoints));

	}
}
