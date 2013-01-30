package BaseBot.Robots.Types;

import BaseBot.Robots.ARobot;
import BaseBot.Robots.HQRobot;
import BaseBot.Robots.SoldierRobot;
import BaseBot.Robots.HQRobot.HQState;
import BaseBot.Robots.SoldierRobot.SoldierType;
import BaseBot.Util.Constants;
import BaseBot.Util.NonConstants;
import BaseBot.Util.RadioChannels;
import battlecode.common.*;
import static BaseBot.Robots.ARobot.mRC;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.EconConstants.*;
import static BaseBot.Util.NonConstants.*;
import static BaseBot.Util.Util.*;
public class HQNormalType {
	
	
	private static int minerCount = 0;
	private static int scoutCount = 0;
	private static int armyCount = 0;
	private static int pointCount = 0;
	private static int generatorCount = 0;
	private static int supplierCount = 0;
	private static int artilleryCount = 0;
	private static int suicideScoutCount = 0;
	private static int protectCount = 0;
	private static int protectLeftCount = 0;
	private static int protectRightCount = 0;
	private static boolean spawnedNukeScouts = false;
	
	private static int scoutedEncampmentSoldierCount = 0;
	private static int scoutedSoldierCount = 0;
	private static int scoutedGeneratorCount = 0;
	private static int scoutedSupplierCount = 0;
	private static int scoutedArtilleryCount = 0;
	private static boolean scoutCouldDieNextTurn = false;
	private static boolean enemyHasArtillery = false;
	private static int numTurnNoScoutResponse = -1;
	private static boolean scoutJustDie = false;
	
	private static int numRoundsSinceBuiltSuicide = 0;

	private static int surroundingEnemyBots =0;
	private static double lastPower = 0;
	private static MapLocation[] waypointsToEnemyHQ;
	private static int lastNextWaypointIndex;
	private static boolean HQInDanger = false;
	private static MapLocation encampmentInDanger;
	private static int rushStartRound;
	
	private static boolean isSmallMap = false;
	
	private static int numEncWaiting = 0;
	private static SoldierType[] soldierTypes = new SoldierType[MAX_POSSIBLE_SOLDIERS];
	private static MapLocation[] waypointsToShields;
	private static boolean enemyNukeSoonTimedOut = false;
	private static int enemyNukeSoonStart = -1;

	public static void run() throws GameActionException
	{
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		actionAllState(alliedRobots);
		//print("end state hQ: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum());

		switch(HQRobot.getState())
		{
		case TURTLE: {
			turtleState();
			break;
		}
		case PREPARE_ATTACK: {
			prepareAttackState();
			break;
		}
		case ATTACK: {
			attackHQState();
			break;
		}
		case RUSH: {
			rushHQState();
			break;
		}
		default:
			break;			
		}
		//print("end state hQ: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum() + "state: " + HQRobot.getState().toString());
	}
	
