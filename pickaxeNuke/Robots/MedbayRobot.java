package pickaxeNuke.Robots;

import pickaxeNuke.Util.RadioChannels;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static pickaxeNuke.Util.Constants.*;
import static pickaxeNuke.Util.Util.*;

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
