package bronzeBot.Robots;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import bronzeBot.Util.RadioChannels;

import static bronzeBot.Util.Constants.*;
import static bronzeBot.Util.Util.*;

public class GeneratorRobot extends EncampmentRobot{
	protected static GeneratorType mType;
	//if you change Generator make sure to change census as well in HQ
	public enum GeneratorType {
		NORMAL
	}
	
	public GeneratorRobot(RobotController rc) {
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
			
			int count = GeneratorRobot.mRadio.readChannel(RadioChannels.CENSUS_START + mType.ordinal() + NUM_SOLDIERTYPES);
			SoldierRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + mType.ordinal() + NUM_SOLDIERTYPES, count + 1);
		}
	}
}
