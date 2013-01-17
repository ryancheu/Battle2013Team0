package bronzeBot.Robots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import bronzeBot.Util.RadioChannels;

import static bronzeBot.Util.Constants.*;
import static bronzeBot.Util.Util.*;

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
