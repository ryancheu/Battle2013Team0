package MineTurtle.Robots;

import MineTurtle.Robots.Types.*;
import battlecode.common.*;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

public class SoldierRobot extends ARobot{
	
	public enum SoldierType {
		OCCUPY_ENCAMPMENT, 
		LAY_MINES,
		ARMY,
		ARTILLERY
	}
	public enum SoldierState {

		// ENCAMPMENT SOLDIER
		FIND_ENCAMPMENT, 
		GOTO_ENCAMPMENT,		

		// MINE SOLDIER
		MINE,
		
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
			for (int i = ENC_CLAIM_RAD_CHAN_START; i < ENC_CLAIM_RAD_CHAN_START + NUM_ENC_TO_CLAIM; i++) {
				if (mRadio.readChannel(i) == -1) {
					mType = SoldierType.OCCUPY_ENCAMPMENT;
					mState = SoldierState.FIND_ENCAMPMENT;					
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
				}
			}
			if ( mType == null )
			{
				mType = SoldierType.ARMY;
				mState = SoldierState.GOTO_RALLY;
			}
		}

		switch (mType) {
			case OCCUPY_ENCAMPMENT:
				SoldierEncampmentType.run(mRC);
				break;
			case LAY_MINES:
				SoldierLayMineType.run(mRC);
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
