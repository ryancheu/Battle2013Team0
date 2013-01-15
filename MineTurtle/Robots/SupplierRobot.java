package MineTurtle.Robots;

import MineTurtle.Robots.SoldierRobot.SoldierType;
import MineTurtle.Util.RadioChannels;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

public class SupplierRobot extends ARobot{
	protected static SupplierType mType;
	//if you change Supplier, make sure your change works with the census
	public enum SupplierType {
		NORMAL
	}
	
	public SupplierRobot(RobotController rc) {
		super(rc);
		mType = SupplierType.NORMAL;
	}

	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		performCensus();
		
	}
	
	public static void performCensus() throws GameActionException {
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0) {
			int count = SupplierRobot.mRadio.readChannel(RadioChannels.CENSUS_START + mType.ordinal() + NUM_OF_CENSUS_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES + 1);
			SoldierRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + mType.ordinal() + NUM_OF_CENSUS_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES + 1, count + 1);
		}
	}
}
