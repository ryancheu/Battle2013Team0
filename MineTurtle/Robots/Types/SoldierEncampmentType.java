package MineTurtle.Robots.Types;


import java.util.ArrayList;

import battlecode.common.*;
import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.SoldierRobot.SoldierState;
import MineTurtle.Robots.SoldierRobot.SoldierType;
import static MineTurtle.Util.Util.*;
import static MineTurtle.Util.Constants.*;

public class SoldierEncampmentType {
	
	public static void run(RobotController rc) throws GameActionException
	{
		if (rc.isActive()) {
			switch(SoldierRobot.getState())
			{
			case FIND_ENCAMPMENT: {
				findEncampmentStateLogic(rc);
				break;
			}
			case GOTO_ENCAMPMENT: {
				gotoEncampmentLogic(rc);
				break;
			}
			default:
				break;
				
			}
		}
	}
	
	private static void findEncampmentStateLogic(RobotController rc) throws GameActionException
	{		
		// Using the radio broadcasts, find the encampment locations already
		// claimed by other soldiers
		int tempRead, numFound;
		ArrayList<MapLocation> claimedEncampmentLocs = new ArrayList<MapLocation>();
		for (numFound = 0; numFound < NUM_ENC_TO_CLAIM; ++numFound) {
			if ((tempRead = SoldierRobot.mRadio.
					readChannel(numFound + ENC_CLAIM_RAD_CHAN_START)) == -1) {
				break;
			} else {
				claimedEncampmentLocs.add(indexToLocation(rc,tempRead));
			}
		}

		MapLocation[] allEncampments = rc.senseEncampmentSquares(rc.getLocation(), MAX_DIST_SQUARED, Team.NEUTRAL);
		int closestDist = MAX_DIST_SQUARED;
		int closestIndex = -1;
		int tempDist;		
		MapLocation tempLocation;
		boolean alreadyClaimed = false;

		// Search through all encampments and find the closest one not
		// already claimed		
		for (int i = 0; i < allEncampments.length; i++) {
			tempLocation = allEncampments[i];

			for (MapLocation l : claimedEncampmentLocs) {
				if (l.equals(tempLocation)) {
					alreadyClaimed = true;
					print("location: " + locationToIndex(rc,tempLocation));
					break;
				}
			}

			if (alreadyClaimed) {
				alreadyClaimed = false;
				continue;
			}

			if ((tempDist = tempLocation.distanceSquaredTo(rc.getLocation())) < closestDist) {
				closestDist = tempDist;
				closestIndex = i;
			}
		}

		// Set the destination to the closest non-claimed encampment, and
		// claim the encampment
		if (closestIndex != -1) {
			SoldierRobot.curDest = allEncampments[closestIndex];
			SoldierRobot.mRadio.writeChannel(ENC_CLAIM_RAD_CHAN_START + numFound, 
					locationToIndex(rc,SoldierRobot.curDest));
			SoldierRobot.switchState(SoldierState.GOTO_ENCAMPMENT);
		} else { // There were no unclaimed encampments
			SoldierRobot.switchType(SoldierType.LAY_MINES);
			SoldierRobot.switchState(SoldierState.MINE);
		}
		return;
	}
	
	private static void gotoEncampmentLogic(RobotController rc) throws GameActionException
	{
		if (SoldierRobot.getDest().equals(rc.getLocation())) {
			if(rc.senseCaptureCost() < rc.getTeamPower())
				rc.captureEncampment(RobotType.ARTILLERY);
			return;
		}
		goToLocation(rc, SoldierRobot.getDest());
	}
}
