package MineTurtle.Robots.Types;


import java.util.ArrayList;

import MineTurtle.Robots.ARobot;
import MineTurtle.Robots.SoldierRobot;
import MineTurtle.Robots.SoldierRobot.SoldierState;
import MineTurtle.Robots.SoldierRobot.SoldierType;
import battlecode.common.*;
import static MineTurtle.Robots.ARobot.mRC;
import static MineTurtle.Util.Constants.*;
import static MineTurtle.Util.Util.*;
public class SoldierEncampmentType {
	
	private static MapLocation[] waypoints;

	public static void run() throws GameActionException
	{				
		if (mRC.isActive()) {
			switch(SoldierRobot.getState())
			{
			case FIND_ENCAMPMENT: {
				findEncampmentStateLogic();
				break;
			}
			case GOTO_ENCAMPMENT: {
				gotoEncampmentLogic();
				break;
			}
			default:
				break;
				
			}
		}
		else if ( SoldierRobot.getState() == SoldierState.CAPTURING_ENCAMPMENT) {
			capturingStateLogic();			
		}
		
		
		if(waypoints == null) {
			precomputeWaypoints(mRC.getLocation());
		}
		
	}
	
	private static void capturingStateLogic() throws GameActionException 
	{
		
		//Increment the number of turns we've been capturing and write that to the channel
		SoldierRobot.numTurnsCapturing++;
		SoldierRobot.mRadio.writeChannel(ENCAMPMENT_BUILDING_CHAN_START
                +SoldierRobot.mClaimedEncampmentChannel 
                - ENC_CLAIM_RAD_CHAN_START, SoldierRobot.numTurnsCapturing);
	}
	
	private static void findEncampmentStateLogic() throws GameActionException
	{
		// Using the radio broadcasts, find the encampment locations already
		// claimed by other soldiers
		int tempRead, numFound;
		ArrayList<MapLocation> claimedEncampmentLocs = new ArrayList<MapLocation>();
		for (numFound = 0; numFound < numEncToClaim; ++numFound) {
			if ((tempRead = SoldierRobot.mRadio.
					readChannel(numFound + ENC_CLAIM_RAD_CHAN_START)) == -1) {
				break;
			} else {
				claimedEncampmentLocs.add(indexToLocation(tempRead));
			}
		}

		MapLocation[] allEncampments = mRC.senseEncampmentSquares(mRC.getLocation(), 
                                                                  MAX_DIST_SQUARED, Team.NEUTRAL);
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
					break;
				}
			}

			if (alreadyClaimed) {
				alreadyClaimed = false;
				continue;
			}

			if ((tempDist = tempLocation.distanceSquaredTo(mRC.getLocation())) < closestDist) {
				closestDist = tempDist;
				closestIndex = i;
			}
		}

		// Set the destination to the closest non-claimed encampment, and
		// claim the encampment
		if (closestIndex != -1) {
			SoldierRobot.curDest = allEncampments[closestIndex];
			SoldierRobot.mClaimedEncampmentChannel = ENC_CLAIM_RAD_CHAN_START + numFound;
			SoldierRobot.mRadio.writeChannel(SoldierRobot.mClaimedEncampmentChannel, 
					locationToIndex(SoldierRobot.curDest));
			SoldierRobot.switchState(SoldierState.GOTO_ENCAMPMENT);
		} else { // There were no unclaimed encampments
			SoldierRobot.switchType(SoldierType.LAY_MINES);
			SoldierRobot.switchState(SoldierState.MINE);
		}
		return;
	}	
	
	private static void gotoEncampmentLogic() throws GameActionException
	{		
		//Break out of going to an encampment if there's enemies nearby
		if (!checkForEnemies() && SoldierRobot.getDest().equals(mRC.getLocation())) {
			//TODO special case, MEDBAY should be better
			if(mRC.senseCaptureCost() < mRC.getTeamPower()){
				
				int rushDistance = mRC.senseHQLocation().distanceSquaredTo(mRC.senseEnemyHQLocation());
				//int HQDist = rc.senseHQLocation().distanceSquaredTo(rc.getLocation());
				int EnemyHQDist = mRC.senseEnemyHQLocation().distanceSquaredTo(mRC.getLocation());
				/*
				int approxDistanceSquaredFromDirect = (int)((HQDist + EnemyHQDist - rushDistance)/2.0);
				*/
				MapLocation HQ = mRC.senseHQLocation();
				MapLocation EnemyHQ = mRC.senseEnemyHQLocation();
				MapLocation Enc = mRC.getLocation();
				//this long arithmatic is for finding how far from the direct a given Enc is
				int num = Math.abs((EnemyHQ.x - HQ.x)*(HQ.y - Enc.y) 
                                   - (HQ.x - Enc.x)*(EnemyHQ.y-HQ.y));
				double denom = Math.sqrt((double)Math.pow((EnemyHQ.x-HQ.x),2.0)
                                         +Math.pow((EnemyHQ.y - HQ.y),2.0));
				int distanceSquaredFromDirect = (int)Math.pow((num / denom),2);
				try { 
					if(distanceSquaredFromDirect <= 63){
						if(SoldierRobot.mRadio.readChannel(MEDBAY_CLAIMED_RAD_CHAN) == 0 &&
								EnemyHQDist<rushDistance &&
								distanceSquaredFromDirect <=24){
							SoldierRobot.mRadio.writeChannel(MEDBAY_CLAIMED_RAD_CHAN, 1);

							mRC.captureEncampment(RobotType.MEDBAY);																		

						}
						else{	
							mRC.captureEncampment(RobotType.SUPPLIER);
						}
					}
					else{
						mRC.captureEncampment(RobotType.GENERATOR);
					}
					
					SoldierRobot.numTurnsCapturing = 1;
					SoldierRobot.mRadio.writeChannel(ENCAMPMENT_BUILDING_CHAN_START
							+SoldierRobot.mClaimedEncampmentChannel 
							- ENC_CLAIM_RAD_CHAN_START, ENCAMPMENT_CAPTURE_STARTED);
				}
				catch (GameActionException e ) {
					 SoldierRobot.numTurnsCapturing = -1;
				}
				
			}

			return;
		}
		
		if(waypoints == null) {
			waypoints = findWaypoints(mRC.getLocation(), SoldierRobot.getDest());
		}
		if(waypoints == null) {
			goToLocation(SoldierRobot.getDest());
		}
		else {
			goToLocation(findNextWaypoint(waypoints));
		}
	}
	
	private static boolean checkForEnemies () throws GameActionException {
		Robot[] enemyRobots = mRC.senseNearbyGameObjects(Robot.class, SOLDIER_ATTACK_RAD, 
                                                         SoldierRobot.mEnemy);
		
		//If there's enemies nearby cancel the encampment claiming and go into army mode
		if ( enemyRobots.length > 0) {			
			//Remember to clear the channel used to claim the encampment
			SoldierRobot.mRadio.writeChannel(SoldierRobot.mClaimedEncampmentChannel, -1);			
			SoldierRobot.switchType(SoldierType.ARMY);
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return true;			
		}
		return false;
	}
}
