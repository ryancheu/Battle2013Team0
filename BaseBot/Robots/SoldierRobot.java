package BaseBot.Robots;

import java.util.ArrayList;




import BaseBot.Robots.Types.*;
import BaseBot.Util.Constants;
import BaseBot.Util.RadioChannels;
import BaseBot.Util.Constants.MineStatus;
import battlecode.common.*;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.NonConstants.*;
import static BaseBot.Util.Util.*;

public class SoldierRobot extends ARobot{
	//if you change number of SoldierTypes that are censused, make sure to update the constant
	public enum SoldierType {
		OCCUPY_ENCAMPMENT,
		LAY_MINES,
		SCOUT,
		ARMY,
		OLDSCHOOLARMY,
		ARMYPOINT,
		SUICIDE,
		PROTECT_LEFT_ENCAMPMENT,
		PROTECT_RIGHT_ENCAMPMENT
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
		GOTO_SHIELD,
		ATTACK_HQ,
		RETREAT,
	}
	
	
	public static MapLocation curDest;
	public static MapLocation enemyHQLoc;
	public static MapLocation HQLoc;
	public static ArrayList<MapLocation> wayPoints;

	protected static SoldierState mState;
	protected static SoldierState mLastState;
	protected static SoldierType mType;
	
	public static int lastWaypointBeforeShield = -1;
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
	
	public static boolean enemyNukingFast = false;
	public static int enemyMineRadius = 0;
	public static MapLocation lastDefusion = null;
	private static MapLocation lastRallyPoint = null;
	
	//use this to determine where, if anywhere, we should broadcast the location of an encampment in progress.
	public static int numEncampmentsBuilding;
	public static int mLastAttackTurn = -1;
	
	public static double mLastTurnEnergon = 40;
	public static int mLastTurnPotentialDamage = 40;
	
	public static boolean enemyHasArtillery = false;
	public static boolean shouldTurnIntoEncampment = false;
	public static boolean shouldBeSearchShield = false;
	
	public static boolean isSmallMap = false;
	
	private static int lastCheckedEncampment;
	
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
		
		int diffX = Math.abs(mRC.getLocation().x - SoldierRobot.enemyHQLoc.x);
		int diffY = Math.abs(mRC.getLocation().y - SoldierRobot.enemyHQLoc.y);
		isSmallMap = Math.max(diffX, diffY) < SMALL_MAP_DIST;
		
