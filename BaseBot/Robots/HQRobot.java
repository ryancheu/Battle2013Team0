package BaseBot.Robots;

import BaseBot.Robots.Types.HQFasterNukeType;
import BaseBot.Robots.Types.HQNormalType;
import BaseBot.Robots.Types.HQNukeType;
import BaseBot.Robots.Types.HQRushType;
import BaseBot.Util.RadioChannels;
import battlecode.common.*;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.Util.*;

public class HQRobot extends ARobot{
	
	//Maybe remove? kept in for organization
	public enum HQType  {
		RUSH,
		ECON,
		NUKE,
		FASTER_NUKE
	}
	public enum HQState { 		
		TURTLE,
		PREPARE_ATTACK,
		ATTACK,
		RUSH,
	}
	
	protected static HQState mState, mLastState;
	protected static HQType mType;
	public static MapLocation enemyHQLoc;
	public static MapLocation mLocation;
	protected static Direction enHQDir;
	
	public static MapLocation enemyLastSeenPosAvg;
	
	public static MapLocation[] encampmentPositions;
	
	public static boolean enemyNukeSoon = false;
	public static boolean enemyNukeSoonNoReally = false;
	
	public static int lastBuiltWasEncampment = -1;
	
	public static int maxEncChannel = 0;
	
	public static int lastCheckedChannel = RadioChannels.ENC_CLAIM_START; //used for encampment checking logic between runs
	
	private static int turnOfNuke = -1;
	
