package MicroTest4.Robots.Types;



import static MicroTest4.Robots.ARobot.mRC;
import static MicroTest4.Util.Constants.*;
import static MicroTest4.Util.NonConstants.*;
import static MicroTest4.Util.Util.*;

import MicroTest2.Util.Constants;
import MicroTest4.Robots.ARobot;
import MicroTest4.Robots.SoldierRobot;
import MicroTest4.Robots.SoldierRobot.SoldierState;
import MicroTest4.Util.RadioChannels;
import battlecode.common.*;

public class SoldierSuicideScoutType {
	
	private static MapLocation[] waypoints;
	private static MapLocation dest;
	private static int[] seenIDs = new int[50];
	private static int enemySoldierCount;
	private static int enemySoldierOnEncampmentCount;
	private static int enemyGeneratorCount;
	private static int enemySupplierCount;
	private static int enemyArtilleryCount;
	
	private static boolean foundPathToEnemy = false;
	private static int enemyPathLastComputed = -SCOUT_RECOMPUTE_PATH_INTERVAL;
	private static int timeout = 0;
	
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
		else {
			Robot[] nearbyRobots = mRC.senseNearbyGameObjects(Robot.class,
					RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS, SoldierRobot.mEnemy);
			MapLocation roboLoc = mRC.getLocation();
			RobotInfo tempRobotInfo;
			int type = 0;
			int typesFound = 0;
			int diffX;
			int diffY;
			int potentialDamage = 0;
			
			if(nearbyRobots.length > 0) {								
				for ( int i = nearbyRobots.length; --i >= 0; )
				{
					tempRobotInfo = mRC.senseRobotInfo(nearbyRobots[i]);
					diffX = Math.abs(tempRobotInfo.location.x - roboLoc.x);
					diffY = Math.abs(tempRobotInfo.location.y - roboLoc.y);
					
					if ( Math.max( diffX, diffY) <= 2 ) {
						potentialDamage += 6;
					}
					if ( isNewID(nearbyRobots[i].getID())) {
						
						type = countRobot(tempRobotInfo);
						if ( type != 0 ) {
							typesFound |=  1 << (type-1);
						}
					}
				}
				if ( potentialDamage >= mRC.getEnergon()) {
					typesFound |= COULD_DIE_FLAG;
				}								
			}
			typesFound |= SCOUT_ALIVE_FLAG;
			ARobot.mRadio.writeChannel(RadioChannels.SCOUT_FOUND_NEW, typesFound);
		}
		
