package pickaxeNuke.Robots;

import pickaxeNuke.Util.*;
import battlecode.common.*;
import static pickaxeNuke.Util.Constants.*;
import static pickaxeNuke.Util.Util.*;

public class EncampmentRobot extends ARobot {

	public EncampmentRobot(RobotController rc) {
		super(rc);
	}

	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		if(mRC.senseNearbyGameObjects(Robot.class, ENCAMPMENT_PROTECT_RAD_SQUARED, mEnemy).length > 0)
			mRadio.writeChannel(RadioChannels.ENCAMPMENT_IN_DANGER, locationToIndex(mRC.getLocation()));
	}

}
