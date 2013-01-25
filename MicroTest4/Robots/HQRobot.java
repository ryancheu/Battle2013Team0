package MicroTest4.Robots;

import java.util.Arrays;



import MicroTest4.Robots.Types.HQFasterNukeType;
import MicroTest4.Robots.Types.HQNormalType;
import MicroTest4.Robots.Types.HQNukeType;
import MicroTest4.Robots.Types.HQRushType;
import MicroTest4.Util.RadioChannels;
import battlecode.common.*;

import static MicroTest4.Robots.ARobot.mRC;
import static MicroTest4.Util.Constants.*;
import static MicroTest4.Util.Util.*;

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
	
	public static void chooseType(){
		//Ideally this will decide based on RUSHDISTANCE, num of neutral mines, team memory
		//TODO: add dependance on what size map the previous one was, for instance, if it was a big map and we lost with nuke def dont do nuke
		long roundNum = mRC.getTeamMemory()[ROUND_NUM_MEMORY];
		//if howEnded == Enemy_Nuked then roundNum = the round we think they started nuke
		long howEnded = mRC.getTeamMemory()[HOW_ENDED_MEMORY];
		long howWePlayed = mRC.getTeamMemory()[HOW_WE_PLAYED_MEMORY];
		int directRushDistanceSquared = HQRobot.enemyHQLoc.distanceSquaredTo(mRC.getLocation());
		if(roundNum != 0 || howEnded != 0 || howWePlayed != 0){
			//they can be used
			if (howEnded == ENEMY_ECON && directRushDistanceSquared < 1500 ) {
				
				mType = HQType.RUSH;
				mState = HQState.TURTLE;
			}
			else if(howEnded == ENEMY_NUKED && howWePlayed == FASTER_NUKE_TYPE && directRushDistanceSquared < 3000){
				//their nuke is faster than our fast nuke...they must be hacking. Rush
				mType = HQType.RUSH;
				mState = HQState.TURTLE;
			}
			else if(howEnded == TIEBREAKERS && directRushDistanceSquared > 2000){
				mType = HQType.ECON;
				mState = HQState.TURTLE;
			}
			else if(howEnded == WE_NUKED && howWePlayed != FASTER_NUKE_TYPE && directRushDistanceSquared > 3000){
				mType = HQType.NUKE;
				mState = HQState.TURTLE;
			}
			else if(howEnded == ENEMY_NUKED && howWePlayed != FASTER_NUKE_TYPE && directRushDistanceSquared > 1500){
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
				//if we rushed or econed we end up here
				mType = HQType.ECON;
				mState = HQState.TURTLE;
			}
		}
		else{
			if (HQRobot.enemyHQLoc.distanceSquaredTo(mRC.getLocation()) < 1000 ) {
				mType = HQType.ECON;
				mState = HQState.TURTLE;
			}
			else if(directRushDistanceSquared > 5000){
				mType = HQType.NUKE;
				mState = HQState.TURTLE;
			}
			else if(directRushDistanceSquared > 7000){
				mType = HQType.FASTER_NUKE;
				mState = HQState.TURTLE;
			}
			else {
				mType = HQType.ECON;
				mState = HQState.TURTLE;
			}
		}
	}
	private void mainHQLogic() throws GameActionException {
		if (mType == null )
		{
			chooseType();
		}
		HQState lastState = mState;
		broadcastTypeAndState();
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
