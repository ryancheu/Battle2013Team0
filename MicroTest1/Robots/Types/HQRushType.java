package MicroTest1.Robots.Types;

import MicroTest1.Robots.ARobot;
import MicroTest1.Robots.HQRobot;
import MicroTest1.Robots.SoldierRobot;
import MicroTest1.Robots.HQRobot.HQState;
import MicroTest1.Robots.SoldierRobot.SoldierType;
import MicroTest1.Util.RadioChannels;
import battlecode.common.*;
import static MicroTest1.Robots.ARobot.mRC;
import static MicroTest1.Util.Constants.*;
import static MicroTest1.Util.NonConstants.*;
import static MicroTest1.Util.RushConstants.*;
import static MicroTest1.Util.Util.*;
public class HQRushType {
	
	
	private static int minerCount = 0;
	private static int scoutCount = 0;
	private static int armyCount = 0;
	private static int pointCount =0;
	private static int generatorCount = 0;
	private static int supplierCount = 0;
	private static double lastPower = 0;
	private static long turnOfNuke = -1;
	private static MapLocation[] waypointsToEnemyHQ;
	private static int lastNextWaypointIndex;
	private static boolean HQInDanger = false;
	private static MapLocation encampmentInDanger;
	private static int rushStartRound;
	private static SoldierType[] soldierTypes = new SoldierType[MAX_POSSIBLE_SOLDIERS];

