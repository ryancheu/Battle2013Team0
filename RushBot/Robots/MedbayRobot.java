package RushBot.Robots;

import RushBot.Util.RadioChannels;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static RushBot.Util.Constants.*;
import static RushBot.Util.Util.*;

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
