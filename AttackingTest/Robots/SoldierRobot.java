package AttackingTest.Robots;

import java.util.ArrayList;


import AttackingTest.Robots.Types.*;
import battlecode.common.*;

import static AttackingTest.Util.Constants.*;
import static AttackingTest.Util.Util.*;

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
		return indexToLocation(mRadio.readChannel(ENEMY_AVG_POS_RAD_CHANNEL));
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
			setNumberOfEncampments();
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
		
		updateWayPoints(); 

		switch (mType) {
			case OCCUPY_ENCAMPMENT:
				SoldierEncampmentType.run();
				break;
			case LAY_MINES:
				SoldierLayMineType.run();
				break;
			case SCOUT:
				SoldierScoutType.run();
				break;
			case ARMY:
				SoldierArmyType.run();
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
	
	public static MapLocation findRallyPoint() throws GameActionException {
		// TODO Auto-generated method stub
		if ( wayPoints.size() > 0 ) {
			mRC.setIndicatorString(locationToIndex(wayPoints.get(0)), "rally");
			return wayPoints.get(0);
		}
			
		else 
			return mRC.senseHQLocation();
	}
	
	//Find nearest medbay location, right now just checks channel
	public static MapLocation findNearestMedBay() throws GameActionException {
		//return rc.senseHQLocation(); //TODO: Change to real code
		return indexToLocation(mRadio.readChannel(MEDBAY_LOCATION_CHAN));
	}
	
	
	public static void addWayPoint(MapLocation ml) {
		wayPoints.add(ml);
	}
	public static void clearWayPoints() {
		wayPoints.clear();
	}
	//Updates the way points, goes to next rally point if reached current rally point
	public static void updateWayPoints() throws GameActionException {
		
		if ( wayPoints.size() > 0 && mRC.getLocation().distanceSquaredTo(wayPoints.get(0)) < SOLDIER_RALLY_RAD) {
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
			clearWayPoints();
			
			for ( int i = 0; i < numWayPoints; i++ ) {
				addWayPoint(indexToLocation(mRadio.readChannel(wayPointStartChan + i )));				
			}			
		}		
	}	
	
}
