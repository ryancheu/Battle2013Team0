package pickaxeNuke.Robots;

import pickaxeNuke.Robots.Types.HQNormalType;
import pickaxeNuke.Util.RadioChannels;
import battlecode.common.*;

import static pickaxeNuke.Util.Constants.*;
import static pickaxeNuke.Util.Util.*;

public class HQRobot extends ARobot{
	
	//Maybe remove? kept in for organization
	public enum HQType  {
		NORMAL,
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
	protected static Direction enHQDir;
	
	public static MapLocation enemyLastSeenPosAvg;
	
	public static boolean enemyNukeSoon = false;
	
	public HQRobot(RobotController rc) {
		super(rc);
		enemyHQLoc = rc.senseEnemyHQLocation();
		enHQDir = rc.getLocation().directionTo(enemyHQLoc);
	}
	
	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		mainHQLogic();
	}
	
	private void mainHQLogic() throws GameActionException {
		if (mType == null )
		{
			mType = HQType.NORMAL;
			mState = HQState.TURTLE;
		}
		switch(mType)
		{
			case NORMAL: 
			{
				HQNormalType.run();
			}
		}
		
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
		HQRobot.mRadio.writeChannel(RadioChannels.SOLDIER_WAYPOINT_RALLY, message);
		HQRobot.mRadio.writeChannel(RadioChannels.HQ_ATTACK_RALLY_START, locationToIndex(loc));
		// HQRobot.mRadio.writeChannel(BACKUP_RALLY_POINT_RAD_CHAN, locationToIndex(loc));
	}

	public static void setRallyPoints(MapLocation[] locs) throws GameActionException {
		setRallyPoints(locs, locs.length);
	}
	
	public static void setRallyPoints(MapLocation[] locs, int length) throws GameActionException {
		int message = Clock.getRoundNum() 
				| (RadioChannels.HQ_ATTACK_RALLY_START << WAYPOINT_ROUND_BITS) 
				| (length << (WAYPOINT_ROUND_BITS + WAYPOINT_START_CHAN_BITS));
		HQRobot.mRadio.writeChannel(RadioChannels.SOLDIER_WAYPOINT_RALLY, message);
		for(int n=0; n<length; ++n) {
			HQRobot.mRadio.writeChannel(RadioChannels.HQ_ATTACK_RALLY_START + n, locationToIndex(locs[n]));
		}
		// HQRobot.mRadio.writeChannel(BACKUP_RALLY_POINT_RAD_CHAN, locationToIndex(locs[length-1]));
	}
	
	public static void switchState(HQState state) {
		mLastState = mState;
		mState = state;
		mRC.setIndicatorString(1, mState.toString());
	}
	public static void switchType(HQType type) {
		mType = type; 
		mRC.setIndicatorString(0, mType.toString());
	}
	
	public static void spawnRobot(SoldierRobot.SoldierType type) throws GameActionException {
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