	public static void setConstants() throws GameActionException{
		CHANCE_OF_DEFUSING_ENEMY_MINE = CHANCE_OF_DEFUSING_ENEMY_MINE_CONST;
		CHANCE_OF_DEFUSING_NEUTRAL_MINE =CHANCE_OF_DEFUSING_NEUTRAL_MINE_CONST;

		LAST_ROUND_SHOT_DELAY = LAST_ROUND_SHOT_DELAY_CONST;

		SOLDIER_ENEMY_CHECK_RAD = SOLDIER_ENEMY_CHECK_RAD_CONST;
		
		SOLDIER_RALLY_RAD = 		SOLDIER_RALLY_RAD_CONST;
		
		SOLDIER_OUTNUMBER_MULTIPLIER =SOLDIER_OUTNUMBER_MULTIPLIER_CONST;
		SOLDIER_RUN_HEALTH = SOLDIER_RUN_HEALTH_CONST;
		SOLDIER_RUN_EVENTUALLY_HEALTH = SOLDIER_RUN_EVENTUALLY_HEALTH_CONST;
		SOLDIER_RETURN_HEALTH = SOLDIER_RETURN_HEALTH_CONST;
		SOLDIER_BATTLE_ENEMY_CHECK_RAD = SOLDIER_BATTLE_ENEMY_CHECK_RAD_CONST;

		RATIO_OF_SUPPLIERS_OVER_GENERATORS = RATIO_OF_SUPPLIERS_OVER_GENERATORS_CONST;

		SOLDIER_ATTACK_RAD = SOLDIER_ATTACK_RAD_CONST;
		SOLDIER_JOIN_ATTACK_RAD = SOLDIER_JOIN_ATTACK_RAD_CONST;

		SCOUT_RAD_SQUARED = SCOUT_RAD_SQUARED_CONST;
		SCOUT_DIST = SCOUT_DIST_CONST;
		NUM_GENERATORSUPPLIER_PER_ARTILLERY = NUM_GENERATORSUPPLIER_PER_ARTILLERY_CONST;
		
		
		SCOUT_RECOMPUTE_PATH_INTERVAL = SCOUT_RECOMPUTE_PATH_INTERVAL_CONST;
		
		RATIO_ARMY_GENERATOR = RATIO_ARMY_GENERATOR_CONST;
		
		LATEST_EARLY_ATTACK_ROUND = LATEST_EARLY_ATTACK_ROUND_CONST;
		
		SOLDIER_BATTLE_DISENGAGE_RAD = (int) (Map_Width*0.01*Map_Width + Map_Height*0.01*Map_Height); //0.1 squared is 0.01
		
	}
	//Set where we shouldn't build to ensure a spawn path
	private static void setUnusableEncampments() throws GameActionException
	{
		//distance out from the HQ in squares
		int dist =1;
		//Here begins code to make sure we never trap our HQ in encampments.
		//just a bool that keeps our loop running. i'll figure it out.
		boolean safety =true;
		//just a double check that the we've moved two rings out since we last found a blockade
		boolean safetySafety=true;
		//map width and height, used for logic checks about our square size
		int width = Map_Width;
		int height = Map_Height;
		//count how many writes we've made
		int writeCount = 0;
		//How many encampments are there inside this square?
		int squareCount = 0;
		//where do we want to be able to move to?
		MapLocation desiredLocation = HQRobot.mLocation.add(HQRobot.mLocation.directionTo(mRC.senseEnemyHQLocation()));
		//this is a very specialized case,
		/*
		 * if you have mines around your base in this case and your enemy is in a diagonal direction
		 * o#o
		 * #A#
		 * o#o
		 * then it's best to just set one of the encampments as unusable
		 */
		if(mRC.senseMine(desiredLocation) == Team.NEUTRAL){
			
			if(minesInAllDiagonals() && encampmentsInAllOrthogonallyAdjacent()){
				MapLocation badEncampmentLocation = HQRobot.mLocation.add(HQRobot.mLocation.directionTo(mRC.senseEnemyHQLocation()).rotateRight());
				writeCount++;				
				HQRobot.mRadio.writeChannel(RadioChannels.NUM_BAD_ENCAMPMENTS + writeCount, 
						locationToIndex(badEncampmentLocation) ^ FIRST_BYTE_KEY);
				//TODO: ASK ZACH ABOUT THIS
			}
		}
		
		while(safety)
		{
			//number of squares per side of our current square?
			int numSquaresPerSide = dist *2 + 1;
			//distance Squared to a corner?
			int distSquared = 2*dist*dist;
			//perimeter squares?
			int perimeterSquares = (numSquaresPerSide*4-4);
			
			//ARE we on an edge?
			//if we're on an edge in the x direction, cut out one side
			if(HQRobot.mLocation.x < dist || HQRobot.mLocation.x > width -dist)
			{
				//if we're on both edges, cut out two sides
				if(HQRobot.mLocation.y < dist || HQRobot.mLocation.y > height -dist)
				{
					perimeterSquares -= 2 * numSquaresPerSide-1;
				}
				//else just one
				else
				{
					perimeterSquares -= numSquaresPerSide;	
				}
			}
			//if we're on just an edge in the y direction, cut out one side
			else if(HQRobot.mLocation.y < dist || HQRobot.mLocation.y > height -dist)
			{
				perimeterSquares -= numSquaresPerSide;
			}
			//otherwise cut the number down by two, just to be safe
			else
			{
				perimeterSquares -=2;
			}
			//how many squares are there within this round's radius?
			int encampmentSquaresThisRound =mRC.senseEncampmentSquares(HQRobot.mLocation, distSquared, null).length;
			
			//if we have more encampmentsquares than normal squares -2, check our desired square.
	 		if(encampmentSquaresThisRound-squareCount >= perimeterSquares)
	 		{
	 			if(mRC.senseEncampmentSquare(desiredLocation))
	 			{
	 				//increment our squares we protected counter
	 				squareCount++;
	 				//write to a channel
	 				writeCount++;
	 				HQRobot.mRadio.writeChannel(RadioChannels.NUM_BAD_ENCAMPMENTS + writeCount, locationToIndex(desiredLocation) ^ FIRST_BYTE_KEY);
	 				desiredLocation = desiredLocation.add(desiredLocation.directionTo(mRC.senseEnemyHQLocation()));
	 				safetySafety = true;
	 				
	 			}
	 			//we've only been safe for one round of checking
	 			else if(safetySafety)
	 			{
	 				desiredLocation = desiredLocation.add(desiredLocation.directionTo(mRC.senseEnemyHQLocation()));
	 				safetySafety = false;
	 			}
	 			//We checked two rounds out, I feel pretty secure in this.
	 			else
	 			{
	 				safety = false;
	 			}
	 		}
	 		else if(safetySafety && mRC.senseEncampmentSquare(desiredLocation))
 			{
 				//increment our squares we protected counter
 				squareCount++;
 				//write to a channel
 				writeCount++;
 				HQRobot.mRadio.writeChannel(RadioChannels.NUM_BAD_ENCAMPMENTS + writeCount, locationToIndex(desiredLocation) ^ FIRST_BYTE_KEY);
 				desiredLocation = desiredLocation.add(desiredLocation.directionTo(mRC.senseEnemyHQLocation()));
 				safetySafety = true;
 				
 			}
	 		//we've only been safe for one round of checking
 			else if(safetySafety)
 			{
 				desiredLocation = desiredLocation.add(desiredLocation.directionTo(mRC.senseEnemyHQLocation()));
 				safetySafety = false;
 			}
 			//We checked two rounds out, I feel pretty secure in this.
 			else
 			{
 				safety = false;
 			}
	 		//increment our distance to one more square out.
	 		dist++;
	 		//keep track of how many encampment squares we've already seen.
	 		squareCount += encampmentSquaresThisRound;
		}
		HQRobot.mRadio.writeChannel(RadioChannels.NUM_BAD_ENCAMPMENTS, writeCount ^ FIRST_BYTE_KEY);
			
	}
	
	public static boolean minesInAllDiagonals(){
		if(mRC.senseMine(HQRobot.mLocation.add(Direction.NORTH_WEST)) == Team.NEUTRAL
				&& mRC.senseMine(HQRobot.mLocation.add(Direction.NORTH_EAST)) == Team.NEUTRAL
				&& mRC.senseMine(HQRobot.mLocation.add(Direction.SOUTH_WEST)) == Team.NEUTRAL
				&& mRC.senseMine(HQRobot.mLocation.add(Direction.SOUTH_EAST)) == Team.NEUTRAL){
			return true;
		}
		return false;
	}
	public static boolean encampmentsInAllOrthogonallyAdjacent(){
		if(mRC.senseEncampmentSquare(HQRobot.mLocation.add(Direction.NORTH))
				&& mRC.senseEncampmentSquare(HQRobot.mLocation.add(Direction.EAST))
				&& mRC.senseEncampmentSquare(HQRobot.mLocation.add(Direction.WEST))
				&& mRC.senseEncampmentSquare(HQRobot.mLocation.add(Direction.SOUTH))){
			return true;
		}
		return false;
	}
	
	
	private static void initializeRadioChannels() throws GameActionException {
		setConstants();
		setUnusableEncampments();
		setNumberOfPreFusionEnc();
		setMapWidthAndHeight();
	}
	
	private static void performCensus() throws GameActionException {
		//Perform census
		if(Clock.getRoundNum()%CENSUS_INTERVAL == 0) {
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.LAY_MINES.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.SCOUT.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.ARMY.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.PROTECT_LEFT_ENCAMPMENT.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.PROTECT_RIGHT_ENCAMPMENT.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES,0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES,0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES 
					+ NUM_OF_CENSUS_GENERATORTYPES + NUM_OF_CENSUS_GENERATORTYPES,0);
			HQRobot.mRadio.writeChannel(RadioChannels.ENC_SOLDIER_WAITING, 0 |FIRST_BYTE_KEY);
			
		}
		
