package MineTurtle.Robots;

import java.util.ArrayList;

import MineTurtle.Robots.Types.*;
import battlecode.common.*;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

public class SoldierRobot extends ARobot{
	
	public enum SoldierType {
		OCCUPY_ENCAMPMENT,
		LAY_MINES,
		SCOUT,
		ARMY,
		ARTILLERY
	}
	public enum SoldierState {

		// ENCAMPMENT SOLDIER
		FIND_ENCAMPMENT, 
		GOTO_ENCAMPMENT,		

		// MINE SOLDIER
		MINE,
		
		// SCOUT SOLDIER
		COMPUTE_SCOUT_PATH,
		SCOUT,
		
		//ARMY SOLDIER		
		GOTO_RALLY,
		BATTLE,
		GOTO_MEDBAY,
	}
	
	
	public static MapLocation curDest;
	public static MapLocation enemyHQLoc;
	public static ArrayList<MapLocation> wayPoints;
	
	protected static SoldierState mState;
	protected static SoldierType mType;
	
	protected static int mLastRecvWayPoint = -1;
	
	private static MapLocation mBattleRally;
	
	
	public static SoldierState getState() 
	{
		return mState;
	}
	
	public static MapLocation getDest()
	{
		return curDest;
	}
	
	//TODO: Fix this
	public static MapLocation getBattleRally() throws GameActionException 
	{
		return indexToLocation(mRC,mRadio.readChannel(ENEMY_AVG_POS_RAD_CHANNEL));
	}

	
	public SoldierRobot(RobotController rc) {
		super(rc);
		mRC = rc;
		enemyHQLoc = rc.senseEnemyHQLocation();
		wayPoints = new ArrayList<MapLocation>();
	}
	
	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		mainSoldierLogic();
	}
	private static void mainSoldierLogic()
			throws GameActionException {
		
		// First run of soldier, assign type
		if (mType == null) {
			//First, add ID to four most recent robot IDs
			int currentBotNumber = mRadio.readChannel(CURRENT_BOT_ID_CHAN);
			mRadio.writeChannel(CURRENT_BOT_ID_CHAN, currentBotNumber+1);
			mRadio.writeChannel(LAST_FOUR_BOT_ID_RAD_CHAN_START + CURRENT_BOT_ID_CHAN % NUM_ROBOTS_TO_CHECK_ID, mRC.getRobot().getID());
			setNumberOfEncampments(mRC);
			for (int i = ENC_CLAIM_RAD_CHAN_START; i < ENC_CLAIM_RAD_CHAN_START + NUM_ENC_TO_CLAIM; i++) {
				if (mRadio.readChannel(i) == -1) {
					mType = SoldierType.OCCUPY_ENCAMPMENT;
					mState = SoldierState.FIND_ENCAMPMENT;
					mRC.setIndicatorString(0, "OCCUPY_ENCAMPMENT");
				}
			}
			if ( mType == null )
			{
				int spawnMiners = mRadio.readChannel(SPAWN_MINER_RAD_CHAN);
				print(spawnMiners);
				if (spawnMiners > 0){
					mRadio.writeChannel(SPAWN_MINER_RAD_CHAN, spawnMiners - 1);
					mType = SoldierType.LAY_MINES;
					mState = SoldierState.MINE;
					mRC.setIndicatorString(0, "LAY_MINES");
				}
			}
			if ( mType == null )
			{
				int spawnScouts = mRadio.readChannel(SPAWN_SCOUT_RAD_CHAN);
				if (spawnScouts > 0){
					mRadio.writeChannel(SPAWN_SCOUT_RAD_CHAN, spawnScouts - 1);
					mType = SoldierType.SCOUT;
					mState = SoldierState.COMPUTE_SCOUT_PATH;
					mRC.setIndicatorString(0, "SCOUT");
				}
			}
			if ( mType == null )
			{
				mType = SoldierType.ARMY;
				mState = SoldierState.GOTO_RALLY;
				mRC.setIndicatorString(0, "ARMY");
			}
		}
		
		updateWayPoints(mRC); 

		switch (mType) {
			case OCCUPY_ENCAMPMENT:
				SoldierEncampmentType.run(mRC);
				break;
			case LAY_MINES:
				SoldierLayMineType.run(mRC);
				break;
			case SCOUT:
				SoldierScoutType.run(mRC);
				break;
			case ARMY:
				SoldierArmyType.run(mRC);
				break;
			default:
				// TODO: raise error
				break;
		}

	}
	public static void switchState(SoldierState state) {
		mState = state;
		mRC.setIndicatorString(1, "state: " + state.ordinal());
	}
	public static void switchType(SoldierType type) {
		mType = type; 
		mRC.setIndicatorString(type.ordinal(), "Type");
	}
	
	public static MapLocation findRallyPoint(RobotController rc) throws GameActionException {
		// TODO Auto-generated method stub
		if ( wayPoints.size() > 0 ) {
			rc.setIndicatorString(locationToIndex(rc,wayPoints.get(0)), "rally");
			return wayPoints.get(0);
		}
			
		else 
			return rc.senseHQLocation();
	}
	
	//Find nearest medbay location, right now just checks channel
	public static MapLocation findNearestMedBay(RobotController rc) throws GameActionException {
		//return rc.senseHQLocation(); //TODO: Change to real code
		return indexToLocation(rc,mRadio.readChannel(MEDBAY_LOCATION_CHAN));
	}
	
	
	public static void addWayPoint(RobotController rc,MapLocation ml) {
		wayPoints.add(ml);
	}
	public static void clearWayPoints(RobotController rc) {
		wayPoints.clear();
	}
	//Updates the way points, goes to next rally point if reached current rally point
	public static void updateWayPoints(RobotController rc) throws GameActionException {
		
		if ( wayPoints.size() > 0 && rc.getLocation().distanceSquaredTo(wayPoints.get(0)) < SOLDIER_RALLY_RAD) {
			if ( wayPoints.size() > 1 )
			{
				wayPoints.remove(0);
			}
		}
		
		int wayPointChanData = mRadio.readChannel(SOLDIER_WAYPOINT_RALLY_CHAN);
		int lastUpdated = wayPointChanData & BIT_MASKS[WAYPOINT_ROUND_BITS];
		if ( mLastRecvWayPoint < lastUpdated ) {
			int wayPointStartChan = (wayPointChanData >> WAYPOINT_ROUND_BITS) & BIT_MASKS[WAYPOINT_START_CHAN_BITS];
			int numWayPoints = (wayPointChanData >> (WAYPOINT_ROUND_BITS+WAYPOINT_START_CHAN_BITS)) & BIT_MASKS[WAYPOINT_NUM_RALLY_BITS];
			clearWayPoints(rc);
			
			for ( int i = 0; i < numWayPoints; i++ ) {
				addWayPoint(rc,indexToLocation(rc,mRadio.readChannel(wayPointStartChan + i )));				
			}			
		}		
	}	
	
}
