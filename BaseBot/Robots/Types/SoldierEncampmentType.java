package BaseBot.Robots.Types;

import BaseBot.Robots.HQRobot;
import BaseBot.Robots.SoldierRobot;
import BaseBot.Robots.SupplierRobot;
import BaseBot.Robots.SoldierRobot.SoldierState;
import BaseBot.Robots.SoldierRobot.SoldierType;
import BaseBot.Util.RadioChannels;
import battlecode.common.*;
import static BaseBot.Robots.ARobot.mRC;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.NonConstants.*;
import static BaseBot.Util.Util.*;
public class SoldierEncampmentType {
	
	private static MapLocation[] waypoints;
	private static int startRound = -1;
	
	private static int numArmy = -1;
	
	private static boolean waiting = false;

	public static void run() throws GameActionException
	{
		if (numArmy == -1) {
			numArmy = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, SoldierRobot.mTeam).length - mRC.senseAlliedEncampmentSquares().length;
		}
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
		else {
			if ( startRound != -1 ) 
			{
				startRound++;
			}
		}	
		performCensus();
				
		if(waypoints == null && SoldierRobot.getState() == SoldierState.GOTO_ENCAMPMENT) {
			precomputeWaypoints(mRC.getLocation());
		}
		
	}
	
	public static void performCensus() throws GameActionException {
		if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0 && SoldierRobot.mCensusRespondChannel != -1) {
			int count = SupplierRobot.mRadio.readChannel(SoldierRobot.mCensusRespondChannel );
			SoldierRobot.mRadio.writeChannel(SoldierRobot.mCensusRespondChannel, count + 1);
			
			if ( waiting ) {
				int numWaiting = SoldierRobot.mRadio.readChannel(RadioChannels.ENC_SOLDIER_WAITING );
				SoldierRobot.mRadio.writeChannel(RadioChannels.ENC_SOLDIER_WAITING, numWaiting +1 );
			}
		}
		if ( SoldierRobot.isMedbay) {
			SoldierRobot.mRadio.writeChannel(RadioChannels.MEDBAY_LOCATION, locationToIndex(mRC.getLocation()));
		}
		
		//figure out how many soldiers there are
		if ( Clock.getRoundNum() % CENSUS_INTERVAL ==1 ) {
			numArmy = SoldierRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.ARMY.ordinal());
		}
	}
	
	private static void findEncampmentStateLogic() throws GameActionException
	{
		
		int a = Clock.getBytecodesLeft();
		int b = Clock.getRoundNum();
		
		boolean[] spotTaken = new boolean[70*70];
		
		mRC.setIndicatorString(0, "Start Encampment: " + a + "Round: " + b);
		
		// Using the radio broadcasts, find the encampment locations already
		// claimed by other soldiers
		int tempRead, numFound;
		int theNumberToUse = -1;
		//ArrayList<MapLocation> claimedEncampmentLocs = new ArrayList<MapLocation>();		
		
		int maxChannelToCheck = SoldierRobot.mRadio.readChannel(RadioChannels.MAX_ENC_CHANNEL_TO_CHECK);
		//print("max Channel: " + maxChannelToCheck);
		
		//print("a: " + Clock.getBytecodeNum() + " round : " + Clock.getRoundNum());
		
		
		for (numFound = 0; numFound < maxChannelToCheck+BUFFER_ENC_CHANNEL_CHECK; ++numFound) {
			if ((tempRead = SoldierRobot.mRadio.
					readChannel(numFound + RadioChannels.ENC_CLAIM_START) - 1) <= 0) { //subtract 1 since 1 is added when we claim the channel
				if ( theNumberToUse == -1 ){
					theNumberToUse = numFound;
				}
			} else {
				//print("taken spot: " + tempRead);
				if ( tempRead < spotTaken.length ) {
					spotTaken[tempRead] = true;
				}
			}
		}
				
		//For all encampments that our HQ has declared "UNUSABLE," mark them as SpotTaken
		int numBadLocations =SoldierRobot.mRadio.readChannel(RadioChannels.NUM_BAD_ENCAMPMENTS)^ FIRST_BYTE_KEY;
		//loop through our bad encampment locations
		for(int q = 1;q<numBadLocations;q++)
		{
			int badIndex = SoldierRobot.mRadio.readChannel(RadioChannels.NUM_BAD_ENCAMPMENTS+q)^ FIRST_BYTE_KEY;
			//if their index is reasonable, make it a claimed spot.
			if(badIndex>=0 && badIndex < spotTaken.length)
			{
				spotTaken[badIndex]=true;
			}
		}
		mRC.setIndicatorString(2,""+ theNumberToUse);
		SoldierRobot.mClaimedEncampmentChannel = RadioChannels.ENC_CLAIM_START + theNumberToUse;
		SoldierRobot.mRadio.writeChannel(SoldierRobot.mClaimedEncampmentChannel, 
				GameConstants.MAP_MAX_HEIGHT*GameConstants.MAP_MAX_WIDTH + 1); //max map width/height + 1

		MapLocation[] allEncampments = mRC.senseEncampmentSquares(mRC.getLocation(), 
                MAX_DIST_SQUARED, Team.NEUTRAL);
		int closestDist = MAX_DIST_SQUARED;
		int closestIndex = -1;
		int tempDist;		
		MapLocation tempLocation;	

		// Search through all encampments and find the closest one not
		// already claimed		
		for (int i = 0; i < allEncampments.length; i++) {
			tempLocation = allEncampments[i];
			
			//TODO make the fix for not trapping self in encampments more stable
			if ((tempDist = tempLocation.distanceSquaredTo(mRC.getLocation())) < closestDist
					&& ! (tempLocation.equals(SoldierRobot.HQLoc.add(SoldierRobot.HQLoc.directionTo(SoldierRobot.enemyHQLoc))))){
				
				if ( spotTaken[locationToIndex(tempLocation)] ) { 
					continue;
				}

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
							locationToIndex(SoldierRobot.curDest) + 1 ); //Add 1 so message don't have to be  intialized to 0
					
					//print("took: " + SoldierRobot.curDest.toString());
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
		
		//print( "bytecodes used: " + (a - Clock.getBytecodesLeft()) + "rounds taken: " + (Clock.getRoundNum()-b));
		mRC.setIndicatorString(1, "End Encampments: " + (a-Clock.getBytecodesLeft()) + "rounds taken: " + (Clock.getRoundNum()-b));
		
		//print("byte codes after: " + Clock.getBytecodesLeft());
		return;
	}	

	private static void gotoEncampmentLogic() throws GameActionException
	{		
		HQRobot.readTypeAndState();
		if (!checkForEnemies() && SoldierRobot.getDest().equals(mRC.getLocation())) {

			//TODO special case, MEDBAY should be better
			if(mRC.senseCaptureCost() < mRC.getTeamPower()){
				
				
				
				double generatorCount = SoldierRobot.mRadio.readChannel(RadioChannels.NUM_GENERATORS);
				double supplierCount = SoldierRobot.mRadio.readChannel(RadioChannels.NUM_SUPPLIERS);
				double artilleryCount = SoldierRobot.mRadio.readChannel(RadioChannels.NUM_ARTILLERY);
				//print("army: " + numArmy);
				//print("generators: " + generatorCount);
				//print("suppliers: " + supplierCount);
				
				int rushDistance = SoldierRobot.HQLoc.distanceSquaredTo(SoldierRobot.enemyHQLoc);
				//int HQDist = rc.SoldierRobot.HQLoc.distanceSquaredTo(rc.getLocation());
				int EnemyHQDist = SoldierRobot.enemyHQLoc.distanceSquaredTo(mRC.getLocation());
				/*
						int approxDistanceSquaredFromDirect = (int)((HQDist + EnemyHQDist - rushDistance)/2.0);
				 */
				/*
				MapLocation HQ = SoldierRobot.HQLoc;
				MapLocation EnemyHQ = SoldierRobot.enemyHQLoc;
				MapLocation Enc = mRC.getLocation();		//intializeEncampentList();
				//this long arithmetic is for finding how far from the direct a given Enc is
				/*
				int num = Math.abs((EnemyHQ.x - HQ.x)*(HQ.y - Enc.y) 
						- (HQ.x - Enc.x)*(EnemyHQ.y-HQ.y));
				double denom = Math.sqrt((double)Math.pow((EnemyHQ.x-HQ.x),2.0)
						+Math.pow((EnemyHQ.y - HQ.y),2.0));
				int distanceSquaredFromDirect = (int)Math.pow((num / denom),2); */


				//Check if the square is a reasonable medbay location. See if there are four non-encampment squares
				//print ("supplier artillery thign" + NUM_GENERATORSUPPLIER_PER_ARTILLERY);
				if (mRC.getTeamPower() > mRC.senseCaptureCost() ) {
					try { 
						if(SoldierRobot.mRadio.readChannel(RadioChannels.MEDBAY_CLAIMED) == 0 &&
								supplierCount + generatorCount >= NUM_SUPPLIER_OR_GENERATOR_BEFORE_MEDBAY && 
								EnemyHQDist<rushDistance &&mRC.senseEncampmentSquares(SoldierRobot.curDest,2,null).length<5){
							SoldierRobot.mRadio.writeChannel(RadioChannels.MEDBAY_CLAIMED, Clock.getRoundNum());
							SoldierRobot.mRadio.writeChannel(RadioChannels.MEDBAY_LOCATION, locationToIndex(mRC.getLocation()));									
							SoldierRobot.isMedbay = true;
							SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
									+ SoldierRobot.mClaimedEncampmentChannel 
									- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_CAPTURE_STARTED);
							if ( mRC.getTeamPower() > mRC.senseCaptureCost() ) {
								mRC.captureEncampment(RobotType.MEDBAY);
								waiting = false;								
							}
							else {
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
									//print("writing claimed 0");
									SoldierRobot.mRadio.writeChannel(RadioChannels.MEDBAY_CLAIMED, 0);
									SoldierRobot.isMedbay = false;
								}
							}
						}						
						else if ( NUM_GENERATORSUPPLIER_PER_ARTILLERY != 999 && ((generatorCount == 0 && supplierCount  >= RATIO_OF_SUPPLIERS_OVER_GENERATORS)
								|| (double)(numArmy)/(generatorCount+1) > RATIO_ARMY_GENERATOR) ) {
							SoldierRobot.mCensusRespondChannel = RadioChannels.CENSUS_START + NUM_SOLDIERTYPES;
							SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS, (int)(generatorCount+1));
							SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
									+ SoldierRobot.mClaimedEncampmentChannel 
									- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_CAPTURE_STARTED);
							if ( mRC.getTeamPower() > mRC.senseCaptureCost() ) {
								mRC.captureEncampment(RobotType.GENERATOR);		
								waiting = false;
							}
							else {
								SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
										+ SoldierRobot.mClaimedEncampmentChannel 
										- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
								SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS, (int)(generatorCount));
							}
						}
						//998 is kinda lazy, probably should fix this at some point
						else if ( NUM_GENERATORSUPPLIER_PER_ARTILLERY >= 998 
								|| (supplierCount+generatorCount == NUM_GENERATORSUPPLIER_PER_ARTILLERY && artilleryCount == 0) || 
								((supplierCount+generatorCount)/(artilleryCount+1)) > NUM_GENERATORSUPPLIER_PER_ARTILLERY ) {
							SoldierRobot.mCensusRespondChannel = RadioChannels.CENSUS_START +  NUM_SOLDIERTYPES 
									+ NUM_OF_CENSUS_GENERATORTYPES  + NUM_OF_CENSUS_SUPPLIERTYPES;									
							SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS, ((int)(artilleryCount+1)));							
							SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
									+ SoldierRobot.mClaimedEncampmentChannel 
									- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_CAPTURE_STARTED);
							if ( mRC.getTeamPower() > mRC.senseCaptureCost()) {
								mRC.captureEncampment(RobotType.ARTILLERY);
								waiting = false;								
							}
							else {
								SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
										+ SoldierRobot.mClaimedEncampmentChannel 
										- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
								SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS, (int)(artilleryCount));
							}
						}						
						else {
							SoldierRobot.mCensusRespondChannel = RadioChannels.CENSUS_START +  NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES;									
							SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS, (int)(supplierCount+1));							
							SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
									+ SoldierRobot.mClaimedEncampmentChannel 
									- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_CAPTURE_STARTED);
							if ( mRC.getTeamPower() > mRC.senseCaptureCost()) {
								mRC.captureEncampment(RobotType.SUPPLIER);
								waiting = false;								
							}
							else {
								SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
										+ SoldierRobot.mClaimedEncampmentChannel 
										- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
								SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS, (int)(supplierCount));
							}
						}
						/*
						else if (((supplierCount)/(generatorCount+1) <= RATIO_OF_SUPPLIERS_OVER_GENERATORS) ) {
							SoldierRobot.mCensusRespondChannel = RadioChannels.CENSUS_START +  NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES;									
							SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS, (int)(supplierCount+1));							
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
								SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS, (int)(supplierCount));
							}
						}
						else {							
							SoldierRobot.mCensusRespondChannel = RadioChannels.CENSUS_START + NUM_SOLDIERTYPES;
							SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS, (int)(generatorCount+1));
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
								SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS, (int)(generatorCount));
							}							
						}
						*/			
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
				else {
					waiting = true;
				}

			}

			return;
		}
	
	/*
		else{
			if (!checkForEnemies() && SoldierRobot.getDest().equals(mRC.getLocation())) {
				SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
						+ SoldierRobot.mClaimedEncampmentChannel 
						- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_CAPTURE_STARTED);
				if ( mRC.getTeamPower() > mRC.senseCaptureCost() ) {
					mRC.captureEncampment(RobotType.ARTILLERY);								
				}
				else {
					SoldierRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START
							+ SoldierRobot.mClaimedEncampmentChannel 
							- RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
				}
			}
		}
			
		*/
		/*
		int dist = Math.max(Math.abs(SoldierRobot.getDest().x - mRC.getLocation().x),
				Math.abs(SoldierRobot.getDest().y - mRC.getLocation().y)) - 1;
		if(mRC.getEnergon() > GameConstants.MINE_DAMAGE * dist + 1) {
			Direction dir = mRC.getLocation().directionTo(SoldierRobot.getDest());
			if(dir.ordinal() < NUM_DIR && mRC.canMove(dir)) {
				//mRC.move(dir);
				//return;
			}
			else {
				startRound -= 1;
			}
		}
		*/
		
		if(startRound == -1) {
			startRound = Clock.getRoundNum();
		}
		if(Clock.getRoundNum() - startRound > GOTO_ENCAMPMENT_MAX_ROUNDS) {
			SoldierRobot.switchType(SoldierType.ARMY);
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return;
		}
		//sense the destination square
		if(mRC.canSenseSquare(SoldierRobot.getDest())){
			GameObject o = mRC.senseObjectAtLocation(SoldierRobot.getDest());
			//if it's a robot
			if(o != null && o.getClass() == Robot.class) {
				//and it's not a soldier, then:
				if(mRC.senseRobotInfo((Robot)o).type != RobotType.SOLDIER) {
					//become an army soldier, and rally
					SoldierRobot.switchType(SoldierType.ARMY);
					SoldierRobot.switchState(SoldierState.GOTO_RALLY);
					return;
				}
				else
				{
					//if it's a rally bot, tell it to become an encampment
					SoldierRobot.mRadio.writeChannel(RadioChannels.BECOME_ENCAMPMENT,
							((SoldierRobot.getDest().x+SoldierRobot.getDest().y*mRC.getMapWidth()) |1879048192));
					
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
			SoldierRobot.mRadio.writeChannel(SoldierRobot.mClaimedEncampmentChannel, 0);			
			SoldierRobot.switchType(SoldierType.ARMY);
			SoldierRobot.switchState(SoldierState.GOTO_RALLY);
			return true;			
		}
		return false;
	}

}
