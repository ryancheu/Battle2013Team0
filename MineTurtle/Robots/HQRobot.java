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
	
	protected static HQState mState;
	protected static HQType mType;
	public static MapLocation enemyHQLoc;
	protected static Direction enHQDir;
	
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
				HQNormalType.run(mRC);
			}
		}
		
	}

	
	public static HQState getState() {
		return mState;
	}
	
	public static void setRallyPoint(MapLocation loc) throws GameActionException {
		mRadio.writeChannel(RALLY_RAD_CHAN, locationToIndex(mRC,loc));	
	}
	
	public static void switchState(HQState state) {
		mState = state;
		mRC.setIndicatorString(state.ordinal(), "State");
	}
	public static void switchType(HQType type) {
		mType = type; 
		mRC.setIndicatorString(type.ordinal(), "Type");
	}
	
	public static void spawnRobot() throws GameActionException {
		Direction tempDir = null;
		for (int i = 0; i < NUM_DIR; ++i) {
			tempDir = Direction.values()[(enHQDir.ordinal() + i + NUM_DIR) % NUM_DIR];
			if (mRC.canMove(tempDir)) {
				mRC.spawn(tempDir);
				break;
			}
		}
	}
	
}
