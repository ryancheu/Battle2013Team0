package MicroTest4.Robots;

import MicroTest4.Util.RadioChannels;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static MicroTest4.Util.Constants.*;
import static MicroTest4.Util.Util.*;

public class GeneratorRobot extends EncampmentRobot{
	protected static GeneratorType mType;
	//if you change Generator make sure to change census as well in HQ
	public enum GeneratorType {
		NORMAL
	}
	
	public GeneratorRobot(RobotController rc) throws GameActionException {
		super(rc);
		mType = GeneratorType.NORMAL;
	}

	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		performCensus();
		
	}
	
	public static void performCensus() throws GameActionException {
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0) {
			
			int count = GeneratorRobot.mRadio.readChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES);
			print(" generator census count: " + count);
			SoldierRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES, count + 1);
		}
	}
}
