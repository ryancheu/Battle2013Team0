package MineTurtle.Robots;

import MineTurtle.Robots.Types.HQNormalType;
import battlecode.common.*;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

public class HQRobot extends ARobot{
	
	//Maybe remove? kept in for organization
	public enum HQType  {
		NORMAL,
	}
	public enum HQState { 		
		TURTLE,
		PREPARE_ATTACK,
		ATTACK
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

	public static void setRallyPoint(MapLocation loc) throws GameActionException {
		int message = Clock.getRoundNum() 
				| (HQ_ATTACK_RALLY_CHAN_START << WAYPOINT_ROUND_BITS) 
				| (1 << (WAYPOINT_ROUND_BITS + WAYPOINT_START_CHAN_BITS));
		HQRobot.mRadio.writeChannel(SOLDIER_WAYPOINT_RALLY_CHAN, message);
		HQRobot.mRadio.writeChannel(HQ_ATTACK_RALLY_CHAN_START, locationToIndex(loc));
	}

	public static void setRallyPoints(MapLocation[] locs) throws GameActionException {
		int message = Clock.getRoundNum() 
				| (HQ_ATTACK_RALLY_CHAN_START << WAYPOINT_ROUND_BITS) 
				| (locs.length << (WAYPOINT_ROUND_BITS + WAYPOINT_START_CHAN_BITS));
		HQRobot.mRadio.writeChannel(SOLDIER_WAYPOINT_RALLY_CHAN, message);
		for(int n=0; n<locs.length; ++n)
			HQRobot.mRadio.writeChannel(HQ_ATTACK_RALLY_CHAN_START + n, locationToIndex(locs[n]));
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
			if (mRC.canMove(tempDir)) {
				mRC.spawn(tempDir);
				break;
			}
		}
		HQRobot.mRadio.writeChannel(NEXT_SOLDIER_TYPE_CHAN, type.ordinal());
	}
	
}
