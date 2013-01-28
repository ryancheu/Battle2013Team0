package OldSchoolBot.Robots;

import static OldSchoolBot.Util.Util.locationToIndex;
import OldSchoolBot.Util.RadioChannels;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class ShieldRobot extends EncampmentRobot{

	public ShieldRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
	}
	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		mRadio.writeChannel(RadioChannels.SHIELD_LOCATION, locationToIndex(mRC.getLocation()));
	}
}
