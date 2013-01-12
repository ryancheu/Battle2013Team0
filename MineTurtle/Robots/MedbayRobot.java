package MineTurtle.Robots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

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