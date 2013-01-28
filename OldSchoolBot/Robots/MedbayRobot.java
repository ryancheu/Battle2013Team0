package OldSchoolBot.Robots;

import OldSchoolBot.Util.RadioChannels;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static OldSchoolBot.Util.Constants.*;
import static OldSchoolBot.Util.Util.*;

public class MedbayRobot extends EncampmentRobot{
	
	public MedbayRobot(RobotController rc) throws GameActionException {
		super(rc);
		
	}

	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		int old = mRadio.readChannel(RadioChannels.MEDBAY_LOCATION);
		if(old > 0 && indexToLocation(old).distanceSquaredTo(mRC.senseEnemyHQLocation())
				< mRC.getLocation().distanceSquaredTo(mRC.senseEnemyHQLocation()))
			return;
		mRadio.writeChannel(RadioChannels.MEDBAY_LOCATION, locationToIndex(mRC.getLocation()));
	}
}
