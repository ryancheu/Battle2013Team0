package MineTurtle.Robots.Types;

import static MineTurtle.Util.Constants.*;

import static MineTurtle.Robots.ARobot.mRC;
import static MineTurtle.Util.Util.*; 

import MineTurtle.Robots.ARobot;
import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.SoldierRobot.SoldierState;
import battlecode.common.*;

public class SoldierScoutType {
	
	private static MapLocation[] waypoints;
	
	public static void run() throws GameActionException {
		
		/*
		//Perfrom census
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0) {
			int count = SoldierRobot.mRadio.readChannel(COUNT_MINERS_RAD_CHAN);
			SoldierRobot.mRadio.writeChannel(COUNT_MINERS_RAD_CHAN, count + 1);
		}
		*/
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
			return;
		}
		
		// If current location is blank, lay a mine there
		if (mRC.senseMine(mRC.getLocation()) == null) {
			mRC.layMine();
			return;
		}

		// Try going away from HQ
		goToLocation(SoldierRobot.enemyHQLoc);
	}

	private static void scoutState() throws GameActionException {
		
		Robot[] nearbyEnemies = mRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.sensorRadiusSquared, SoldierRobot.mEnemy);
		
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
			if(closestEnemy != null)
				goToLocation(mRC.getLocation().add(mRC.getLocation().directionTo(closestEnemy).opposite()), false);
			return;
		}
		goToLocation(findNextWaypoint(waypoints));

	}
}
