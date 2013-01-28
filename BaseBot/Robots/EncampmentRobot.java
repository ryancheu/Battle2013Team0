package BaseBot.Robots;

import BaseBot.Util.*;
import battlecode.common.*;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.Util.*;

public class EncampmentRobot extends ARobot {

	public EncampmentRobot(RobotController rc) throws GameActionException {
		super(rc);
		HQRobot.readTypeAndState();
	}

	@Override
	public void takeTurn() throws GameActionException {
		super.takeTurn();
		GameObject[] enemiesInArea = mRC.senseNearbyGameObjects(Robot.class, ENCAMPMENT_PROTECT_RAD_SQUARED, mEnemy);
		if(enemiesInArea.length > 0){
			mRadio.writeChannel(RadioChannels.ENCAMPMENT_IN_DANGER, locationToIndex(mRC.getLocation()));
		}
		//nothing is done with ID_OF_HARASSER yet :(
		if(enemiesInArea.length == 1){
			mRadio.writeChannel(RadioChannels.ID_OF_HARASSER, enemiesInArea[0].getID());
		}
	}

}
