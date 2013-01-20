package BaseBot.Robots;

import java.util.ArrayList;
import static BaseBot.Util.NonConstants.*;
import static BaseBot.Util.Constants.*;







import BaseBot.Robots.Types.*;
import BaseBot.Util.RadioChannels;
import battlecode.common.*;
import static BaseBot.Util.Util.*;

public class SoldierRobot extends ARobot{
	//if you change number of SoldierTypes that are censused, make sure to update the constant
	public enum SoldierType {
		OCCUPY_ENCAMPMENT,
		LAY_MINES,
		SCOUT,
		ARMY
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
		ATTACK_HQ,
	}
	
	
	public static MapLocation curDest;
	public static MapLocation enemyHQLoc;
	public static MapLocation HQLoc;
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
	public static int mCensusRespondChannel = -1;
	
	private static MapLocation mBattleRally;
	public static int[][] THREE_AWAY_BITS= new int[7][7];
	
	public static boolean isMedbay = false;
	
	
	
	public static SoldierState getState() 
	{
		return mState;
	}

	public static SoldierState getLastState() {
		return mLastState;
	}
	
	public static MapLocation getDest()
	{
		return curDest;
	}
	
	//TODO: Fix this
	public static MapLocation getBattleRally() throws GameActionException 
	{
		return indexToLocation(mRadio.readChannel(RadioChannels.ENEMY_AVG_POS));
	}
	
	public static MapLocation getEnemyPos() throws GameActionException
	{
		return indexToLocation(mRadio.readChannel(RadioChannels.ENEMY_LOCATION));
	}
	
	public SoldierRobot(RobotController rc) {
		super(rc);
		mRC = rc;
		HQLoc = rc.senseHQLocation();
		enemyHQLoc = rc.senseEnemyHQLocation();
		wayPoints = new ArrayList<MapLocation>();
		THREE_AWAY_BITS[0][0] = Integer.parseInt("00000001",2);
		THREE_AWAY_BITS[1][0] = Integer.parseInt("10000001",2);
		THREE_AWAY_BITS[2][0] = Integer.parseInt("11000001",2);
		THREE_AWAY_BITS[3][0] = Integer.parseInt("11000001",2);
		THREE_AWAY_BITS[4][0] = Integer.parseInt("11000001",2);
		THREE_AWAY_BITS[5][0] = Integer.parseInt("11000000",2);
		THREE_AWAY_BITS[6][0] = Integer.parseInt("01000000",2);
		THREE_AWAY_BITS[6][1] = Integer.parseInt("01100000",2);
		THREE_AWAY_BITS[6][2] = Integer.parseInt("01110000",2);
		THREE_AWAY_BITS[6][3] = Integer.parseInt("01110000",2);
		THREE_AWAY_BITS[6][4] = Integer.parseInt("01110000",2);
		THREE_AWAY_BITS[6][5] = Integer.parseInt("00110000",2);
		THREE_AWAY_BITS[6][6] = Integer.parseInt("00010000",2);
		THREE_AWAY_BITS[5][6] = Integer.parseInt("00011000",2);
		THREE_AWAY_BITS[4][6] = Integer.parseInt("00011100",2);
		THREE_AWAY_BITS[3][6] = Integer.parseInt("00011100",2);
		THREE_AWAY_BITS[2][6] = Integer.parseInt("00011100",2);
		THREE_AWAY_BITS[1][6] = Integer.parseInt("00001100",2);
		THREE_AWAY_BITS[0][6] = Integer.parseInt("00000100",2);
		THREE_AWAY_BITS[0][5] = Integer.parseInt("00000110",2);
		THREE_AWAY_BITS[0][4] = Integer.parseInt("00000111",2);
		THREE_AWAY_BITS[0][3] = Integer.parseInt("00000111",2);
		THREE_AWAY_BITS[0][2] = Integer.parseInt("00000111",2);
		THREE_AWAY_BITS[0][1] = Integer.parseInt("00000011",2);
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
			HQRobot.readTypeAndState();
			setNumberOfEncampments();
			setNumberOfMidGameEnc();
			setNumberOfPreFusionEnc();
			mType = SoldierType.values()[mRadio.readChannel(RadioChannels.NEXT_SOLDIER_TYPE)];
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
			mRadio.writeChannel(RadioChannels.NEW_UNIT_ID,
					mType.ordinal() + mRC.getRobot().getID() * SoldierType.values().length);
		}
		
