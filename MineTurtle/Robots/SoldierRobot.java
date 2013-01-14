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
		CAPTURING_ENCAMPMENT,

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
	protected static SoldierState mLastState;
	protected static SoldierType mType;
	
	public static int mIDOrderPos = 0;
	public static int mNumArmyID = 0;
	
	public static int mClaimedEncampmentChannel = -1;
	
	private static boolean mDidAction = false;
	public static int numTurnsCapturing = 0;
	
	
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
	
	public static MapLocation getEnemyPos() throws GameActionException
	{
		return indexToLocation(mRadio.readChannel(ENEMY_LOCATION_CHAN));
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
			//int currentBotNumber = mRadio.readChannel(CURRENT_BOT_ID_CHAN);
			//mRadio.writeChannel(CURRENT_BOT_ID_CHAN, currentBotNumber+1);
			//mRadio.writeChannel(LAST_FOUR_BOT_ID_RAD_CHAN_START + CURRENT_BOT_ID_CHAN % NUM_ROBOTS_TO_CHECK_ID, mRC.getRobot().getID());
			setNumberOfEncampments();
			setNumberOfPreFusionEnc();
			mType = SoldierType.values()[mRadio.readChannel(NEXT_SOLDIER_TYPE_CHAN)];
			switch(mType) {
			case OCCUPY_ENCAMPMENT:
				mState = SoldierState.FIND_ENCAMPMENT;
				break;
			case LAY_MINES:
				mState = SoldierState.MINE;
				break;
			case SCOUT:
				mState = SoldierState.COMPUTE_SCOUT_PATH;
				break;
			default:
				mType = SoldierType.ARMY;
				mState = SoldierState.GOTO_RALLY;
			}
			mRC.setIndicatorString(0, mType.toString());
			mRC.setIndicatorString(1, mState.toString());
		}
		
		preformCensus();
		updateWayPoints(); 
		mDidAction = true;
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

		while(!mDidAction) {
			mDidAction = true;
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

	}
	public static void switchState(SoldierState state) {
		mLastState = mState;
		mState = state;
		mDidAction = false;
		mRC.setIndicatorString(1, mState.toString());
	}
	public static void switchType(SoldierType type) {
		mType = type; 		
		mDidAction = false;
		mRC.setIndicatorString(0, mType.toString());
	}
	
	public static void preformCensus() throws GameActionException {
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0) {
			int count = SoldierRobot.mRadio.readChannel(CENSUS_RAD_CHAN_START + mType.ordinal());
			mIDOrderPos = count;
			SoldierRobot.mRadio.writeChannel(CENSUS_RAD_CHAN_START + mType.ordinal(), count + 1);
		}
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 1) {
			mNumArmyID = SoldierRobot.mRadio.readChannel(CENSUS_RAD_CHAN_START + mType.ordinal());			
		}
	}
	public static MapLocation findRallyPoint() throws GameActionException {
		return findRallyPoint(false);		
	}
	public static MapLocation findRallyPoint(boolean stayInFormation) throws GameActionException {
		// TODO Auto-generated method stub
		mRC.setIndicatorString(2, "");
		if ( wayPoints.size() > 0 ) {
			//return wayPoints.get(0);
			MapLocation point = findNextWaypoint(wayPoints.toArray(new MapLocation[0]));
			
			
			if (stayInFormation) {
				//Add for parallel to direction to enemy spread
				point = point.add(point.directionTo(getEnemyPos()),
						(int)(-1*(EXP_PARALLEL_SPREAD*((float)mIDOrderPos/(float)mNumArmyID) - EXP_PARALLEL_SPREAD/2)));
				
				//Add for perpendicular to direction to enemy spread
				point = point.add(Direction.values()[(point.directionTo(getEnemyPos()).ordinal()+ 2)%NUM_DIR],
						(int) (((mIDOrderPos%(Math.ceil(mNumArmyID/HORZ_PERP_SPREAD_EXP_PARA))) - mNumArmyID/(HORZ_PERP_SPREAD_EXP_PARA*2))*HORZ_PERP_SPREAD_MULTIPLIER));				

			}
			else {				
				/*
				if(mRC.getLocation().distanceSquaredTo(point) < RALLY_RAD_SQUARED) {	
					mRC.setIndicatorString(2, "getEnemyPos");
					return getEnemyPos();
				}
				*/				
			}
			mRC.setIndicatorString(2, point.toString());
			
			return point;
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
	public static int getNumWayPoints() {
		return wayPoints.size();
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
