package MineTurtle.Robots;

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
		ATTACK_HQ,
	}
	
	
	public static MapLocation curDest;
	public static MapLocation enemyHQLoc;
	protected static SoldierState mState;
	protected static SoldierType mType;
	
	
	public static SoldierState getState() 
	{
		return mState;
	}
	
	public static MapLocation getDest()
	{
		return curDest;
	}

	
	public SoldierRobot(RobotController rc) {
		super(rc);
		mRC = rc;
		enemyHQLoc = rc.senseEnemyHQLocation();
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
		mRC.setIndicatorString(state.ordinal(), "State");
	}
	public static void switchType(SoldierType type) {
		mType = type; 
		mRC.setIndicatorString(type.ordinal(), "Type");
	}
	
	public static MapLocation findRallyPoint(RobotController rc) throws GameActionException {
		// TODO Auto-generated method stub
		return indexToLocation(rc,mRadio.readChannel(RALLY_RAD_CHAN));
	}
	

}
