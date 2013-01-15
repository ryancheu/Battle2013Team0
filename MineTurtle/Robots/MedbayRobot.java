package MineTurtle.Robots;

import MineTurtle.Util.RadioChannels;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;

public class MedbayRobot extends EncampmentRobot{
	
	public MedbayRobot(RobotController rc) {
		super(rc);
		
	}

	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		mRadio.writeChannel(RadioChannels.MEDBAY_LOCATION, locationToIndex(mRC.getLocation()));
	}
}