		//TODO: make these real consts
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
		THREE_AWAY_BITS[1][1] = Integer.parseInt("10000010",2);
		THREE_AWAY_BITS[1][2] = Integer.parseInt("10001100",2);
		THREE_AWAY_BITS[1][3] = Integer.parseInt("10001000",2);
		THREE_AWAY_BITS[1][4] = Integer.parseInt("10001001",2);
		THREE_AWAY_BITS[1][5] = Integer.parseInt("00001010",2);
		THREE_AWAY_BITS[2][5] = Integer.parseInt("00110010",2);
		THREE_AWAY_BITS[3][5] = Integer.parseInt("00100010",2);
		THREE_AWAY_BITS[4][5] = Integer.parseInt("00100100",2);
		THREE_AWAY_BITS[5][5] = Integer.parseInt("00101000",2);
		THREE_AWAY_BITS[5][4] = Integer.parseInt("11001000",2);
		THREE_AWAY_BITS[5][3] = Integer.parseInt("10001000",2);
		THREE_AWAY_BITS[5][2] = Integer.parseInt("10011000",2);
		THREE_AWAY_BITS[5][1] = Integer.parseInt("10100000",2);
		THREE_AWAY_BITS[4][1] = Integer.parseInt("00100001",2);
		THREE_AWAY_BITS[3][1] = Integer.parseInt("00100010",2);
		THREE_AWAY_BITS[2][1] = Integer.parseInt("01100010",2);
		
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
			case ARMYPOINT:
				mState = SoldierState.GOTO_RALLY;
				break;
			case PROTECT_LEFT_ENCAMPMENT:
				mState = SoldierState.GOTO_RALLY;
				break;
			case PROTECT_RIGHT_ENCAMPMENT:
				mState = SoldierState.GOTO_RALLY;
				break;
			case SUICIDE:
				mState = SoldierState.COMPUTE_SCOUT_PATH;
				break;
			default:
				if(HQRobot.mType==HQRobot.HQType.RUSH )
				{
					mType = SoldierType.OLDSCHOOLARMY;
					mState = SoldierState.GOTO_RALLY;
				}
				else
				{
					mType = SoldierType.ARMY;
					mState = SoldierState.GOTO_RALLY;
				}
				if(SoldierRobot.mRadio.readChannel(RadioChannels.SHIELD_LOCATION) > 0)
					mState = SoldierState.GOTO_SHIELD;
				break;
			}
			mRC.setIndicatorString(0, mType.toString());
			mRC.setIndicatorString(1, mState.toString());
			mRadio.writeChannel(RadioChannels.NEW_UNIT_ID,
					mType.ordinal() + mRC.getRobot().getID() * SoldierType.values().length);
		}
		
		performCensus();
		updateWayPoints();
		updateMineStatus();
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
			case OLDSCHOOLARMY:
				SoldierArmyTypeOldSchool.run();
				break;
			case ARMYPOINT:
				SoldierArmyType.run();
				break;
			case PROTECT_LEFT_ENCAMPMENT:
				SoldierProtectLeftEncampmentType.run();
				break;
			case PROTECT_RIGHT_ENCAMPMENT:
				SoldierProtectRightEncampmentType.run();
				break;
			case SUICIDE:
				SoldierSuicideScoutType.run();
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
				case OLDSCHOOLARMY:
					SoldierArmyTypeOldSchool.run();
					break;
				case ARMYPOINT:
					SoldierArmyType.run();
					break;
				case PROTECT_LEFT_ENCAMPMENT:
					SoldierProtectLeftEncampmentType.run();
					break;
				case PROTECT_RIGHT_ENCAMPMENT:
					SoldierProtectRightEncampmentType.run();
					break;
				case SUICIDE:
					SoldierSuicideScoutType.run();
					break;
				default:
					// TODO: raise error
					break;
			}
			mLastState = lastState;
		}
		
		if ( !SoldierRobot.enemyHasArtillery ) { 
			if (mLastTurnEnergon - mRC.getEnergon() > mLastTurnPotentialDamage && !(mRC.senseMine(mRC.getLocation()) == mEnemy)) {
				mRadio.writeChannel(RadioChannels.ENEMY_HAS_ARTILLERY_NORMAL, 1);
				SoldierRobot.enemyHasArtillery = true;
//				print("artillery Found");
			}
			else if (Clock.getRoundNum() % CENSUS_INTERVAL == 1 && mRadio.readChannel(RadioChannels.ENEMY_HAS_ARTILLERY_NORMAL) == 1 ) {
				SoldierRobot.enemyHasArtillery = true;
			}			
		}
		else {
			mRC.setIndicatorString(0, "potential damage: " + mLastTurnPotentialDamage);
		}
		
	
			mLastTurnEnergon = mRC.getEnergon();
	
		

	} 
	
	private static void updateMineStatus() throws GameActionException {
		if(mRC.isActive() && lastDefusion != null) {
			setMineStatus(lastDefusion, MineStatus.DEFUSED);
			lastDefusion = null;
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
			mNumArmyID = SoldierRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.ARMY.ordinal());
			//TODO: Maybe make this have the ability to switch back?
			if ( !enemyNukingFast && SoldierRobot.mRadio.readChannel(RadioChannels.ENEMY_FASTER_NUKE) == 1) {
				enemyNukingFast = true;
			}
		}
		int enemies =mRadio.readChannel(RadioChannels.HQ_IN_DANGER);
		
		if((enemies & FIRST_BYTE_KEY)==FIRST_BYTE_KEY && mType ==SoldierType.ARMY && (enemies^FIRST_BYTE_KEY)>1)
		{
			mState = SoldierState.RETREAT;
			mRadio.writeChannel(RadioChannels.HQ_IN_DANGER, enemies-1);
		}
	}
	public static MapLocation findRallyPoint() throws GameActionException {
		return findRallyPoint(true);		
		
	}
	public static MapLocation findRallyPoint(int scoutType) throws GameActionException {
		return findRallyPoint(true, scoutType);		
		
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

			lastRallyPoint = point;
			return point;
		}
			
		else {
			// isLastRally = true;
			if(lastRallyPoint != null) {
				return lastRallyPoint;
			}
			else {
				return new MapLocation(
						(6*mRC.senseHQLocation().x + mRC.senseEnemyHQLocation().x)/7,
						(6*mRC.senseHQLocation().y + mRC.senseEnemyHQLocation().y)/7);
			}
		}
	}

	public static MapLocation findRallyPoint(boolean stayInFormation, int scoutType) throws GameActionException {
		// TODO Auto-generated method stub
		mRC.setIndicatorString(2, "");
		
		// isLastRally = false;
		
		if ( wayPoints.size() > 0 ) {
			//return wayPoints.get(0);
			MapLocation point = findNextWaypoint(wayPoints.toArray(new MapLocation[0]));
			
			
			if (stayInFormation) {
				float factor = 1;
				
				point = adjustPointIntoFormation(point, factor, scoutType);
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
			
			lastRallyPoint = point;
			return point;
		}
			
		else {
			// isLastRally = true;
			if(lastRallyPoint != null) {
				return lastRallyPoint;
			}
			else {
				return new MapLocation(
						(6*mRC.senseHQLocation().x + mRC.senseEnemyHQLocation().x)/7,
						(6*mRC.senseHQLocation().y + mRC.senseEnemyHQLocation().y)/7);
			}
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
	public static MapLocation adjustPointIntoFormation(MapLocation point, float factor,int type) throws GameActionException {
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
		//ALRIGHT.
		//What we're doing here is making a couple of point men.
		
		//Here, we only move parallel to the line towards the enemy's base. This is to put up our early warning guard.
		
		if(type==0)
		{
			float spreadAmountPara = -1*((EXP_PARALLEL_SPREAD*((float)mIDOrderPos/(float)mNumArmyID) - EXP_PARALLEL_SPREAD/2));
			float spreadAmountPerp = (float) (((mIDOrderPos%(Math.ceil(mNumArmyID/HORZ_PERP_SPREAD_EXP_PARA)))
					- mNumArmyID/(HORZ_PERP_SPREAD_EXP_PARA*2))*HORZ_PERP_SPREAD_MULTIPLIER);

			spreadAmountPara *= factor;
			spreadAmountPerp *= factor;

			point = point.add((int)(diffXNormal*spreadAmountPara), (int)(diffYNormal*spreadAmountPara));
			point = point.add((int)(-1*diffYNormal*spreadAmountPerp),(int)(diffXNormal*spreadAmountPerp));

		}
		else if(type==1)
		{
			
			float spreadAmountPara = 1*((EXP_PARALLEL_SPREAD*((float)mIDOrderPos/(float)mNumArmyID) - EXP_PARALLEL_SPREAD/2));
			
			//I need an increasing factor to make them actually move far enough
			spreadAmountPara *= POINT_FORWARD_FACTOR*factor;
			
			point = point.add((int)(diffXNormal*spreadAmountPara), (int)(diffYNormal*spreadAmountPara));
		}
		//These two move to either side of our big wall, in case someone tries to go by us.
		else if(type==2)
		{
			float spreadAmountPerp = (float) (((mNumArmyID%(Math.ceil(mNumArmyID/HORZ_PERP_SPREAD_EXP_PARA)))
					- mNumArmyID/(HORZ_PERP_SPREAD_EXP_PARA*2))*HORZ_PERP_SPREAD_MULTIPLIER);
			//I need an increasing factor to make them actually move far enough
			spreadAmountPerp *= -POINT_SIDEWAYS_FACTOR*factor;
			//make sure they never switch sides (was having this problem earlier)
			if(spreadAmountPerp>0)
			{
				spreadAmountPerp*=-1;
			}
			point = point.add((int)(-1*diffYNormal*spreadAmountPerp),(int)(diffXNormal*spreadAmountPerp));
		}
		else
		{
			float  spreadAmountPerp = (float) (((mNumArmyID%(Math.ceil(mNumArmyID/HORZ_PERP_SPREAD_EXP_PARA)))
					- mNumArmyID/(HORZ_PERP_SPREAD_EXP_PARA*2))*HORZ_PERP_SPREAD_MULTIPLIER);
			//I need an increasing factor to make them actually move far enough
			spreadAmountPerp *= POINT_SIDEWAYS_FACTOR*factor;
			//make sure they never switch sides (was having this problem earlier)
			if(spreadAmountPerp<0)
			{
				spreadAmountPerp*=-1;
			}
			point = point.add((int)(-1*diffYNormal*spreadAmountPerp),(int)(diffXNormal*spreadAmountPerp));
		}


		return point;
	}
	public static MapLocation findEncampmentRallyPoint(boolean stayInFormation, int scoutType) throws GameActionException {
		// TODO Auto-generated method stub
		mRC.setIndicatorString(2, "");
		
		// isLastRally = false;
		
		if ( wayPoints.size() > 0 ) {
			//return wayPoints.get(0);
			MapLocation point = findNextWaypoint(wayPoints.toArray(new MapLocation[0]));
			
			
			if (stayInFormation) {
				point = findEncampmentToDefend(scoutType);
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
			
			lastRallyPoint = point;
			return point;
		}
			
		else {
			// isLastRally = true;
			if(lastRallyPoint != null) {
				return lastRallyPoint;
			}
			else {
				return new MapLocation(
						(6*mRC.senseHQLocation().x + mRC.senseEnemyHQLocation().x)/7,
						(6*mRC.senseHQLocation().y + mRC.senseEnemyHQLocation().y)/7);
			}
		}
	}
	
	
	public static MapLocation findEncampmentToDefend(int scoutType) throws GameActionException {
		//loop through all owned encampments, find the one that is the farthest away from both HQs
		MapLocation[] alliedEncampments = mRC.senseAlliedEncampmentSquares();
		MapLocation farthestEncampment = null;
		MapLocation closestToEnemyHQEnc = null;
		int largestDistance = 0;
		int smallestDistanceToEnemyHQ = MAX_DIST_SQUARED;
		int startByteCode = Clock.getBytecodesLeft(); 
		int encampmentIndex;
		if(lastCheckedEncampment < alliedEncampments.length){
			encampmentIndex = lastCheckedEncampment; 
		}
		else{
			encampmentIndex = 0;
		}
		if(scoutType == 0){
			//should be leftScout
			for(; encampmentIndex < alliedEncampments.length; encampmentIndex++){
				MapLocation HQ = SoldierRobot.HQLoc;
				MapLocation EnemyHQ = SoldierRobot.enemyHQLoc;
				MapLocation Enc = alliedEncampments[encampmentIndex];
				int distanceToEnemyHQ = Enc.distanceSquaredTo(EnemyHQ);
				boolean isLeft = ((EnemyHQ.x - HQ.x)*(Enc.y - HQ.y) - (EnemyHQ.y - HQ.y)*(Enc.x - HQ.x)) > 0;
				//this long arithmetic is for finding how far from the direct a given Enc is
				
				int num = Math.abs((EnemyHQ.x - HQ.x)*(HQ.y - Enc.y) 
						- (HQ.x - Enc.x)*(EnemyHQ.y-HQ.y));
				double denom = Math.sqrt((double)Math.pow((EnemyHQ.x-HQ.x),2.0)
						+Math.pow((EnemyHQ.y - HQ.y),2.0));
				int distanceSquaredFromDirect = (int)Math.pow((num / denom),2);

				if(distanceSquaredFromDirect > largestDistance && isLeft){
					farthestEncampment = Enc;
					largestDistance = distanceSquaredFromDirect;
				}
				if(distanceToEnemyHQ < smallestDistanceToEnemyHQ && isLeft && distanceSquaredFromDirect > 30){
					closestToEnemyHQEnc = Enc;
					smallestDistanceToEnemyHQ = distanceToEnemyHQ;
				}
				if ( startByteCode - Clock.getBytecodesLeft() > MAX_BYTE_CODE_FOR_ENCAMPMENT_CHECK)
	        	{
	        		lastCheckedEncampment = encampmentIndex;
	        		//return rough draft
	        		if(closestToEnemyHQEnc == null){
	        			if(farthestEncampment == null){
	        				return findRallyPoint();
	        			}
	        			return farthestEncampment;
	        		}
	        		else{
	        			return new MapLocation((farthestEncampment.x + closestToEnemyHQEnc.x)/2,(farthestEncampment.y + closestToEnemyHQEnc.y)/2);
	        		}
	        	}
			}
		}
		else{
			//should be right scout
			for(; encampmentIndex < alliedEncampments.length; encampmentIndex++){
				MapLocation HQ = SoldierRobot.HQLoc;
				MapLocation EnemyHQ = SoldierRobot.enemyHQLoc;
				MapLocation Enc = alliedEncampments[encampmentIndex];	
				int distanceToEnemyHQ = Enc.distanceSquaredTo(EnemyHQ);
				boolean isRight = ((EnemyHQ.x - HQ.x)*(Enc.y - HQ.y) - (EnemyHQ.y - HQ.y)*(Enc.x - HQ.x)) < 0;
				//this long arithmetic is for finding how far from the direct a given Enc is

				int num = Math.abs((EnemyHQ.x - HQ.x)*(HQ.y - Enc.y) 
						- (HQ.x - Enc.x)*(EnemyHQ.y-HQ.y));
				double denom = Math.sqrt((double)Math.pow((EnemyHQ.x-HQ.x),2.0)
						+Math.pow((EnemyHQ.y - HQ.y),2.0));
				int distanceSquaredFromDirect = (int)Math.pow((num / denom),2);

				if(distanceSquaredFromDirect > largestDistance && isRight){
					farthestEncampment = alliedEncampments[encampmentIndex];
					largestDistance = distanceSquaredFromDirect;
				}
				if(distanceToEnemyHQ < smallestDistanceToEnemyHQ && isRight && distanceSquaredFromDirect > 30){
					closestToEnemyHQEnc = Enc;
					smallestDistanceToEnemyHQ = distanceToEnemyHQ;
				}
				if ( startByteCode - Clock.getBytecodesLeft() > MAX_BYTE_CODE_FOR_ENCAMPMENT_CHECK)
	        	{
	        		lastCheckedEncampment = encampmentIndex;
	        		//return rough draft
	        		if(closestToEnemyHQEnc == null){
	        			if(farthestEncampment == null){
	        				return findRallyPoint();
	        			}
	        			return farthestEncampment;
	        		}
	        		else{
	        			return new MapLocation((farthestEncampment.x + closestToEnemyHQEnc.x)/2,(farthestEncampment.y + closestToEnemyHQEnc.y)/2);
	        		}
	        	}
			}
		}
		lastCheckedEncampment = alliedEncampments.length;
		//null checks
		if(closestToEnemyHQEnc == null){
			if(farthestEncampment == null){
				return findRallyPoint();
			}
			return farthestEncampment;
		}
		else{
			return new MapLocation((farthestEncampment.x + closestToEnemyHQEnc.x)/2,(farthestEncampment.y + closestToEnemyHQEnc.y)/2);
		}
		
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
		
		int wayPointChanData =  mRadio.readChannel(RadioChannels.SOLDIER_WAYPOINT_RALLY);
	
		int lastUpdated = wayPointChanData & BIT_MASKS[WAYPOINT_ROUND_BITS];
		if ( mLastRecvWayPoint < lastUpdated ) {
			int wayPointStartChan = (wayPointChanData >> WAYPOINT_ROUND_BITS) & BIT_MASKS[WAYPOINT_START_CHAN_BITS];
			int numWayPoints = (wayPointChanData >> (WAYPOINT_ROUND_BITS+WAYPOINT_START_CHAN_BITS)) & BIT_MASKS[WAYPOINT_NUM_RALLY_BITS];
			clearWayPoints();
			
			for ( int i = 0; i < numWayPoints; i++ ) {
				int tempSignal = mRadio.readChannel(wayPointStartChan + i );
				if((tempSignal & FIRST_BYTE_KEY_MASK)==FIRST_BYTE_KEY)
				{
					addWayPoint(indexToLocation(tempSignal ^ FIRST_BYTE_KEY));
				}
			}			
		}
		
	}	
	
}