		if (Clock.getRoundNum() == 0) {
			initializeRadioChannels();
			int diffX = Math.abs(mRC.getLocation().x - HQRobot.enemyHQLoc.x);
			int diffY = Math.abs(mRC.getLocation().y - HQRobot.enemyHQLoc.y);
			isSmallMap = Math.max(diffX, diffY) < SMALL_MAP_DIST;
			if (isSmallMap ) {
				HQRobot.mRadio.writeChannel(RadioChannels.NUM_ARTILERY_SMALL_MAP, NUM_EARLY_ARTILLRY_SMALL_MAP);
			}
			else {
				HQRobot.mRadio.writeChannel(RadioChannels.NUM_ARTILERY_SMALL_MAP, 0);
			}
		}
		else if(Clock.getRoundNum()%CENSUS_INTERVAL == 1){
			minerCount  = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.LAY_MINES.ordinal());
			// Don't respawn scouts unless we have vision
			if(Clock.getRoundNum() > 500) {
				scoutCount  = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.SCOUT.ordinal());
			}
			armyCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.ARMY.ordinal());
			pointCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START+SoldierType.ARMYPOINT.ordinal());
			protectLeftCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START+SoldierType.PROTECT_LEFT_ENCAMPMENT.ordinal());
			protectRightCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START+SoldierType.PROTECT_RIGHT_ENCAMPMENT.ordinal());
			protectCount = protectLeftCount + protectRightCount;
			generatorCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES);
			
			supplierCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES);
			artilleryCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES 
					+ NUM_OF_CENSUS_GENERATORTYPES + NUM_OF_CENSUS_SUPPLIERTYPES);
			
			numEncWaiting = FIRST_BYTE_KEY ^ HQRobot.mRadio.readChannel(RadioChannels.ENC_SOLDIER_WAITING);
		
			HQRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS,generatorCount);
			HQRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS,supplierCount);
			HQRobot.mRadio.writeChannel(RadioChannels.NUM_ARTILLERY,artilleryCount);
		}
		else if ( Clock.getRoundNum() % CENSUS_INTERVAL ==2 ) {

			 HQRobot.mRadio.writeChannel(RadioChannels.CLAIM_LOCATION_START,0^FIRST_BYTE_KEY);
			 checkForMedbay();
			 checkForShields();
			 checkForSecondMedbay();
		}
	}
	
	private static void updateEnemyLocationData() throws GameActionException {
		//Sense Enemy robots and broadcast average position to bots
		Robot[] enemyRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mEnemy);		
		int avgX = 0, avgY = 0, numSoldiers = 0;
		for(int i=enemyRobots.length;--i>=0;){
			RobotInfo info = mRC.senseRobotInfo(enemyRobots[i]);
			if(info.type == RobotType.SOLDIER){
				numSoldiers ++;
				avgX += info.location.x;
				avgY += info.location.y;
			}
		}

		if ( numSoldiers > 0) {
			avgX /= numSoldiers;
			avgY /= numSoldiers;
			
			if ( HQRobot.enemyLastSeenPosAvg == null) {
				HQRobot.enemyLastSeenPosAvg = new MapLocation(avgX,avgY);				
			}
			else {
				int oldX = HQRobot.enemyLastSeenPosAvg.x;
				int oldY = HQRobot.enemyLastSeenPosAvg.y;
				HQRobot.enemyLastSeenPosAvg = new MapLocation((int)((avgX*AVG_POSITION_RECENT_WEIGHT + oldX)/(1f+AVG_POSITION_RECENT_WEIGHT)), 
															  (int)((avgY*AVG_POSITION_RECENT_WEIGHT + oldY)/(1f+AVG_POSITION_RECENT_WEIGHT)));
			}
		}
		else {
			avgX = HQRobot.enemyHQLoc.x;
			avgY = HQRobot.enemyHQLoc.y;
			//Turn towards enemy HQ if we haven't seen enemies this turn
			if(HQRobot.enemyLastSeenPosAvg!=null)
			{
				int oldX = HQRobot.enemyLastSeenPosAvg.x;
				int oldY = HQRobot.enemyLastSeenPosAvg.y;
			
				HQRobot.enemyLastSeenPosAvg = new MapLocation((int)((avgX*AVG_POSITION_RECENT_WEIGHT + oldX)/(1f+AVG_POSITION_RECENT_WEIGHT)), 
						(int)((avgY*AVG_POSITION_RECENT_WEIGHT + oldY)/(1f+AVG_POSITION_RECENT_WEIGHT)));;
			}
		}
		
		//Write the average enemy location to be used by battling units
		HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_AVG_POS, locationToIndex(new MapLocation(avgX,avgY)));
		if (HQRobot.enemyLastSeenPosAvg != null) {
			HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_LOCATION, locationToIndex(HQRobot.enemyLastSeenPosAvg));
		}
		else {
			HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_LOCATION, locationToIndex(HQRobot.enemyHQLoc));
		}
	}
	
	private static void updateScoutWayPoints() throws GameActionException {
		// Check for waypoints from our scout
		int numScoutWaypoints = HQRobot.mRadio.readChannel(RadioChannels.NUM_SCOUT_WAYPOINTS);
		if(numScoutWaypoints > 0){			
			waypointsToEnemyHQ = new MapLocation[numScoutWaypoints];			
			//print(numScoutWaypoints);
			if ( numScoutWaypoints < 100 ) {
				for(int n=0; n<numScoutWaypoints; ++n) {
					waypointsToEnemyHQ[n] = indexToLocation(HQRobot.mRadio.readChannel(RadioChannels.SCOUT_WAYPOINTS_START + n));
				}
			}
			HQRobot.mRadio.writeChannel(RadioChannels.NUM_SCOUT_WAYPOINTS, 0);
		}
	}
	
	
	private static void actionAllState(Robot[] allies) throws GameActionException {
		//print("start Action All state: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum());
		//Updates the number of each unit we have 		
		performCensus(); 
		//print("census: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum());
		//Broadcasts enemy position data to army
		updateEnemyLocationData();
		//print("enemyloc: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum());
		//Updates waypoints for scouts
		updateScoutWayPoints(); 
		//print("scouts: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum());
		//Check if the medbay is alive
		checkForMedbay();
		//Check for the rest of the encampments
		//print("medbay: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum());
		
		if ( Clock.getRoundNum() % CHECK_ENCAMPMENT_ROUND_DELAY == 0 ) {
			checkAllEncampments();
		}
		
		//print("end encampments: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum());
		//check if THE HQ is threatened
		checkHQSafety();
		//Check if an encampment is threatened
		checkEncampmentSafety();
		//Check if we're being nuked
		checkEnemyNuking();
		//Check if we should rush the enemy HQ
		checkShouldRush();
		//Check if we spawned a new unit
		checkNewUnitType();
		//Decay the distance from the enemy HQ that we're expecting mines
		updateEnemyMineRadius();
		
		if ( suicideScoutCount > 0 )
		{
			numRoundsSinceBuiltSuicide++;
		}
		
		//check
		if ( numTurnNoScoutResponse == 0 && numRoundsSinceBuiltSuicide > 10) {
			boolean newInfo = checkScoutState();
			if (newInfo) {
				if ( Clock.getRoundNum() < 300
						&& (enemyHasArtillery || scoutedArtilleryCount > 0)
						&& !enemyNukeSoonTimedOut ) {
					HQRobot.enemyNukeSoon = true;
					enemyNukeSoonStart = Clock.getRoundNum();
					spawnNukeScouts();
					print("we think nuke");
					mRC.setIndicatorString(0, "we think nuke because of artillery");
					HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_FASTER_NUKE, 1);
				}
			}
			//commented now to prevent false alarms
			/*
			if ( mRC.senseMineLocations(HQRobot.enemyHQLoc, 300, HQRobot.mEnemy).length > 1) {			
				HQRobot.enemyNukeSoon = true;
				print("we think mines nuke");
				HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_FASTER_NUKE, 1);
			}
			*/
		}
		if ( numTurnNoScoutResponse == 1 && !scoutJustDie) {
			scoutJustDie = true;
		}
		else if ( numTurnNoScoutResponse ==1 && !scoutJustDie ) {
			scoutJustDie = false; 
			numTurnNoScoutResponse = 2;
		}
				
		
		//print("end Action All state: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum());
		
		//TODO: comment why sometimes these return and some don't
		if(mRC.isActive()){
			if(mRC.checkResearchProgress(Upgrade.NUKE) > Upgrade.NUKE.numRounds - RUSH_NUKE_TIME) {
				// We're almost done with the nuke!
				mRC.researchUpgrade(Upgrade.NUKE);
				mRC.setIndicatorString(2, "Nuke almost done!, get ready to wear hats!!!");
				return;
			}

			if(numEncToClaim > 0 && Clock.getRoundNum() < 10){
				HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);				
				return;
			}
			
			if(mRC.getTeamPower() < PREFUSION_POWER_RESERVE 
					&& ((mRC.senseNearbyGameObjects(Robot.class, (mRC.getLocation().distanceSquaredTo(mRC.senseEnemyHQLocation()))/2, HQRobot.mEnemy).length == 0)
					|| mRC.hasUpgrade(Upgrade.FUSION))){
				mRC.setIndicatorString(0, "researching: " + "round " + Clock.getRoundNum());
				pickResearch();
				return;
			}
			
			if(HQRobot.enemyNukeSoon) {
				pickActionBeingNuked();
				return;
			}
			else {
				pickActionNotBeingNuked();
				return;
			}
			
		}
		
		lastPower  = mRC.getTeamPower();
		
	}
	
	private static void updateEnemyMineRadius() throws GameActionException {
		int oldRadius = HQRobot.mRadio.readChannel(RadioChannels.ENEMY_MINE_RADIUS);
		if((oldRadius & FIRST_BYTE_KEY_MASK) != FIRST_BYTE_KEY) {
			oldRadius = 0;
		}
		else {
			oldRadius ^= FIRST_BYTE_KEY;
		}
		if(oldRadius > 0 && ARobot.rand.nextFloat() < 0.1)
			--oldRadius;
		HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_MINE_RADIUS, oldRadius | FIRST_BYTE_KEY);
	}
	
	private static void pickActionNotBeingNuked() throws GameActionException {
		int tempMax = RadioChannels.ENC_CLAIM_START + Math.min(numEncToClaim, NUM_PREFUSION_ENC);
		for (int i = RadioChannels.ENC_CLAIM_START;
				i < tempMax; i++) {
			if ((HQRobot.mRadio.readChannel(i)) == -1) {
				HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
				return;
			}
		}			
		
		if((minerCount + armyCount) < NUM_MINERS) { 
			++ minerCount;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.LAY_MINES);
			return;
		}
		if (armyCount < NUM_ARMY_BEFORE_SCOUTS ) {
			++armyCount;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
			HQRobot.mRadio.writeChannel(RadioChannels.POINT_SCOUT_TYPE, 0);
			return;
		}		
		if ( suicideScoutCount < 1 &&!isSmallMap) {
			++suicideScoutCount;
			numTurnNoScoutResponse = 0;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.SUICIDE);
			return;
		}
		
		//TODO: make this conditional on how many enemies we see
		if (HQRobot.lastBuiltWasEncampment >= NUM_ARMY_PER_ENC_PRE_FUSION && numEncWaiting < MAX_WAITING_ENC) {
			tempMax = RadioChannels.ENC_CLAIM_START + HQRobot.maxEncChannel + BUFFER_ENC_CHANNEL_CHECK;
			for (int i = RadioChannels.ENC_CLAIM_START;
					i <tempMax; i++) {
				if (HQRobot.mRadio.readChannel(i) == 0) { 
					HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);	

					return;
				}
			}
		}
		if(armyCount < NUM_ARMY_NO_FUSION) {
			++ armyCount;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
			HQRobot.mRadio.writeChannel(RadioChannels.POINT_SCOUT_TYPE, 0);
			return;
		}
		if(scoutCount < NUM_SCOUTS &&!isSmallMap) {
			++ scoutCount;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.SCOUT);
			return;
		}		
		
		
		if (!mRC.hasUpgrade(Upgrade.FUSION)) {
			mRC.researchUpgrade(Upgrade.FUSION);
			return;
		}
		
		if (mRC.hasUpgrade(Upgrade.PICKAXE) && minerCount < NUM_MINERS_WITH_PICKAXE
				&& mRC.getTeamPower() > POWER_RESERVE/* && mRC.getTeamPower() > lastPower*/) {
			++ minerCount;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.LAY_MINES);
			return;	
		}
		
		if (HQRobot.lastBuiltWasEncampment >= NUM_SOLDIER_BEFORE_ENC && numEncWaiting < MAX_WAITING_ENC) {
			tempMax = RadioChannels.ENC_CLAIM_START + HQRobot.maxEncChannel + BUFFER_ENC_CHANNEL_CHECK;
			for (int i = RadioChannels.ENC_CLAIM_START;
					i <tempMax; i++) {
				if (HQRobot.mRadio.readChannel(i) == 0) { 
					HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);		
					return;
				}
			}
		}
		if(pointCount<NUM_POINT_SCOUTS) {

			++pointCount;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMYPOINT);
			HQRobot.mRadio.writeChannel(RadioChannels.POINT_SCOUT_TYPE, pointCount);
			return;
		}		
		if(protectCount < NUM_PROTECT_ENCAMPMENTS && armyCount > ARMY_COUNT_BEFORE_PROTECT_ENCAMPMENTS){
			if(protectLeftCount == 0){
				HQRobot.spawnRobot(SoldierRobot.SoldierType.PROTECT_LEFT_ENCAMPMENT);
				++protectLeftCount;
			}
			else{
				HQRobot.spawnRobot(SoldierRobot.SoldierType.PROTECT_RIGHT_ENCAMPMENT);
				++protectRightCount;
			}
			++protectCount;
			HQRobot.mRadio.writeChannel(RadioChannels.PROTECT_ENCAMPMENT_TYPE, protectCount);
			
			return;
		}
		
		if(armyCount < NUM_ARMY_WITH_FUSION
				&& mRC.getTeamPower() > POWER_RESERVE/* && mRC.getTeamPower() > lastPower*/) {
			++ armyCount;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
			HQRobot.mRadio.writeChannel(RadioChannels.POINT_SCOUT_TYPE, 0);
			return;
		}
		
		pickResearch();
	}
	
	private static void pickActionBeingNuked() throws GameActionException {
		int tempMax = RadioChannels.ENC_CLAIM_START + Math.min(numEncToClaim, NUM_PREFUSION_ENC);
		for (int i = RadioChannels.ENC_CLAIM_START;
				i < tempMax; i++) {
			if ((HQRobot.mRadio.readChannel(i)) == -1) {
				HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
				return;
			}
		}			
		
		if (!mRC.hasUpgrade(Upgrade.FUSION)) {
			mRC.researchUpgrade(Upgrade.FUSION);
			return;
		}
		if ( !mRC.hasUpgrade(Upgrade.DEFUSION) ) {
			mRC.researchUpgrade(Upgrade.DEFUSION);
			return;
		}
		if ( !mRC.hasUpgrade(Upgrade.VISION)) {
			mRC.researchUpgrade(Upgrade.VISION);
			return;
		}

		if(scoutCount < NUM_SCOUTS) {
			++ scoutCount;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.SCOUT);
			return;
		}
		
		if(armyCount < NUM_ARMY_WITH_FUSION
				&& mRC.getTeamPower() > POWER_RESERVE/* && mRC.getTeamPower() > lastPower*/) {
			++ armyCount;
			HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
			HQRobot.mRadio.writeChannel(RadioChannels.POINT_SCOUT_TYPE, 0);
			return;
		}
		
		pickResearch();
	}
	
	private static void checkEnemyNuking() throws GameActionException {
		if(!HQRobot.enemyNukeSoonNoReally && mRC.checkResearchProgress(Upgrade.NUKE) <= Upgrade.NUKE.numRounds/2 
		           && mRC.senseEnemyNukeHalfDone()) {
					HQRobot.enemyNukeSoon = true;
					HQRobot.enemyNukeSoonNoReally = true;
					spawnNukeScouts();
		}
		if(HQRobot.enemyNukeSoon && !HQRobot.enemyNukeSoonNoReally
				&& Clock.getRoundNum() - enemyNukeSoonStart > 100) {
			HQRobot.enemyNukeSoon = false;
			enemyNukeSoonTimedOut = true;
			HQRobot.switchState(HQState.TURTLE);
		}
		HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_FASTER_NUKE, HQRobot.enemyNukeSoon ? 1 : 0);
	}
	
	private static void spawnNukeScouts() throws GameActionException {
		if ( !spawnedNukeScouts && scoutCount < 2 ) {			
			int message = Clock.getRoundNum() << 2;
			int numExtraScouts = 2 - scoutCount;
			message |= numExtraScouts;
			HQRobot.mRadio.writeChannel(RadioChannels.CHANGE_SCOUT, message);
			scoutCount += numExtraScouts;
		}		
		spawnedNukeScouts = true;
	}
	
	

	private static void checkNewUnitType() throws GameActionException {
		if(Clock.getRoundNum() == 0)
			HQRobot.mRadio.writeChannel(RadioChannels.NEW_UNIT_ID, -1);
		
		int value;
		if((value = HQRobot.mRadio.readChannel(RadioChannels.NEW_UNIT_ID)) != -1) {
			if(value/SoldierType.values().length < MAX_POSSIBLE_SOLDIERS) {
				soldierTypes[value/SoldierType.values().length]
						= SoldierType.values()[value%SoldierType.values().length];
			}
			HQRobot.mRadio.writeChannel(RadioChannels.NEW_UNIT_ID, -1);
		}
	}

	private static void checkShouldRush() {
		if(mRC.senseNearbyGameObjects(Robot.class, mRC.senseEnemyHQLocation(),
				HQ_ENTER_RUSH_RAD, HQRobot.mTeam).length > 1 )
			HQRobot.switchState(HQState.RUSH);
	}

	private static void checkHQSafety() throws GameActionException {
		Robot[] bots = mRC.senseNearbyGameObjects(Robot.class,HQ_PROTECT_RAD_SQUARED,ARobot.mEnemy);
		if(bots.length > 0){
			
			//the only reason this is being written is to change everyone who is not already a soldier to soldier type
			// I check this against hq in danger so that we don't write a hundred times
			if(!HQInDanger && bots.length > surroundingEnemyBots)
			{
				HQRobot.mRadio.writeChannel(RadioChannels.HQ_IN_DANGER, (bots.length+2-surroundingEnemyBots )| FIRST_BYTE_KEY);
			}
			surroundingEnemyBots = bots.length;
			HQInDanger = true;
		}
		else{
			surroundingEnemyBots=0;
			HQInDanger = false;
			HQRobot.mRadio.writeChannel(RadioChannels.HQ_IN_DANGER, FIRST_BYTE_KEY|0);
		}
	}
	
	private static void checkEncampmentSafety() throws GameActionException {
		int value = HQRobot.mRadio.readChannel(RadioChannels.ENCAMPMENT_IN_DANGER);
		if(value != -1) {
			encampmentInDanger = indexToLocation(value);
		}
		else {
			encampmentInDanger = null;
		}
		HQRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_IN_DANGER, -1);
	}

	private static void checkAllEncampments() throws GameActionException {
		int startByteCode = Clock.getBytecodesLeft(); 
		
		MapLocation tempLocation;
		int tempInt;
		int maxChannelFound = RadioChannels.ENC_CLAIM_START;
		//Go through all the encampments that have been claimed and thought to be used		
		
		//If they have been lost, change the channels to signify that
		int i = HQRobot.lastCheckedChannel; 
		int tempMax = RadioChannels.ENC_CLAIM_START + HQRobot.maxEncChannel + BUFFER_ENC_CHANNEL_CHECK;
		//print(i);
        for ( ; i < tempMax; i++ ) {
        	if ((tempInt = HQRobot.mRadio.readChannel(i)) != 0) {
        		tempLocation = indexToLocation(tempInt -1); // subtract 1 b.c it adds 1 when it takes it so we don't need initialization
        		if (!mRC.canSenseSquare(tempLocation) )
        		{
        			//If we can't sense the square, check to see if the tower says it should have been built or not
        			tempInt = HQRobot.mRadio.readChannel(RadioChannels.ENCAMPMENT_BUILDING_START + i - RadioChannels.ENC_CLAIM_START);
        			if ( tempInt == ENCAMPMENT_CAPTURE_STARTED ) {
        				//print ("overwriting channel: " + (RadioChannels.ENCAMPMENT_BUILDING_START + i - RadioChannels.ENC_CLAIM_START));
        				HQRobot.mRadio.writeChannel(i, ENCAMPMENT_NOT_CLAIMED);
        				HQRobot.mRadio.writeChannel(RadioChannels.ENCAMPMENT_BUILDING_START + i - RadioChannels.ENC_CLAIM_START, ENCAMPMENT_NOT_CLAIMED);
        			}
        			else {
        				maxChannelFound = i;
        			}
        		}
        		else {
        			maxChannelFound = i;
        		}
        	}
        	//print(i);
        	if ( startByteCode - Clock.getBytecodesLeft() > MAX_BYTE_CODE_FOR_ENCAMPMENT_CHECK)
        	{
        		HQRobot.lastCheckedChannel = i;
        		return;
        	}
        }
        HQRobot.lastCheckedChannel = RadioChannels.ENC_CLAIM_START;
        //print("setting max channel to: " + (maxChannelFound - RadioChannels.ENC_CLAIM_START));
        HQRobot.maxEncChannel = maxChannelFound - RadioChannels.ENC_CLAIM_START;
        HQRobot.mRadio.writeChannel(RadioChannels.MAX_ENC_CHANNEL_TO_CHECK, HQRobot.maxEncChannel);
    }
	private static void checkForMedbay() throws GameActionException {
		MapLocation medbay = indexToLocation(HQRobot.mRadio.readChannel(RadioChannels.MEDBAY_LOCATION));
		if(mRC.canSenseSquare(medbay)){
			GameObject o = mRC.senseObjectAtLocation(medbay);
			int startRound = HQRobot.mRadio.readChannel(RadioChannels.MEDBAY_CLAIMED);
			if(o != null && o.getTeam() == mRC.getTeam()
					&& (mRC.senseRobotInfo((Robot) o).type == RobotType.MEDBAY || Clock.getRoundNum() - GameConstants.CAPTURE_ROUND_DELAY - 1 < startRound)) {
				return;
			}
		}
		// The medbay value was invalid, replace it with our location
		HQRobot.mRadio.writeChannel(RadioChannels.MEDBAY_LOCATION, locationToIndex(mRC.getLocation()));
		
		//TODO: If we have a ton of encampments this could take a long time
		// If the location wasn't our location, unclaim the encampment so we try to reclaim it
		
		
		if(!medbay.equals(mRC.getLocation())){
			int tempMax = RadioChannels.ENC_CLAIM_START + HQRobot.maxEncChannel + BUFFER_ENC_CHANNEL_CHECK;
			for (int i = RadioChannels.ENC_CLAIM_START;
					i < tempMax; i++) { 
				if (HQRobot.mRadio.readChannel(i) == locationToIndex(medbay)) {
					HQRobot.mRadio.writeChannel(i, -1);
				}
			}		
			HQRobot.mRadio.writeChannel(RadioChannels.MEDBAY_CLAIMED, 0);
		}
		
	}
	private static void checkForShields() throws GameActionException {
		int shieldData = HQRobot.mRadio.readChannel(RadioChannels.SHIELD_LOCATION);
		int startRound = HQRobot.mRadio.readChannel(RadioChannels.SHIELDS_CLAIMED);
		if (shieldData == -2 && Clock.getRoundNum() - GameConstants.CAPTURE_ROUND_DELAY - 1 < startRound) 
		{
			return; 
		}
		MapLocation shields = indexToLocation(shieldData);		
		if(mRC.canSenseSquare(shields)){			
			GameObject o = mRC.senseObjectAtLocation(shields);
			if(o != null && o.getTeam() == mRC.getTeam()
					&& (mRC.senseRobotInfo((Robot) o).type == RobotType.SHIELDS)) {
				return;
			}
		}
		// The shields value was invalid, set to 0 so soldiers don't go there
		HQRobot.mRadio.writeChannel(RadioChannels.SHIELD_LOCATION, 0);
		
	}
	private static void checkForSecondMedbay() throws GameActionException {
		int medbayData = HQRobot.mRadio.readChannel(RadioChannels.SECOND_MEDBAY);		
		int startRound = HQRobot.mRadio.readChannel(RadioChannels.SECOND_MEDBAY_CLAIMED);
		if (Clock.getRoundNum() - GameConstants.CAPTURE_ROUND_DELAY - 1 < startRound) 
		{
			return; 
		}
		MapLocation medbay = indexToLocation(medbayData);		
		if(mRC.canSenseSquare(medbay)){
			GameObject o = mRC.senseObjectAtLocation(medbay);
			if(((o != null && o.getTeam() == mRC.getTeam() && (mRC.senseRobotInfo((Robot) o).type == RobotType.MEDBAY) ))) {
				return;
			}
		}
		// The medbay value was invalid, remove it 
		HQRobot.mRadio.writeChannel(RadioChannels.SECOND_MEDBAY, 0);
		
		
	}

	private static void pickResearch() throws GameActionException {
		if (!mRC.hasUpgrade(Upgrade.FUSION)) {
			mRC.researchUpgrade(Upgrade.FUSION);
		}
		else if ( !mRC.hasUpgrade(Upgrade.DEFUSION) ) {
			mRC.researchUpgrade(Upgrade.DEFUSION);
		}
		else if ( !mRC.hasUpgrade(Upgrade.VISION)) {
			mRC.researchUpgrade(Upgrade.VISION);
		}
		else {
			mRC.researchUpgrade(Upgrade.NUKE);
		}
	}
	
	private static boolean checkScoutState() throws GameActionException {
		int scoutInfo = HQRobot.mRadio.readChannel(RadioChannels.SCOUT_FOUND_NEW);
		boolean returnVal = false;
		if ( scoutInfo != 0 ) {
			if ( (scoutInfo & ENC_SOLDIER_FLAG) != 0 )
			{
				scoutedEncampmentSoldierCount = HQRobot.mRadio.readChannel(RadioChannels.ENEMY_SOLDIER_ON_ENCAMPMENT_COUNT);				
			}
			if ( (scoutInfo & SOLDIER_FLAG) != 0 )
			{
				scoutedSoldierCount = HQRobot.mRadio.readChannel(RadioChannels.ENEMY_SOLDIER_COUNT);
			}
			if ( (scoutInfo & GENERATOR_FLAG) != 0 )
			{
				scoutedGeneratorCount = HQRobot.mRadio.readChannel(RadioChannels.ENEMY_GENERATOR_COUNT);
			}
			if ( (scoutInfo & SUPPLIER_FLAG) != 0 )
			{
				scoutedSupplierCount = HQRobot.mRadio.readChannel(RadioChannels.ENEMY_SUPPLIER_COUNT);
			}
			if ( (scoutInfo & ARTILLERY_FLAG) != 0 )
			{
				scoutedArtilleryCount = HQRobot.mRadio.readChannel(RadioChannels.ENEMY_ARTILLERY_COUNT);				
			}
			if ( (scoutInfo & COULD_DIE_FLAG) != 0 )
			{
				scoutCouldDieNextTurn = true;
			}
			returnVal = true;
		}
		else if ( !scoutCouldDieNextTurn ) {
			enemyHasArtillery = true;
			numTurnNoScoutResponse++;
			returnVal = true;
		}
		else {
			numTurnNoScoutResponse++;
		}
		
		return returnVal;
	}
	
	private static void turtleState() throws GameActionException {
		
		//Commented unless we start using the suicide scout again
		/*
		if ( scoutJustDie ) {
			if ( Clock.getRoundNum() < LATEST_EARLY_ATTACK_ROUND && scoutedArtilleryCount ==0 && scoutedSoldierCount < 6 && scoutedSoldierCount < Clock.getRoundNum()/20) {
				HQRobot.switchState(HQState.ATTACK);
				return;
			}
			
		}
		*/
		
		
		if (!HQInDanger && encampmentInDanger == null) {

			//Get all our encampment squares
			MapLocation encampmentSquares[] = mRC.senseAlliedEncampmentSquares();
			int tempRead = HQRobot.mRadio.readChannel(RadioChannels.CLAIM_LOCATION_START);
			MapLocation tempLoc;
				
			
			if(encampmentSquares.length>0){
				//store the furthest distance from our base
				int distSquared =0;
				//store our encampment closest to enemy base (give it default value)
				int leastDist= encampmentSquares[0].distanceSquaredTo(HQRobot.enemyHQLoc);
				//loop through each encampment. if its distance is shorter than current least dist, replace it
				for(int i = encampmentSquares.length;--i>=0;)
				{ 
					int temp = encampmentSquares[i].distanceSquaredTo(HQRobot.enemyHQLoc);
					if( temp<leastDist)
					{
						leastDist = temp;
						//store the location of the furthest encampment
						distSquared = i;

					}
				}
				boolean inProgress = false;
				if((tempRead & FIRST_BYTE_KEY_MASK) == FIRST_BYTE_KEY)
				{
					tempRead = tempRead ^ FIRST_BYTE_KEY;
					for(int q =1;q<=tempRead;q++)
					{
						tempLoc = indexToLocation((HQRobot.mRadio.readChannel(RadioChannels.CLAIM_LOCATION_START+q)));
							int temp = tempLoc.distanceSquaredTo(HQRobot.enemyHQLoc);
						if( temp<=leastDist)
						{
							leastDist = temp;
							//store the location of the furthest encampment
							encampmentSquares[0] = tempLoc;
							inProgress=true;
						}	
					}
						}
				//get distance from us to furthest encampment
				if(inProgress)
				{
					distSquared = (int)(mRC.getLocation().distanceSquaredTo(encampmentSquares[0]));
				}
				else
				{
					distSquared = (int)(mRC.getLocation().distanceSquaredTo(encampmentSquares[distSquared]));
				}

				MapLocation rallyLoc = new MapLocation(
						(6*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/7,
						(6*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/7);
				//move our wall to a point on the line between us and the enemy base.
				//That point should be the as far from us as our farthest encampment
				if(distSquared> rallyLoc.distanceSquaredTo(mRC.getLocation()))
				{//get distance from us to enemy HQ
					int dist = mRC.getLocation().distanceSquaredTo(HQRobot.enemyHQLoc);
					//How far along that vector should we go?
					float move =  (float)Math.sqrt((float)distSquared/dist);
					HQRobot.setRallyPoint(new MapLocation(
							(int)(mRC.getLocation().x +move*(HQRobot.enemyHQLoc.x-mRC.getLocation().x) ),
							(int)(mRC.getLocation().y + move*(HQRobot.enemyHQLoc.y-mRC.getLocation().y))));
				}
				//if that distance is too short, use our old code!
				else
				{
					HQRobot.setRallyPoint(rallyLoc);
				}
			}
			else
			{
				HQRobot.setRallyPoint(new MapLocation(
						(6*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/7,
						(6*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/7));
			}

		}
		else if(HQInDanger) {
			//TODO:change everyone to army type and send them to HQ
			HQRobot.setRallyPoint(mRC.getLocation());
		}
		else {
			//TODO:this should send a few guys to hunt down whoever disturbed the encampment
			 HQRobot.setRallyPoint(encampmentInDanger);
		}
		
		// Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		if ( HQRobot.enemyNukeSoon ) {
			HQRobot.switchState(HQState.PREPARE_ATTACK); 
		}
		
		else if (Clock.getRoundNum() >= ATTACK_ROUND ) {
			print("attack round trigger");
			HQRobot.switchState(HQState.ATTACK);
		}
		else {
			HQRobot.mRadio.writeChannel(RadioChannels.SHOULD_LAY_MINES, 1);
		}
	}

	private static void prepareAttackState() throws GameActionException {
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		HQRobot.mRadio.writeChannel(RadioChannels.SHOULD_LAY_MINES, 0);				
		if(Math.min(armyCount, alliedRobots.length) > NUM_ARMY_BEFORE_ATTACK
				|| (HQRobot.enemyNukeSoonNoReally && Math.min(armyCount, alliedRobots.length) > NUM_ARMY_BEFORE_ATTACK_WITH_NUKE)) {			
			HQRobot.switchState(HQState.ATTACK); //attack!
			return;
		}
		//make sure we spawn shield/medbay 
		if ( !spawnedNukeScouts && HQRobot.mRadio.readChannel(RadioChannels.ENEMY_HAS_ARTILLERY_NORMAL ) ==1 ) {
			spawnNukeScouts();
		}
		MapLocation preAttackRallyLocation = new MapLocation(
				(4*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/5,
				(4*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/5);						
		
		MapLocation avg = findMedianSoldier(alliedRobots, soldierTypes);
		//Update a smoothed position for the median ( used to army to determine when to retreat

		mRC.setIndicatorString(2, avg+"");				
		
		
		if(waypointsToEnemyHQ == null || !HQRobot.enemyNukeSoon ) {		

			HQRobot.setRallyPoint(preAttackRallyLocation);
		
		}
		else{
			//HQRobot.setRallyPoints(waypointsToEnemyHQ);
			
			int shieldLocationVal = SoldierRobot.mRadio.readChannel(RadioChannels.SHIELD_LOCATION);
			if(shieldLocationVal > 0) {
				// Rally to shields
				
				if(waypointsToShields == null) {
					waypointsToShields = convertWaypoints(waypointsToEnemyHQ,
							waypointsToEnemyHQ[0], indexToLocation(shieldLocationVal)); 
				}
				int nextWaypointIndex = findNextWaypointIndex(waypointsToShields, avg);

				if(lastNextWaypointIndex != nextWaypointIndex
						|| HQRobot.getLastState()!=HQRobot.HQState.PREPARE_ATTACK
						|| HQRobot.rand.nextFloat() < 0.1) {
					HQRobot.setRallyPoints(waypointsToShields, nextWaypointIndex+1);
					lastNextWaypointIndex = nextWaypointIndex;
				}
			}
			else {
				// Rally to middle of path to enemy
				
				int nextWaypointIndex = findNextWaypointIndex(waypointsToEnemyHQ, avg);
			
				nextWaypointIndex = Math.min(nextWaypointIndex, waypointsToEnemyHQ.length/2);
				
				if(lastNextWaypointIndex != nextWaypointIndex
						|| HQRobot.getLastState()!=HQRobot.HQState.PREPARE_ATTACK
						|| HQRobot.rand.nextFloat() < 0.1) {
					HQRobot.setRallyPoints(waypointsToEnemyHQ, nextWaypointIndex+1);
					lastNextWaypointIndex = nextWaypointIndex;
				}
			}
		}
		
	}

	private static void attackHQState() throws GameActionException {
		
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		/*
		int avgX = 0, avgY = 0, numSoldiers = 0;
		for(Robot bot:alliedRobots){
			RobotInfo info = mRC.senseRobotInfo(bot);
			if(info.type == RobotType.SOLDIER){
				numSoldiers ++;
				avgX += info.location.x;
				avgY += info.location.y;
			}
		}
		avgX /= numSoldiers;
		avgY /= numSoldiers;
		*/
		MapLocation avg = findMedianSoldier(alliedRobots, soldierTypes);
		mRC.setIndicatorString(2, avg+"");
		
		if((Math.min(armyCount, alliedRobots.length) < NUM_ARMY_BEFORE_RETREAT
				&& (!HQRobot.enemyNukeSoonNoReally))) {
			HQRobot.switchState(HQState.PREPARE_ATTACK);
		}

		if(waypointsToEnemyHQ == null)
			HQRobot.setRallyPoint(mRC.senseEnemyHQLocation());
		else{
			//HQRobot.setRallyPoints(waypointsToEnemyHQ);
			int nextWaypointIndex = findNextWaypointIndex(waypointsToEnemyHQ, avg);
			if(HQRobot.enemyNukeSoon){
				/*for(int n=nextWaypointIndex; n < waypointsToEnemyHQ.length - 1; ++n) {
					if(Clock.getBytecodesLeft() < 1000)
						break;
					if(mRC.senseNearbyGameObjects(Robot.class, waypointsToEnemyHQ[n],
							32, HQRobot.mTeam).length >= NUM_ARMY_BEFORE_ATTACK_WITH_NUKE) {
						nextWaypointIndex = n + 1;
					}
				}*/
				nextWaypointIndex = waypointsToEnemyHQ.length - 1;
			}
			if(lastNextWaypointIndex != nextWaypointIndex
					|| HQRobot.getLastState()!=HQRobot.HQState.ATTACK
					|| HQRobot.rand.nextFloat() < 0.1) {
				HQRobot.setRallyPoints(waypointsToEnemyHQ, nextWaypointIndex+1);
				lastNextWaypointIndex = nextWaypointIndex;
			}
			//HQRobot.setRallyPoints(waypointsToEnemyHQ);
			//mRC.setIndicatorString(2, findNextWaypoint(waypointsToEnemyHQ, new MapLocation(avgX, avgY)).toString());
		}
		
		HQRobot.mRadio.writeChannel(RadioChannels.SHOULD_LAY_MINES, 0);

	}
	
	private static void rushHQState() throws GameActionException {
		if(HQRobot.getLastState() != HQState.RUSH) {
			rushStartRound = Clock.getRoundNum();
		}
		if(Clock.getRoundNum() - rushStartRound > HQ_RUSH_TIMEOUT) {
			HQRobot.switchState(HQState.PREPARE_ATTACK);
		}
		else if(waypointsToEnemyHQ == null)
			HQRobot.setRallyPoint(mRC.senseEnemyHQLocation());
		else {
			int nextWaypointIndex = waypointsToEnemyHQ.length - 1;
			if(lastNextWaypointIndex != nextWaypointIndex
					|| HQRobot.getLastState()!=HQRobot.HQState.RUSH) {
				HQRobot.setRallyPoints(waypointsToEnemyHQ, nextWaypointIndex+1);
				lastNextWaypointIndex = nextWaypointIndex;
			}
		}
	}
	
}



