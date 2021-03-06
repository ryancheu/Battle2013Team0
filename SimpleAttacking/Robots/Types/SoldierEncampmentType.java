package SimpleAttacking.Robots.Types;


import java.util.ArrayList;



import SimpleAttacking.Robots.ARobot;
import SimpleAttacking.Robots.HQRobot;
import SimpleAttacking.Robots.SoldierRobot;
import SimpleAttacking.Robots.SupplierRobot;
import SimpleAttacking.Robots.SoldierRobot.SoldierState;
import SimpleAttacking.Robots.SoldierRobot.SoldierType;
import SimpleAttacking.Util.RadioChannels;
import battlecode.common.*;
import static SimpleAttacking.Robots.ARobot.mRC;
import static SimpleAttacking.Util.Constants.*;
import static SimpleAttacking.Util.Util.*;
public class SoldierEncampmentType {
	
	private static MapLocation[] waypoints;
	private static int startRound = -1;

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
		performCensus();
				
		if(waypoints == null && SoldierRobot.getState() == SoldierState.GOTO_ENCAMPMENT) {
			precomputeWaypoints(mRC.getLocation());
		}
		
	}
	
	public static void performCensus() throws GameActionException {
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0) {
			int count = SupplierRobot.mRadio.readChannel(SoldierRobot.mCensusRespondChannel );
			SoldierRobot.mRadio.writeChannel(SoldierRobot.mCensusRespondChannel, count + 1);
		}
	}
	
	
	private static void capturingStateLogic() throws GameActionException 
	{
		
		//Increment the number of turns we've been capturing and write that to the channel
		SoldierRobot.numTurnsCapturing++;
		SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
                +SoldierRobot.mClaimedEncampmentChannel 
                - RadioChannels.ENC_CLAIM_START, SoldierRobot.numTurnsCapturing);
	}
	
	private static void findEncampmentStateLogic() throws GameActionException
	{
		// Using the radio broadcasts, find the encampment locations already
		// claimed by other soldiers
		int tempRead, numFound;
		int theNumberToUse = -1;
		ArrayList<MapLocation> claimedEncampmentLocs = new ArrayList<MapLocation>();				
		
		for (numFound = 0; numFound < numEncToClaim; ++numFound) {
			if ((tempRead = SoldierRobot.mRadio.
					readChannel(numFound + RadioChannels.ENC_CLAIM_START)) == -1) {
				if ( theNumberToUse == -1 ){
					theNumberToUse = numFound;
				}
			} else {
				claimedEncampmentLocs.add(indexToLocation(tempRead));
			}
		}
		
		
		mRC.setIndicatorString(2,""+ theNumberToUse);
		SoldierRobot.mClaimedEncampmentChannel = RadioChannels.ENC_CLAIM_START + theNumberToUse;
		SoldierRobot.mRadio.writeChannel(SoldierRobot.mClaimedEncampmentChannel, 0);

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
			//TODO make the fix for not trapping self in encampments more stable
			if ((tempDist = tempLocation.distanceSquaredTo(mRC.getLocation())) < closestDist
					&& ! (tempLocation.equals(SoldierRobot.HQLoc.add(SoldierRobot.HQLoc.directionTo(SoldierRobot.enemyHQLoc))))){
				closestDist = tempDist;
				closestIndex = i;
			}
		}

		// Set the destination to the closest non-claimed encampment, and
		// claim the encampment
		if (closestIndex != -1) {
			SoldierRobot.curDest = allEncampments[closestIndex];			
			//Three because that's how many redudant channels we have
			//TODO: make this a constant
			if ( mRC.getTeamPower()  > GameConstants.BROADCAST_SEND_COST*6  + GameConstants.BROADCAST_READ_COST*6) {
				try {
					SoldierRobot.mRadio.writeChannel(SoldierRobot.mClaimedEncampmentChannel, 
							locationToIndex(SoldierRobot.curDest));
					/*
					SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START + SoldierRobot.mClaimedEncampmentChannel - RadioChannels.ENC_CLAIM_START,ENCAMPMENT_NOT_CLAIMED);
					*/
					SoldierRobot.switchState(SoldierState.GOTO_ENCAMPMENT);
					//print("took encampment" + SoldierRobot.curDest + "chan: " + SoldierRobot.mClaimedEncampmentChannel);
				}
				catch(GameActionException e) {
					//print("uh oh "); 
				}
			}
		} else { // There were no unclaimed encampments
			SoldierRobot.switchType(SoldierType.LAY_MINES);
			SoldierRobot.switchState(SoldierState.MINE);
		}
		
		//print("byte codes after: " + Clock.getBytecodesLeft());
		return;
	}	
	
	private static void gotoEncampmentLogic() throws GameActionException
	{		
		
		//Break out of going to an encampment if there's enemies nearby
		if (!checkForEnemies() && SoldierRobot.getDest().equals(mRC.getLocation())) {
			
			//TODO special case, MEDBAY should be better
			if(mRC.senseCaptureCost() < mRC.getTeamPower()){
				int generatorCount = SoldierRobot.mRadio.readChannel(RadioChannels.NUM_GENERATORS);
				int supplierCount = SoldierRobot.mRadio.readChannel(RadioChannels.NUM_SUPPLIERS);
				int rushDistance = SoldierRobot.HQLoc.distanceSquaredTo(SoldierRobot.enemyHQLoc);
				//int HQDist = rc.SoldierRobot.HQLoc.distanceSquaredTo(rc.getLocation());
				int EnemyHQDist = SoldierRobot.enemyHQLoc.distanceSquaredTo(mRC.getLocation());
				/*
				int approxDistanceSquaredFromDirect = (int)((HQDist + EnemyHQDist - rushDistance)/2.0);
				*/
				MapLocation HQ = SoldierRobot.HQLoc;
				MapLocation EnemyHQ = SoldierRobot.enemyHQLoc;
				MapLocation Enc = mRC.getLocation();
				//this long arithmetic is for finding how far from the direct a given Enc is
				int num = Math.abs((EnemyHQ.x - HQ.x)*(HQ.y - Enc.y) 
                                   - (HQ.x - Enc.x)*(EnemyHQ.y-HQ.y));
				double denom = Math.sqrt((double)Math.pow((EnemyHQ.x-HQ.x),2.0)
                                         +Math.pow((EnemyHQ.y - HQ.y),2.0));
				int distanceSquaredFromDirect = (int)Math.pow((num / denom),2);
				if (mRC.getTeamPower() > mRC.senseCaptureCost() ) {
					try { 
						if(supplierCount<=13){
							
							if(SoldierRobot.mRadio.readChannel(RadioChannels.MEDBAY_CLAIMED) == 0 &&
									EnemyHQDist<rushDistance &&
									distanceSquaredFromDirect <=24){
								SoldierRobot.mRadio.writeChannel(RadioChannels.MEDBAY_CLAIMED, 1);
								SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
										+ SoldierRobot.mClaimedEncampmentChannel 
										- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_CAPTURE_STARTED);
								if ( mRC.getTeamPower() > mRC.senseCaptureCost() ) {
									mRC.captureEncampment(RobotType.MEDBAY);																		
								}
								else {
									SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
											+ SoldierRobot.mClaimedEncampmentChannel 
											- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
									SoldierRobot.mRadio.writeChannel(RadioChannels.MEDBAY_CLAIMED, 0);
								}
								
							}
							else if(generatorCount==0 || supplierCount/((double)generatorCount) > RATIO_OF_SUPPLIERS_OVER_GENERATORS) {							
								SoldierRobot.mCensusRespondChannel = RadioChannels.CENSUS_START + RobotType.GENERATOR.ordinal() + NUM_SOLDIERTYPES;
								int count = SoldierRobot.mRadio.readChannel(RadioChannels.NUM_GENERATORS);
								SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS, count+1);
								SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
										+ SoldierRobot.mClaimedEncampmentChannel 
										- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_CAPTURE_STARTED);
								if ( mRC.getTeamPower() > mRC.senseCaptureCost() ) {
									mRC.captureEncampment(RobotType.GENERATOR);								
								}
								else {
									SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
											+ SoldierRobot.mClaimedEncampmentChannel 
											- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
									SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS, count);
								}							
							}
							else {							
								SoldierRobot.mCensusRespondChannel = RadioChannels.CENSUS_START + RobotType.SUPPLIER.ordinal() + NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES;
								int count = SoldierRobot.mRadio.readChannel(RadioChannels.NUM_SUPPLIERS);
								SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS, count+1);							
								SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
										+ SoldierRobot.mClaimedEncampmentChannel 
										- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_CAPTURE_STARTED);
								if ( mRC.getTeamPower() > mRC.senseCaptureCost()) {
									mRC.captureEncampment(RobotType.SUPPLIER);
								}
								else {
									SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
											+ SoldierRobot.mClaimedEncampmentChannel 
											- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
									SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS, count);
								}
							}
						}
						else{
							SoldierRobot.mCensusRespondChannel = RadioChannels.CENSUS_START + RobotType.GENERATOR.ordinal() + NUM_SOLDIERTYPES;
							int count = SoldierRobot.mRadio.readChannel(RadioChannels.NUM_GENERATORS);
							SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS, count+1);
							SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
									+ SoldierRobot.mClaimedEncampmentChannel 
									- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_CAPTURE_STARTED);
							if ( mRC.getTeamPower() > mRC.senseCaptureCost() ) {
								mRC.captureEncampment(RobotType.GENERATOR);
							}
							else {
								SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
										+ SoldierRobot.mClaimedEncampmentChannel 
										- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
								SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS, count);
							}
						}			
					}
					catch (GameActionException e ) {
						/*
						 SoldierRobot.numTurnsCapturing = -1;
						 SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
									+ SoldierRobot.mClaimedEncampmentChannel 
									- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
						*/
						 
					}
				}
				
			}

			return;
		}
		
		int dist = Math.max(Math.abs(SoldierRobot.getDest().x - mRC.getLocation().x),
				Math.abs(SoldierRobot.getDest().y - mRC.getLocation().y)) - 1;
		if(mRC.getEnergon() > GameConstants.MINE_DAMAGE * dist + 1) {
			Direction dir = mRC.getLocation().directionTo(SoldierRobot.getDest());
			if(dir.ordinal() < NUM_DIR && mRC.canMove(dir)) {
				mRC.move(dir);
				return;
			}
			else {
				startRound -= 10;
			}
		}
		
		if(startRound == -1) {
			startRound = Clock.getRoundNum();
		}
		if(Clock.getRoundNum() - startRound > GOTO_ENCAMPMENT_MAX_ROUNDS) {
			SoldierRobot.switchType(SoldierType.ARMY);
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}
		
		if(mRC.canSenseSquare(SoldierRobot.getDest())){
			GameObject o = mRC.senseObjectAtLocation(SoldierRobot.getDest());
			if(o != null && o.getClass() == Robot.class) {
				if(mRC.senseRobotInfo((Robot)o).type != RobotType.SOLDIER) {
					SoldierRobot.switchType(SoldierType.ARMY);
					SoldierRobot.switchState(SoldierState.GOTO_RALLY);
					return;
				}
			}
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