	public HQRobot(RobotController rc) {
		super(rc);
		enemyHQLoc = rc.senseEnemyHQLocation();
		enHQDir = rc.getLocation().directionTo(enemyHQLoc);
		mLocation = rc.getLocation();
	}
	
	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		mainHQLogic();
	}
	
	public static void chooseType() throws GameActionException{
		//Ideally this will decide based on RUSHDISTANCE, num of neutral mines, team memory
		//TODO: add dependance on what size map the previous one was, for instance, if it was a big map and we lost with nuke def dont do nuke
		//analyze maps based on where encampments are
		//if encampments are in between you and the enemy and the map is small, you want safe pickaxe nuke
		//no longer use goodForPickaxe because pickaxe is bad 
		//boolean goodForPickaxeNuke = isMapGoodForPickaxeNuke();
		long roundNum = mRC.getTeamMemory()[ROUND_NUM_MEMORY];
		long howEnded = mRC.getTeamMemory()[HOW_ENDED_MEMORY];
		boolean nukeFasterThanOurFastestNuke = (howEnded == ENEMY_NUKED && roundNum < 100);
		long howWePlayed = mRC.getTeamMemory()[HOW_WE_PLAYED_MEMORY];
		int directRushDistanceSquared = HQRobot.enemyHQLoc.distanceSquaredTo(mRC.getLocation());
		//this is removed because we don't want pickaxe nuke unless we get beaten by it
		/*
		if(!nukeFasterThanOurFastestNuke && goodForPickaxeNuke && (roundNum != 0 || howEnded != 0 || howWePlayed != 0)){
			
			//this is very similar to the main memory strategy checking block
			//but it only uses Nuke and FasterNuke
			
			//we have team memory from last game
			//all you have to do is decide between fast pickaxe nuke and safe pickaxe nuke
			if(howEnded == ENEMY_NUKED && howWePlayed != FASTER_NUKE_TYPE){
				//this should be our ideal counter to nuke, right now, that's nuke :((
				mType = HQType.FASTER_NUKE;
				mState = HQState.TURTLE;
			}
			else if(howEnded == WE_NUKED && howWePlayed == FASTER_NUKE_TYPE){
				mType = HQType.FASTER_NUKE;
				mState = HQState.TURTLE;
			}
			else{
				mType = HQType.NUKE;
				mState = HQState.TURTLE;
			}
		}
		else if(!nukeFasterThanOurFastestNuke && goodForPickaxeNuke){
			//we have no team memory to work with
			mType = HQType.NUKE;
			mState = HQState.TURTLE;
		}
		*/
		if(roundNum != 0 || howEnded != 0 || howWePlayed != 0){
			//we have team memory to work with
			if (howEnded == ENEMY_ECON && directRushDistanceSquared < 1500 ) {
				
				mType = HQType.RUSH;
				mState = HQState.TURTLE;
			}
			else if(howEnded == ENEMY_NUKED && nukeFasterThanOurFastestNuke && directRushDistanceSquared < 3000){
				//their nuke is faster than our fast nuke...they must be hacking. Rush
				mType = HQType.RUSH;
				mState = HQState.TURTLE;
			}
			else if(howEnded == TIEBREAKERS && directRushDistanceSquared > 2000){
				mType = HQType.ECON;
				mState = HQState.TURTLE;
			}
			/*
			else if(howEnded == WE_NUKED && howWePlayed != FASTER_NUKE_TYPE && directRushDistanceSquared > 3000){
				mType = HQType.NUKE;
				mState = HQState.TURTLE;
			}
			*/
			else if(!nukeFasterThanOurFastestNuke && howEnded == ENEMY_NUKED && howWePlayed != FASTER_NUKE_TYPE && directRushDistanceSquared > 1500){
				//this should be our ideal counter to nuke, right now, that's nuke :((
				mType = HQType.FASTER_NUKE;
				mState = HQState.TURTLE;
			}
			else if(howEnded == WE_NUKED && howWePlayed == FASTER_NUKE_TYPE && directRushDistanceSquared > 1500){
				//if we faster nuked successfully last time and the map isn't tiny then faster nuke
				mType = HQType.FASTER_NUKE;
				mState = HQState.TURTLE;
			}
			else {
				//if we rushed or econed for the win we end up here
				mType = HQType.ECON;
				mState = HQState.TURTLE;
			}
		}
		else{
			//no team memory and it's a bad map for picknuke
			
			mType = HQType.ECON;
			mState = HQState.TURTLE;
			
		}
		mRC.setIndicatorString(0, mType.toString());
		mRC.setIndicatorString(1, mState.toString());
	}
	private static boolean isMapGoodForPickaxeNuke() throws GameActionException{ 
		//takes like 4000bytecodes on lines
		//but only like 1000 bytecodes on other maps
		MapLocation[] allEncampments = mRC.senseEncampmentSquares(mRC.getLocation(), RobotType.ARTILLERY.attackRadiusMaxSquared,Team.NEUTRAL);
		MapLocation EnemyHQ = mRC.senseEnemyHQLocation();
		MapLocation HQ = mRC.getLocation();
		MapLocation tempLocation;
		int totalArtilleryLocations = 0;
		for (int i = 0; i < allEncampments.length; i++) {
			tempLocation = allEncampments[i];
			int num = Math.abs((EnemyHQ.x - HQ.x)*(HQ.y - tempLocation.y) 
					- (HQ.x - tempLocation.x)*(EnemyHQ.y-HQ.y));
			double denom = Math.sqrt((double)Math.pow((EnemyHQ.x-HQ.x),2.0)
					+Math.pow((EnemyHQ.y - HQ.y),2.0));
			int distanceSquaredFromDirect = (int)Math.pow((num / denom),2);
			int distanceFromHQ = HQ.distanceSquaredTo(tempLocation);
			int distanceFromEnemyHQ =  EnemyHQ.distanceSquaredTo(tempLocation);
			int distanceFromHQToHQ = HQ.distanceSquaredTo(EnemyHQ);
			if(distanceSquaredFromDirect < RobotType.ARTILLERY.attackRadiusMaxSquared
					&& distanceFromEnemyHQ <= distanceFromHQToHQ){
				totalArtilleryLocations++;
			}
		}
		int w = Math.abs(HQ.x - EnemyHQ.x);
		int h = Math.abs(HQ.y - EnemyHQ.y);
		int A = Math.max(w, h);
		return totalArtilleryLocations >= A/5;
		//there will be double the encampments that we actually want to take in between the two of us
		//if encampments are close and they are in between me and enemy it is a good map
		//in order to tell if in front of me, check if the distance from the encampment to the enemy is greater than the distance from me to the enemy
		//then check if they are close to within the direct distance to the enemy
		
	}
	private void mainHQLogic() throws GameActionException {
		if (mType == null )
		{
			chooseType();
		}
		HQState lastState = mState;
		broadcastTypeAndState();
		// write to the team memory what turn it is (or what turn nuke should be started)
		// and how we or they might die this round
		setTeamMemory();
		
		switch(mType)
		{
		case RUSH:
			HQRushType.run();
			break;
		case NUKE:
			HQNukeType.run();
			break;
		case ECON:
			HQNormalType.run();
			break;
		case FASTER_NUKE:
			HQFasterNukeType.run();
			break;
		}
		mLastState = lastState;
		
	}

	
	public static HQState getState() {
		return mState;
	}
	
	public static HQState getLastState() {
		return mLastState;
	}

	public static void setRallyPoint(MapLocation loc) throws GameActionException {
		int message = Clock.getRoundNum() 
				| (RadioChannels.HQ_ATTACK_RALLY_START << WAYPOINT_ROUND_BITS) 
				| (1 << (WAYPOINT_ROUND_BITS + WAYPOINT_START_CHAN_BITS));
		HQRobot.mRadio.writeChannel(RadioChannels.SOLDIER_WAYPOINT_RALLY,message);
		HQRobot.mRadio.writeChannel(RadioChannels.HQ_ATTACK_RALLY_START,FIRST_BYTE_KEY | locationToIndex(loc));
		// HQRobot.mRadio.writeChannel(BACKUP_RALLY_POINT_RAD_CHAN, locationToIndex(loc));
	}

	public static void setRallyPoints(MapLocation[] locs) throws GameActionException {
		setRallyPoints(locs, locs.length);
	}
	
	public static void setRallyPoints(MapLocation[] locs, int length) throws GameActionException {
		int message = Clock.getRoundNum() 
				| (RadioChannels.HQ_ATTACK_RALLY_START << WAYPOINT_ROUND_BITS) 
				| (length << (WAYPOINT_ROUND_BITS + WAYPOINT_START_CHAN_BITS)) ;
		HQRobot.mRadio.writeChannel(RadioChannels.SOLDIER_WAYPOINT_RALLY, message);
		for(int n=0; n<length; ++n) {
			HQRobot.mRadio.writeChannel(RadioChannels.HQ_ATTACK_RALLY_START + n, FIRST_BYTE_KEY |locationToIndex(locs[n]));
		}
		// HQRobot.mRadio.writeChannel(BACKUP_RALLY_POINT_RAD_CHAN, locationToIndex(locs[length-1]));
	}
	
	public static void switchState(HQState state) {
		mState = state;
		mRC.setIndicatorString(1, mState.toString());
	}
	public static void switchType(HQType type) {
		mType = type; 
		mRC.setIndicatorString(0, mType.toString());
	}
	
	private static void broadcastTypeAndState() throws GameActionException {
		//print("type is: " + mType.ordinal());
		mRadio.writeChannel(RadioChannels.HQ_TYPE, mType.ordinal());
		mRadio.writeChannel(RadioChannels.HQ_STATE, mState.ordinal());
	}
	
	public static void readTypeAndState() throws GameActionException {
		//Make sure what we're checking is actually within bounds.
		
		int hqType = mRadio.readChannel(RadioChannels.HQ_TYPE);
		int hqState = mRadio.readChannel(RadioChannels.HQ_STATE);
		if(hqType>=0 && hqType < HQType.values().length && hqState >= 0 && hqState < HQState.values().length )								
				
		{
			mType = HQType.values()[hqType];
			mState = HQState.values()[hqState];
		}
		//otherwise just dump into econ
		else
		{
			//print("bad range: " + mRadio.readChannel(RadioChannels.HQ_TYPE));
			//print("bad range: " + mRadio.readChannel(RadioChannels.HQ_STATE));
			mType = HQType.ECON;
			mState = HQState.TURTLE;
		}
		switch(mType) {
		case RUSH:
			HQRushType.setConstants();
			break;
		case NUKE:
			HQNukeType.setConstants();
			break;
		case ECON:
			HQNormalType.setConstants();
			break;
		case FASTER_NUKE:
			HQFasterNukeType.setConstants();
			break;
		}
	}
	
	public static void spawnRobot(SoldierRobot.SoldierType type) throws GameActionException {
		
		if (type == SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT )
		{
			HQRobot.maxEncChannel++;
			HQRobot.lastBuiltWasEncampment = 0;
			//print("writing to max channel: " + HQRobot.maxEncChannel);
			mRadio.writeChannel(RadioChannels.MAX_ENC_CHANNEL_TO_CHECK, HQRobot.maxEncChannel);
		}
		else {
			HQRobot.lastBuiltWasEncampment++;
		}
		Direction tempDir = null;
		for (int i = 0; i < NUM_DIR; ++i) {
			tempDir = Direction.values()[(enHQDir.ordinal() + i + NUM_DIR) % NUM_DIR];
			if (mRC.canMove(tempDir)  && mRC.senseMine(mRC.getLocation().add(tempDir)) != Team.NEUTRAL) {
				mRC.spawn(tempDir);
				break;
			}
		}
		HQRobot.mRadio.writeChannel(RadioChannels.NEXT_SOLDIER_TYPE, type.ordinal());
	}	
	
	public static void setTeamMemory() throws GameActionException {
		if(Clock.getRoundNum() < 10){
			mRC.setTeamMemory(HOW_WE_PLAYED_MEMORY, mType.ordinal());
		}
		if(mRC.senseEnemyNukeHalfDone() && turnOfNuke == -1){
			turnOfNuke = Clock.getRoundNum()-Upgrade.NUKE.numRounds/2;
		}
		mRC.setTeamMemory(ENEMY_NUKE_START_ROUND, turnOfNuke);
		if(mRC.getEnergon()<=1 && Clock.getRoundNum()>2000){
			mRC.setTeamMemory(ROUND_NUM_MEMORY,Clock.getRoundNum());
			mRC.setTeamMemory(HOW_ENDED_MEMORY, TIEBREAKERS);
		}
		else if(mRC.getEnergon()>48 && Clock.getRoundNum()>=395){
			//48 is the amount of health damage 8 guys surrounding your HQ does
			mRC.setTeamMemory(0, turnOfNuke);
			MapLocation enemyHQ = mRC.senseEnemyHQLocation();
			if(mRC.canSenseSquare(enemyHQ) 
					&& mRC.senseRobotInfo((Robot)mRC.senseObjectAtLocation(enemyHQ)).energon <= 48){
				mRC.setTeamMemory(HOW_ENDED_MEMORY, WE_KILLED);
				// We killed them
			}
			else if(mRC.checkResearchProgress(Upgrade.NUKE) < 399) {
				// Died to nuke
				mRC.setTeamMemory(HOW_ENDED_MEMORY, ENEMY_NUKED);
			}
			else {
				// We nuked them
				mRC.setTeamMemory(HOW_ENDED_MEMORY, WE_NUKED);
			}
		}
		else if(mRC.getEnergon()<=48 && Clock.getRoundNum() < 400){
			mRC.setTeamMemory(ROUND_NUM_MEMORY,Clock.getRoundNum());
			mRC.setTeamMemory(HOW_ENDED_MEMORY, ENEMY_RUSH);
			//died to rush
		}
		else{
			mRC.setTeamMemory(ROUND_NUM_MEMORY,Clock.getRoundNum());
			//died to econ
			mRC.setTeamMemory(HOW_ENDED_MEMORY, ENEMY_ECON);
		}
	}
	
}

class Pair<A extends Comparable<A>, B> implements Comparable<Pair<A, B>>{

	public final A a;
	public final B b;
	
	private Pair(A a, B b){
		this.a = a;
		this.b = b;
	}
	
    public static <A extends Comparable<A>, B> Pair<A, B> of(A a, B b) {
        return new Pair<A, B>(a, b);
    }
	
	@Override
	public int compareTo(Pair<A, B> o) {
		return a.compareTo(o.a);
		//return cmp == 0 ? b.compareTo(o.b) : cmp;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair))
			return false;
		return a.equals(((Pair<?, ?>)obj).a) && b.equals(((Pair<?, ?>)obj).b);
	}
	
}
