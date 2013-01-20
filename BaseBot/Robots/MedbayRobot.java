package BaseBot.Robots;

import BaseBot.Util.RadioChannels;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static BaseBot.Util.EconConstants.*;
import static BaseBot.Util.Util.*;

public class MedbayRobot extends EncampmentRobot{
	
	public MedbayRobot(RobotController rc) throws GameActionException {
		super(rc);
		
	}

	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		mRadio.writeChannel(RadioChannels.MEDBAY_LOCATION, locationToIndex(mRC.getLocation()));
	}
}
