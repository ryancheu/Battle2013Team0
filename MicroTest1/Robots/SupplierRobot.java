package MicroTest1.Robots;

import MicroTest1.Robots.SoldierRobot.SoldierType;
import MicroTest1.Util.RadioChannels;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import static MicroTest1.Util.Constants.*;
import static MicroTest1.Util.Util.*;

public class SupplierRobot extends EncampmentRobot{
	protected static SupplierType mType;
	//if you change Supplier, make sure your change works with the census
	public enum SupplierType {
		NORMAL
	}
	
	public SupplierRobot(RobotController rc) throws GameActionException {
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
			int count = SupplierRobot.mRadio.readChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES );
			SoldierRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES, count + 1);
		}
	}
}