	public static void run() throws GameActionException
	{
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		actionAllState(alliedRobots);

		switch(HQRobot.getState())
		{
		case TURTLE: {
			turtleState();
			break;
		}
		case PREPARE_ATTACK: {
			attackHQState();
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
	}
	private static void setAllTeamMemory() throws GameActionException{
		if(Clock.getRoundNum() == 0){
			mRC.setTeamMemory(HOW_WE_PLAYED_MEMORY, RUSH_TYPE);
		}
		if(mRC.senseEnemyNukeHalfDone() && turnOfNuke == -1){
			turnOfNuke = Clock.getRoundNum()-Upgrade.NUKE.numRounds/2;
		}
		
		if(mRC.getEnergon()<=1 && Clock.getRoundNum()>2000){
			mRC.setTeamMemory(ROUND_NUM_MEMORY,Clock.getRoundNum());
			mRC.setTeamMemory(HOW_ENDED_MEMORY, TIEBREAKERS);
		}
		else if(mRC.getEnergon()>48 && Clock.getRoundNum()>=400){
			//48 is the amount of health damage 8 guys surrounding your HQ does
			mRC.setTeamMemory(0,turnOfNuke);
			MapLocation enemyHQ = mRC.senseEnemyHQLocation();
			if(mRC.canSenseSquare(enemyHQ) 
					&& mRC.senseRobotInfo((Robot)mRC.senseObjectAtLocation(enemyHQ)).energon <= 48){
				mRC.setTeamMemory(HOW_ENDED_MEMORY, WE_KILLED);
				// We killed them
			}
			else if(mRC.checkResearchProgress(Upgrade.NUKE) < 399) {
				// Died to nuke
				mRC.setTeamMemory(HOW_ENDED_MEMORY, ENEMY_NUKED);
			}
			else {
				// We nuked them
				mRC.setTeamMemory(HOW_ENDED_MEMORY, WE_NUKED);
			}
		}
		else if(mRC.getEnergon()<=48 && Clock.getRoundNum() < 400){
			mRC.setTeamMemory(ROUND_NUM_MEMORY,Clock.getRoundNum());
			mRC.setTeamMemory(HOW_ENDED_MEMORY, ENEMY_RUSH);
			//died to rush
		}
		else{
			mRC.setTeamMemory(ROUND_NUM_MEMORY,Clock.getRoundNum());
			//died to econ
			mRC.setTeamMemory(HOW_ENDED_MEMORY, ENEMY_ECON);
		}
		
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
		
	}
	private static void initializeRadioChannels() throws GameActionException {
		setConstants();
		setNumberOfEncampments();
		setNumberOfMidGameEnc();
		setNumberOfPreFusionEnc();
		setMapWidthAndHeight();
	}
	
	private static void performCensus() throws GameActionException {
		//Perform census
		if(Clock.getRoundNum()%CENSUS_INTERVAL == 0) {
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.LAY_MINES.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.SCOUT.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + SoldierType.ARMY.ordinal(),0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES,0);
			HQRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES,0);
			
			
		}
		
		if (Clock.getRoundNum() == 0) {
			//TODO set behavior for game based on team memory
			mRC.setIndicatorString(0,""+mRC.getTeamMemory()[0]);
			initializeRadioChannels();
			
		}
		else if(Clock.getRoundNum()%CENSUS_INTERVAL == 1){
			minerCount  = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.LAY_MINES.ordinal());
			// Don't respawn scouts unless we have vision
			if(mRC.hasUpgrade(Upgrade.VISION)) {
				scoutCount  = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.SCOUT.ordinal());
			}
			armyCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + SoldierType.ARMY.ordinal());
			generatorCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES);
			supplierCount = HQRobot.mRadio.readChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES + NUM_OF_CENSUS_GENERATORTYPES);
			HQRobot.mRadio.writeChannel(RadioChannels.NUM_GENERATORS,generatorCount);
			HQRobot.mRadio.writeChannel(RadioChannels.NUM_SUPPLIERS,supplierCount);
		}
	}
	
	private static void updateEnemyLocationData() throws GameActionException {
		//Sense Enemy robots and broadcast average position to bots
		Robot[] enemyRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mEnemy);		
		int avgX = 0, avgY = 0, numSoldiers = 0;
		for(Robot bot:enemyRobots){
			RobotInfo info = mRC.senseRobotInfo(bot);
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
		//Check if we should rush the enemy HQ
		checkShouldRush();
		//Check if we spawned a new unit
		checkNewUnitType();
		//write to the team memory what turn it is (or what turn nuke should be started) and how we or they might die this round
		setAllTeamMemory();
		
		//print("end Action All state: " + Clock.getBytecodesLeft() + "Round: " + Clock.getRoundNum());
		
		//TODO: comment why sometimes these return and some don't
		if(mRC.isActive()){
			if(mRC.checkResearchProgress(Upgrade.NUKE) > Upgrade.NUKE.numRounds - RUSH_NUKE_TIME) {
				// We're almost done with the nuke!
				mRC.researchUpgrade(Upgrade.NUKE);
				mRC.setIndicatorString(2, "Nuke almost done!, get ready to wear hats!!!");
				return;
			}
			/*if(numEncToClaim > 0 && Clock.getRoundNum() < 10){
				//HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);				
				return;
			}
			*/
			if(mRC.getTeamPower() < PREFUSION_POWER_RESERVE){
				pickResearch();
				return;
			}
			/*for (int i = RadioChannels.ENC_CLAIM_START;
					i < RadioChannels.ENC_CLAIM_START + Math.min(numEncToClaim, NUM_PREFUSION_ENC); i++) {
				if (HQRobot.mRadio.readChannel(i) == -1) {
					//HQRobot.spawnRobot();
					return;
				}
			}
			*/
			if(minerCount < NUM_MINERS) { 
				++ minerCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.LAY_MINES);
				return;
			}
			else if(scoutCount < NUM_SCOUTS) {
				++ scoutCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.SCOUT);
				return;
			}
			else if(pointCount<NUM_POINT_SCOUTS)
			{
				HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMYPOINT);
				HQRobot.mRadio.writeChannel(RadioChannels.POINT_SCOUT_TYPE, pointCount);
				++pointCount;
				return;
			}
			else if(armyCount < NUM_ARMY_NO_FUSION){
				++ armyCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
				return;
			}
			else if (!mRC.hasUpgrade(Upgrade.FUSION)) {
				mRC.researchUpgrade(Upgrade.FUSION);
				return;
			} 
			else if (HQRobot.enemyNukeSoon && !mRC.hasUpgrade(Upgrade.DEFUSION)) {
				mRC.researchUpgrade(Upgrade.DEFUSION);
				return;
			}
			else if (mRC.hasUpgrade(Upgrade.PICKAXE) && minerCount < NUM_MINERS_WITH_PICKAXE
					&& mRC.getTeamPower() > POWER_RESERVE/* && mRC.getTeamPower() > lastPower*/) {
				++ minerCount;
				HQRobot.spawnRobot(SoldierRobot.SoldierType.LAY_MINES);
				return;	
			}
			else {
				if(!HQRobot.enemyNukeSoon) {
					/*
					for (int i = RadioChannels.ENC_CLAIM_START;
							i < RadioChannels.ENC_CLAIM_START + HQRobot.maxEncChannel + BUFFER_ENC_CHANNEL_CHECK; i++) {
						if (HQRobot.mRadio.readChannel(i) == 0) { 
							//HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);							
							return;
						}
					}
					
					if(Clock.getRoundNum() > LATE_GAME){
						for (int i = RadioChannels.ENC_CLAIM_START;
								i < RadioChannels.ENC_CLAIM_START + HQRobot.maxEncChannel + BUFFER_ENC_CHANNEL_CHECK; i++) {
							if (HQRobot.mRadio.readChannel(i) == 0) {
								//HQRobot.spawnRobot(SoldierRobot.SoldierType.OCCUPY_ENCAMPMENT);
								return;
							}
						}
					}
					*/
				}
				if(armyCount < NUM_ARMY_WITH_FUSION
						&& mRC.getTeamPower() > POWER_RESERVE/* && mRC.getTeamPower() > lastPower*/) {
					++ armyCount;
					HQRobot.spawnRobot(SoldierRobot.SoldierType.ARMY);
					return;
				}								
				pickResearch();
			}
		}
		
		lastPower  = mRC.getTeamPower();
		
	}
	
	private static void checkEnemyNuking() throws GameActionException {
		if(!HQRobot.enemyNukeSoon && mRC.checkResearchProgress(Upgrade.NUKE) <= Upgrade.NUKE.numRounds/2 
		           && mRC.senseEnemyNukeHalfDone()) {
					HQRobot.enemyNukeSoon = true;
		}
		HQRobot.mRadio.writeChannel(RadioChannels.ENEMY_FASTER_NUKE, HQRobot.enemyNukeSoon ? 1 : 0);
	}
	

	private static void checkNewUnitType() throws GameActionException {
		if(Clock.getRoundNum() == 0)
			HQRobot.mRadio.writeChannel(RadioChannels.NEW_UNIT_ID, -1);
		
		int value;
		if((value = HQRobot.mRadio.readChannel(RadioChannels.NEW_UNIT_ID)) != -1) {
			soldierTypes[value/SoldierType.values().length]
					= SoldierType.values()[value%SoldierType.values().length];
			HQRobot.mRadio.writeChannel(RadioChannels.NEW_UNIT_ID, -1);
		}
	}

	private static void checkShouldRush() {
		if(mRC.senseNearbyGameObjects(Robot.class, mRC.senseEnemyHQLocation(),
				HQ_ENTER_RUSH_RAD, HQRobot.mTeam).length > 0)
			HQRobot.switchState(HQState.RUSH);
	}

	private static void checkHQSafety() throws GameActionException {

		if(mRC.senseNearbyGameObjects(Robot.class,HQ_PROTECT_RAD_SQUARED,ARobot.mEnemy).length > 0){
			HQInDanger = true;
			//the only reason this is being written is to change everyone who is not already a soldier to soldier type
			HQRobot.mRadio.writeChannel(RadioChannels.HQ_IN_DANGER, 1);
		}
		else{
			HQInDanger = false;
			HQRobot.mRadio.writeChannel(RadioChannels.HQ_IN_DANGER, 0);
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
		
		//print(i);
        for ( ; i < RadioChannels.ENC_CLAIM_START + HQRobot.maxEncChannel + BUFFER_ENC_CHANNEL_CHECK; i++ ) {
        	if ((tempInt = HQRobot.mRadio.readChannel(i)) != 0) {
        		tempLocation = indexToLocation(tempInt -1); // subtract 1 b.c it adds 1 when it takes it so we don't need initialization
        		if (!mRC.canSenseSquare(tempLocation) )
        		{
        			//If we can't sense the square, check to see if the tower says it should have been built or not
        			tempInt = HQRobot.mRadio.readChannel(RadioChannels.ENCAMPMENT_BUILDING_START + i - RadioChannels.ENC_CLAIM_START);
        			if ( tempInt == ENCAMPMENT_CAPTURE_STARTED ) {
        				print ("overwriting channel: " + (RadioChannels.ENCAMPMENT_BUILDING_START + i - RadioChannels.ENC_CLAIM_START));
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
			for (int i = RadioChannels.ENC_CLAIM_START;
					i < RadioChannels.ENC_CLAIM_START + HQRobot.maxEncChannel + BUFFER_ENC_CHANNEL_CHECK; i++) { 
				if (HQRobot.mRadio.readChannel(i) == locationToIndex(medbay)) {
					HQRobot.mRadio.writeChannel(i, -1);
				}
			}		
			HQRobot.mRadio.writeChannel(RadioChannels.MEDBAY_CLAIMED, 0);
		}
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
	
	private static void turtleState() throws GameActionException {
		if (encampmentInDanger == null) {
			
			//Get all our encampment squares
			MapLocation encampmentSquares[] = mRC.senseAlliedEncampmentSquares();
			if(encampmentSquares.length>0){
				//store the furthest distance from our base
				int distSquared =0;
				//store our encampment closest to enemy base (give it default value)
				int leastDist= encampmentSquares[0].distanceSquaredTo(HQRobot.enemyHQLoc);
				//loop through each encampment. if its distance is shorter than current least dist, replace it
				for(int i = 0;i<encampmentSquares.length;i++)
				{ 
					int temp = encampmentSquares[i].distanceSquaredTo(HQRobot.enemyHQLoc);
					if( temp<leastDist)
					{
						leastDist = temp;
						//store the location of the furthest encampment
						distSquared = i;
						
					}
				}
				//get distance from us to furthest encampment
				distSquared = (int)(mRC.getLocation().distanceSquaredTo(encampmentSquares[distSquared]));
				
				
				MapLocation rallyLoc = new MapLocation(
						(6*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/7,
						(6*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/7);
				float time = Clock.getRoundNum()/2500;
				MapLocation movingSpot = new MapLocation(
						(int)(mRC.getLocation().x +time*(HQRobot.enemyHQLoc.x-mRC.getLocation().x) ),
						(int)(mRC.getLocation().y + time*(HQRobot.enemyHQLoc.y-mRC.getLocation().y)));
				//move our wall to a point on the line between us and the enemy base.
				//That point should be the as far from us as our farthest encampment
				if(distSquared> rallyLoc.distanceSquaredTo(mRC.getLocation()) && distSquared> movingSpot.distanceSquaredTo(mRC.getLocation()))
				{//get distance from us to enemy HQ
					int dist = mRC.getLocation().distanceSquaredTo(HQRobot.enemyHQLoc);
					//How far along that vector should we go?
					float move =  (float)Math.sqrt((float)distSquared/dist);
					HQRobot.setRallyPoint(new MapLocation(
							(int)(mRC.getLocation().x +move*(HQRobot.enemyHQLoc.x-mRC.getLocation().x) ),
							(int)(mRC.getLocation().y + move*(HQRobot.enemyHQLoc.y-mRC.getLocation().y))));
				}
				//if that distance is too short, use our old code!
				else if(rallyLoc.distanceSquaredTo(mRC.getLocation()) <movingSpot.distanceSquaredTo(mRC.getLocation()))
				{
					HQRobot.setRallyPoint(movingSpot);
				}
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
		else {
			HQRobot.setRallyPoint(encampmentInDanger);
		}
		
		// Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		checkEnemyNuking();
		if ( HQRobot.enemyNukeSoon ) {
			HQRobot.switchState(HQState.ATTACK); 
		}
		
		else if (Clock.getRoundNum() >= ATTACK_ROUND ) {
			HQRobot.switchState(HQState.ATTACK);
		}
		else {
			HQRobot.mRadio.writeChannel(RadioChannels.SHOULD_LAY_MINES, 1);
		}
	}

	private static void prepareAttackState() throws GameActionException {
		Robot[] alliedRobots = mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, HQRobot.mTeam);
		MapLocation preAttackRallyLocation = new MapLocation(
				(4*mRC.getLocation().x + HQRobot.enemyHQLoc.x)/5,
				(4*mRC.getLocation().y + HQRobot.enemyHQLoc.y)/5);
		if(Math.min(armyCount, alliedRobots.length) > NUM_ARMY_BEFORE_ATTACK)
			HQRobot.switchState(HQState.ATTACK); //attack!
		HQRobot.setRallyPoint(preAttackRallyLocation);
		
		HQRobot.mRadio.writeChannel(RadioChannels.SHOULD_LAY_MINES, 0);
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
		
		if((Math.min(armyCount, alliedRobots.length) < NUM_ARMY_BEFORE_RETREAT && (!HQRobot.enemyNukeSoon)) 
				|| (HQRobot.enemyNukeSoon && Math.min(armyCount, alliedRobots.length) < NUM_ARMY_BEFORE_ATTACK_WITH_NUKE)) 
			HQRobot.switchState(HQState.PREPARE_ATTACK);

		if(waypointsToEnemyHQ == null)
			HQRobot.setRallyPoint(mRC.senseEnemyHQLocation());
		else{
			//HQRobot.setRallyPoints(waypointsToEnemyHQ);
			int nextWaypointIndex = findNextWaypointIndex(waypointsToEnemyHQ, avg);
			if(HQRobot.enemyNukeSoon){
				if(nextWaypointIndex < waypointsToEnemyHQ.length - 1
						&& mRC.senseNearbyGameObjects(Robot.class, waypointsToEnemyHQ[nextWaypointIndex],
						32, HQRobot.mTeam).length >= NUM_ARMY_BEFORE_ATTACK_WITH_NUKE)
					++nextWaypointIndex;
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