		performCensus();
		updateWayPoints(); 
		mDidAction = true;
		SoldierState lastState = mState;
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
		mLastState = lastState;

		while(!mDidAction) {
			mDidAction = true;
			lastState = mState;
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
			mLastState = lastState;
		}
		

	} 
	public static void switchState(SoldierState state) {
		mState = state;
		mDidAction = false;
		mRC.setIndicatorString(1, mState.toString());
	}
	public static void switchType(SoldierType type) {
		mType = type; 		
		mDidAction = false;
		mRC.setIndicatorString(0, mType.toString());
	}
	
	public static void performCensus() throws GameActionException {
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0) {
			int count = SoldierRobot.mRadio.readChannel(RadioChannels.CENSUS_START + mType.ordinal());
			mIDOrderPos = count;
			SoldierRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + mType.ordinal(), count + 1);
		}
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 1) {
			mNumArmyID = SoldierRobot.mRadio.readChannel(RadioChannels.CENSUS_START + mType.ordinal());			
		}
	}
	public static MapLocation findRallyPoint() throws GameActionException {
		return findRallyPoint(true);		
	}
	public static MapLocation findRallyPoint(boolean stayInFormation) throws GameActionException {
		// TODO Auto-generated method stub
		mRC.setIndicatorString(2, "");
		
		// isLastRally = false;
		
		if ( wayPoints.size() > 0 ) {
			//return wayPoints.get(0);
			MapLocation point = findNextWaypoint(wayPoints.toArray(new MapLocation[0]));
			
			
			if (stayInFormation) {
				float factor = 1;
				if(!point.equals(wayPoints.get(wayPoints.size()-1))) {
					factor = 0.25f;
				}
				
				point = adjustPointIntoFormation(point, factor);
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
			
		else {
			// isLastRally = true;
			return mRC.senseHQLocation();
		}
	}
	
	public static MapLocation adjustPointIntoFormation(MapLocation point, float factor) throws GameActionException {
		MapLocation enemyPosition = getEnemyPos();
		float diffX = point.x - enemyPosition.x;
		float diffY = point.y - enemyPosition.y;
		float length = (float) Math.sqrt(diffX*diffX + diffY*diffY);

		float diffXNormal;
		float diffYNormal;
		if ( length == 0) {
			diffXNormal = 0;
			diffYNormal = 0;
		}
		else {
			diffXNormal = diffX/length;
			diffYNormal = diffY/length;
		}

		//Extra check for jamming
		if ( mNumArmyID ==0 ) {
			mNumArmyID = 1;
		}
		float spreadAmountPara = -1*((EXP_PARALLEL_SPREAD*((float)mIDOrderPos/(float)mNumArmyID) - EXP_PARALLEL_SPREAD/2));
		float spreadAmountPerp = (float) (((mIDOrderPos%(Math.ceil(mNumArmyID/HORZ_PERP_SPREAD_EXP_PARA)))
				- mNumArmyID/(HORZ_PERP_SPREAD_EXP_PARA*2))*HORZ_PERP_SPREAD_MULTIPLIER);

		spreadAmountPara *= factor;
		spreadAmountPerp *= factor;

		point = point.add((int)(diffXNormal*spreadAmountPara), (int)(diffYNormal*spreadAmountPara));
		point = point.add((int)(-1*diffYNormal*spreadAmountPerp),(int)(diffXNormal*spreadAmountPerp));

		return point;
	}
	
	//Find nearest medbay location, right now just checks channel
	public static MapLocation findNearestMedBay() throws GameActionException {
		//return rc.senseHQLocation(); //TODO: Change to real code
		return indexToLocation(mRadio.readChannel(RadioChannels.MEDBAY_LOCATION));
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
		
		int wayPointChanData = mRadio.readChannel(RadioChannels.SOLDIER_WAYPOINT_RALLY);
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