		if(waypoints == null && dest != null)
			waypoints = findWaypoints(foundPathToEnemy ? mRC.getLocation() : mRC.senseHQLocation(),
					dest);
	}
	
	private static void pickDestination() {		
		dest = SoldierRobot.enemyHQLoc;
		foundPathToEnemy = false;
	}
	
	private static void computeScoutPath() throws GameActionException {
		SoldierRobot.mRadio.writeChannel(RadioChannels.SCOUT_FOUND_NEW, SCOUT_ALIVE_FLAG);
		if(waypoints != null){
			if(!foundPathToEnemy) {
				SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SCOUT_WAYPOINTS, waypoints.length);
				for(int n=0; n<waypoints.length; ++n){
					SoldierRobot.mRadio.writeChannel(RadioChannels.SCOUT_WAYPOINTS_START + n, locationToIndex(waypoints[n]));
				}		
				foundPathToEnemy = true;
				enemyPathLastComputed = Clock.getRoundNum();
			}
			else {
				SoldierRobot.switchState(SoldierState.SCOUT);
				return;
			}
		}
		
		if(dest == null) {
			pickDestination();
		}
		
		Robot[] nearbyRobots = mRC.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS, SoldierRobot.mEnemy);
		
		//TODO: this code actually doesn't really do much , fix it
		if(nearbyRobots.length > 0) {
			
			MapLocation roboLoc = mRC.getLocation();
			RobotInfo tempRobotInfo;
			int type = 0;
			int typesFound = 0;
			int diffX;
			int diffY;
			int potentialDamage = 0;
			for ( int i = nearbyRobots.length; --i >= 0; )
			{
				tempRobotInfo = mRC.senseRobotInfo(nearbyRobots[i]);
				diffX = Math.abs(tempRobotInfo.location.x - roboLoc.x);
				diffY = Math.abs(tempRobotInfo.location.y - roboLoc.y);
				
				if ( Math.max( diffX, diffY) <= 2 ) {
					potentialDamage += 6;
				}
				if ( isNewID(nearbyRobots[i].getID())) {
					
					type = countRobot(tempRobotInfo);
					if ( type != 0 ) {
						typesFound |=  1 << (type-1);
					}
				}
			}
			if ( potentialDamage >= mRC.getEnergon()) {
				typesFound |= COULD_DIE_FLAG;
			}
			typesFound |= SCOUT_ALIVE_FLAG;
			ARobot.mRadio.writeChannel(RadioChannels.SCOUT_FOUND_NEW, typesFound);
			return;
		}
						
		// Try going towards destination directly
		goToLocation(dest);
	}

	private static void scoutState() throws GameActionException {		
		MapLocation movedLoc = goToLocationReturn(findNextWaypoint(waypoints),true);
		mRC.setIndicatorString(2, findNextWaypoint(waypoints).toString());
		Robot[] nearbyRobots = mRC.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS, SoldierRobot.mEnemy);
		
		RobotInfo tempRobotInfo;
		int type = 0;
		int typesFound = 0;
		int diffX;
		int diffY;
		int potentialDamage = 0;
		for ( int i = nearbyRobots.length; --i >= 0; )
		{
			tempRobotInfo = mRC.senseRobotInfo(nearbyRobots[i]);
			diffX = Math.abs(tempRobotInfo.location.x - movedLoc.x);
			diffY = Math.abs(tempRobotInfo.location.y - movedLoc.y);
			
			if ( Math.max( diffX, diffY) <= 2 ) {
				potentialDamage += 6;
			}
			if ( isNewID(nearbyRobots[i].getID())) {
				
				type = countRobot(tempRobotInfo);
				if ( type != 0 ) {
					typesFound |=  1 << (type-1);
				}
			}
		}
		if ( potentialDamage >= mRC.getEnergon()) {
			typesFound |= COULD_DIE_FLAG;
		}
		typesFound |= SCOUT_ALIVE_FLAG;		
		ARobot.mRadio.writeChannel(RadioChannels.SCOUT_FOUND_NEW, typesFound);
	}
	
	private static boolean isNewID(int ID){
		int indexID;
		for(indexID = 0; indexID < seenIDs.length ;indexID++){
			if(seenIDs[indexID] == 0 ){
				break;
			}
			else if(ID == seenIDs[indexID]){
				return false;
			}
		}
		if(indexID < seenIDs.length){
			seenIDs[indexID] = ID;
		}
		return true;
	}
	
	private static int countRobot(RobotInfo tempRobotInfo) throws GameActionException{
		if(tempRobotInfo.type == RobotType.SOLDIER){
			if(mRC.senseEncampmentSquare(tempRobotInfo.location)){
				enemySoldierOnEncampmentCount++;
				ARobot.mRadio.writeChannel(RadioChannels.ENEMY_SOLDIER_ON_ENCAMPMENT_COUNT, enemySoldierOnEncampmentCount);
				return 1;
			}
			else{
				enemySoldierCount++;
				ARobot.mRadio.writeChannel(RadioChannels.ENEMY_SOLDIER_COUNT, enemySoldierCount);
				return 2; 
			}
		}
		else if(tempRobotInfo.type == RobotType.GENERATOR){
			enemyGeneratorCount++;
			ARobot.mRadio.writeChannel(RadioChannels.ENEMY_GENERATOR_COUNT, enemyGeneratorCount);
			return 3;
		}
		else if(tempRobotInfo.type == RobotType.SUPPLIER){
			enemySupplierCount++;
			ARobot.mRadio.writeChannel(RadioChannels.ENEMY_SUPPLIER_COUNT, enemySupplierCount);
			return 4;
		}
		else if(tempRobotInfo.type == RobotType.ARTILLERY){
			enemyArtilleryCount++;
			ARobot.mRadio.writeChannel(RadioChannels.ENEMY_ARTILLERY_COUNT, enemyArtilleryCount);
			return 5;
		}
		else {
			return 0;
		}
	}
}
