package MicroTest1.Robots;

import MicroTest1.Util.*;
import battlecode.common.*;
import static MicroTest1.Util.Constants.*;
import static MicroTest1.Util.Util.*;

public class EncampmentRobot extends ARobot {

	public EncampmentRobot(RobotController rc) throws GameActionException {
		super(rc);
		HQRobot.readTypeAndState();
	}

	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		
		if(mRC.senseNearbyGameObjects(Robot.class, ENCAMPMENT_PROTECT_RAD_SQUARED, mEnemy).length > 0)
			mRadio.writeChannel(RadioChannels.ENCAMPMENT_IN_DANGER, locationToIndex(mRC.getLocation()));
	}

}
