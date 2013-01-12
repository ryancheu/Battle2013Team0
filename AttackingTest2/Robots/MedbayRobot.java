package AttackingTest2.Robots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static AttackingTest2.Util.Constants.*;
import static AttackingTest2.Util.Util.*;

public class MedbayRobot extends ARobot{
	
	public MedbayRobot(RobotController rc) {
		super(rc);
		
	}

	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		mRadio.writeChannel(MEDBAY_LOCATION_CHAN, locationToIndex(mRC.getLocation()));
	}
}
