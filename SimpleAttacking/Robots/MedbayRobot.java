package SimpleAttacking.Robots;

import SimpleAttacking.Util.RadioChannels;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static SimpleAttacking.Util.Constants.*;
import static SimpleAttacking.Util.Util.*;

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
