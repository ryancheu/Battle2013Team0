package nickTest1;

import java.util.ArrayList;

import battlecode.common.*;

/**
 * The example funcs player is a player meant to demonstrate basic usage of the
 * most common commands. Robots will move around randomly, occasionally mining
 * and writing useless messages. The HQ will spawn soldiers continuously.
 */
public class RobotPlayer {

	public enum SoldierType {
		OCCUPY_ENCAMPMENT, 
		LAY_MINES,
		ARMY,
		ARTILLERY
	}

	public enum SoldierState {

		// ENCAMPMENT SOLDIER
		FIND_ENCAMPMENT, 
		GOTO_ENCAMPMENT,		

		// MINE SOLDIER
		MINE,
		
		GOTO_RALLY,
		
		//ARTILLERY
		FIRE,
		
		//HQ
		TURTLE,
		PREPARE_ATTACK,
		ATTACK,
	}

	// Battlecode constants
	private static final int MAX_DIST_SQUARED = 70 * 70;
	private static final int NUM_DIR = 8;

	// Player specific
	private static int NUM_SUPPLIERS = 0;
	private static int NUM_GENERATORS = 0;
	
	// Player Consts
	private static int NUM_ENC_TO_CLAIM = 10;
	private static final int NUM_MINERS = 0;
	private static final int NUM_ARMY = 25;
	private static final int CENSUS_INTERVAL = 10;

	// HQ Consts
	private static final int NUM_ROBOT_TO_SPAWN = NUM_ENC_TO_CLAIM + NUM_MINERS + NUM_ARMY;

	// Radio Consts
	private static final int TEAM_A_BROADCAST_OFFSET = 1234;
	private static final int TEAM_B_BROADCAST_OFFSET = 4321;
	private static final int ENC_CLAIM_RAD_CHAN_START = 100; // NUM_ENC_TO_CLAIM channels
	private static final int COUNT_MINERS_RAD_CHAN = ENC_CLAIM_RAD_CHAN_START + NUM_ENC_TO_CLAIM; // 1 channel
	private static final int SPAWN_MINER_RAD_CHAN = COUNT_MINERS_RAD_CHAN + 1; // 1 channel
	private static final int RALLY_RAD_CHAN = SPAWN_MINER_RAD_CHAN + 1; // 1 channel

	// Used by all
	private static Team ourTeam;
	private static Team enemyTeam;
	private static RobotType myType = null;
	private static MapLocation myLocation = null;
	private static final int[] testDirOrderAll = { 0, 1, -1, 2, -2, 3, -3, 4 };
	private static final int[] testDirOrderFront = { 0, 1, -1 };
	private static final int[] testDirOrderFrontSide = { 0, 1, -1, 2, -2 };
	private static int mapWidth;
	private static int mapHeight;
	private static int broadcastOffset;

	// HQ Only Variables
	private static MapLocation enHQPos = null;
	private static Direction enHQDir = null;
	private static Robot[] alliedRobots = null;

	//Soldier Variables
	private static MapLocation curDest = null;
	private static SoldierType mySoldierType = null;
	private static SoldierState mySoldierState = null;
	
	//Artillery Variables
	private static int lastRoundShot = 0;
	private static final int LAST_ROUND_SHOT_DELAY = 5;

	public static void run(RobotController rc) {
		ourTeam = rc.getTeam();
		enemyTeam = ourTeam.opponent();
		myType = rc.getType();
		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();
		broadcastOffset = ourTeam == Team.A ? TEAM_A_BROADCAST_OFFSET : TEAM_B_BROADCAST_OFFSET;
		enHQPos = rc.senseEnemyHQLocation();
		if (myType == RobotType.HQ) {
			enHQDir = rc.getLocation().directionTo(enHQPos);
			myLocation = rc.getLocation();
			mySoldierState = SoldierState.PREPARE_ATTACK;
		}
		while (true) {
			try {
				if (myType == RobotType.HQ)
					mainHQLogic(rc);
				else if (rc.isActive()) {
					switch(myType){
					case SOLDIER:
						myLocation = rc.getLocation();
						mainSoldierLogic(rc);
						break;
					case ARTILLERY:
						mainArtilleryLogic(rc);
						break;
					}
				}
				if(Clock.getRoundNum()%CENSUS_INTERVAL == 0)
					performCensus(rc);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			// End turn
			rc.yield();
		}
	}
	
	private static void performCensus(RobotController rc) throws GameActionException {
		if(myType == RobotType.HQ)
			rc.broadcast(broadcastOffset + COUNT_MINERS_RAD_CHAN, 0);
		else if(mySoldierState == SoldierState.MINE){
			int count = rc.readBroadcast(broadcastOffset + COUNT_MINERS_RAD_CHAN);
			rc.broadcast(broadcastOffset + COUNT_MINERS_RAD_CHAN, count + 1);
		}
	}

	private static void mainArtilleryLogic(RobotController rc) throws GameActionException
	{
		artilleryFireLogic(rc);
	}

	private static void mainHQLogic(RobotController rc) throws GameActionException {
		alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, ourTeam);
		
		if (Clock.getRoundNum() == 0) {
			setNumberOfEncampments(rc);
			System.out.println("ran hq code");
			for (int i = ENC_CLAIM_RAD_CHAN_START; i < NUM_ENC_TO_CLAIM + ENC_CLAIM_RAD_CHAN_START; ++i) {
				rc.broadcast(broadcastOffset + i, -1);
			}
		}
		else if(Clock.getRoundNum()%CENSUS_INTERVAL == 1){
			int minerCount = rc.readBroadcast(broadcastOffset + COUNT_MINERS_RAD_CHAN);
			if(minerCount < NUM_MINERS)
				rc.broadcast(broadcastOffset + SPAWN_MINER_RAD_CHAN, NUM_MINERS - minerCount);
		}
		
		if(rc.isActive()){
			if (alliedRobots.length <= NUM_ROBOT_TO_SPAWN) {
				spawnRobot(rc);
			} else {
				if (rc.hasUpgrade(Upgrade.FUSION)) {
					rc.researchUpgrade(Upgrade.NUKE);
				} else {
					rc.researchUpgrade(Upgrade.FUSION);
				}
			}
		}
		
		switch(mySoldierState){
		case TURTLE:
			turtleHQLogic(rc);
			break;
		case PREPARE_ATTACK:
			prepareAttackHQLogic(rc);
			break;
		case ATTACK:
			attackHQLogic(rc);
			break;
		}
		
	}

	private static void spawnRobot(RobotController rc) throws GameActionException {
		Direction tempDir = null;
		for (int i = 0; i < NUM_DIR; ++i) {
			tempDir = Direction.values()[(enHQDir.ordinal() + i + NUM_DIR) % NUM_DIR];
			if (rc.canMove(tempDir)) {
				rc.spawn(tempDir);
				break;
			}
		}
	}
	
	private static void setNumberOfEncampments(RobotController rc) throws GameActionException{
		//should use number of encampments, number of closer encampments, 
		MapLocation[] allEncampments = rc.senseEncampmentSquares(myLocation, MAX_DIST_SQUARED, Team.NEUTRAL);
		int encampmentsLength = allEncampments.length;
		int encampmentsCloserLength = 0;
		int rushDistance = rc.senseHQLocation().distanceSquaredTo(rc.senseEnemyHQLocation());
		System.out.println(rushDistance);
		MapLocation[] encampmentsCloser = new MapLocation[allEncampments.length];
		
		for(int e = 0; e < allEncampments.length; e++){
			if(allEncampments[e].distanceSquaredTo(rc.senseEnemyHQLocation()) > allEncampments[e].distanceSquaredTo(rc.senseHQLocation())){
				encampmentsCloser[encampmentsCloserLength] = allEncampments[e];
				encampmentsCloserLength++;
			}
		}
		//NUM_ENC_TO_CLAIM=allEncampments.length/4;
		//some function of encampmentsLength,encampmentsCloserLength, rushDistance
		
		if(rushDistance<1000){
			NUM_ENC_TO_CLAIM=encampmentsCloserLength;
		}
		if(rushDistance>=1000 && rushDistance < 2000){
			NUM_ENC_TO_CLAIM=encampmentsCloserLength;
		}
		if(rushDistance>=2000 && rushDistance < 5000){
			NUM_ENC_TO_CLAIM = (int)(encampmentsLength/4.0);
		}
		if(rushDistance >= 5000){
			NUM_ENC_TO_CLAIM = (int)(encampmentsLength/3.0);
		}
		
		/*
		 * data for rush distance:
		 * 8978 - fucking huge
		 * 3242 - huge
		 * 1570 - moderate
		 * 800 - small
		 * 1170 - moderate
		 */
		
	}

	private static void turtleHQLogic(RobotController rc) throws GameActionException {
		setRallyPoint(rc, myLocation);
		if(rc.checkResearchProgress(Upgrade.NUKE) <= 200 && rc.senseEnemyNukeHalfDone())
			mySoldierState = SoldierState.ATTACK;
	}
	
	private static void prepareAttackHQLogic(RobotController rc) throws GameActionException {
		setRallyPoint(rc, new MapLocation((2*myLocation.x + enHQPos.x)/3, (2*myLocation.y + enHQPos.y)/3));
		if(alliedRobots.length >= NUM_ROBOT_TO_SPAWN || rc.senseEnemyNukeHalfDone())
			mySoldierState = SoldierState.ATTACK;
	}
	
	private static void attackHQLogic(RobotController rc) throws GameActionException {
		int avgX = 0, avgY = 0, numSoldiers = 0;
		for(Robot bot:alliedRobots){
			RobotInfo info = rc.senseRobotInfo(bot);
			if(info.type == RobotType.SOLDIER){
				numSoldiers ++;
				avgX += info.location.x;
				avgY += info.location.y;
			}
		}
		avgX /= numSoldiers;
		avgY /= numSoldiers;
		setRallyPoint(rc, new MapLocation((2*avgX + enHQPos.x)/3, (2*avgY + enHQPos.y)/3));
		if(alliedRobots.length < NUM_ROBOT_TO_SPAWN/2)
			mySoldierState = SoldierState.PREPARE_ATTACK; // Retreat!
	}
	private static void mainSoldierLogic(RobotController rc)
			throws GameActionException {

		// First run of soldier, assign type
		assignType: if (mySoldierType == null) {

			for (int i = ENC_CLAIM_RAD_CHAN_START; i < ENC_CLAIM_RAD_CHAN_START
					+ NUM_ENC_TO_CLAIM; i++) {
				if (rc.readBroadcast(broadcastOffset + i) == -1) {
					mySoldierType = SoldierType.OCCUPY_ENCAMPMENT;
					mySoldierState = SoldierState.FIND_ENCAMPMENT;
					break assignType;
				}
			}

			int spawnMiners = rc.readBroadcast(broadcastOffset + SPAWN_MINER_RAD_CHAN);
			if (spawnMiners > 0){
				rc.broadcast(broadcastOffset + SPAWN_MINER_RAD_CHAN, spawnMiners - 1);
				mySoldierType = SoldierType.LAY_MINES;
				mySoldierState = SoldierState.MINE;
				break assignType;
			}

			mySoldierType = SoldierType.ARMY;
			mySoldierState = SoldierState.GOTO_RALLY;
		}
		
		switch (mySoldierType) {
		case OCCUPY_ENCAMPMENT:
			encampmentSoldierLogic(rc);
			break;
		case LAY_MINES:
			layMineSoldierLogic(rc);
			break;
		case ARMY:
			armySoldierLogic(rc);
			break;
		default:
			// TODO: raise error
			break;
		}

	}

	private static void encampmentSoldierLogic(RobotController rc)
			throws GameActionException {
		switch (mySoldierState) {
			case FIND_ENCAMPMENT: {
				findEncampmentStateLogic(rc);
				break;
			}
			case GOTO_ENCAMPMENT: {
				gotoEncampmentLogic(rc);
				break;
			}
		}
	}
	
	private static void findEncampmentStateLogic(RobotController rc) throws GameActionException
	{
		System.out.println("running enc code");
		// Using the radio broadcasts, find the encampment locations already
		// claimed by other soldiers
		int tempRead, numFound;
		ArrayList<MapLocation> claimedEncampmentLocs = new ArrayList<MapLocation>();
		for (numFound = 0; numFound < NUM_ENC_TO_CLAIM; ++numFound) {
			if ((tempRead = rc.readBroadcast(broadcastOffset + numFound + ENC_CLAIM_RAD_CHAN_START)) == -1) {
				break;
			} else {
				claimedEncampmentLocs.add(indexToLocation(tempRead));
			}
		}

		MapLocation[] allEncampments = rc.senseEncampmentSquares(myLocation, MAX_DIST_SQUARED, Team.NEUTRAL);
		NUM_ENC_TO_CLAIM=allEncampments.length/4;
		int closestDist = MAX_DIST_SQUARED;
		int closestIndex = -1;
		int tempDist;		
		MapLocation tempLocation;
		boolean alreadyClaimed = false;

		// Search through all encampments and find the closest one not
		// already claimed
		System.out.println("enc length: " + allEncampments.length);
		for (int i = 0; i < allEncampments.length; i++) {
			tempLocation = allEncampments[i];

			for (MapLocation l : claimedEncampmentLocs) {
				if (l.equals(tempLocation)) {
					alreadyClaimed = true;
					System.out.println("location: " + locationToIndex(tempLocation));
					break;
				}
			}

			if (alreadyClaimed) {
				alreadyClaimed = false;
				continue;
			}

			if ((tempDist = tempLocation.distanceSquaredTo(myLocation)) < closestDist) {
				closestDist = tempDist;
				closestIndex = i;
			} else {
				System.out.println("temp dist" + tempDist);
			}
		}

		// Set the destination to the closest non-claimed encampment, and
		// claim the encampment
		if (closestIndex != -1) {
			curDest = allEncampments[closestIndex];
			rc.broadcast(broadcastOffset + ENC_CLAIM_RAD_CHAN_START + numFound, locationToIndex(curDest));
			mySoldierState = SoldierState.GOTO_ENCAMPMENT;
		} else { // There were no unclaimed encampments
			// TODO: Broadcast error message
			mySoldierType = SoldierType.LAY_MINES;
			mySoldierState = SoldierState.MINE;
		}
		return;
	}
	
	private static void gotoEncampmentLogic(RobotController rc) throws GameActionException
	{
		if (curDest.equals(myLocation)) {
			if(rc.senseCaptureCost()<rc.getTeamPower())
				rc.captureEncampment(RobotType.SUPPLIER);
				
	
		}

		/*
		// Try to head towards the target position, no turning back (NO
		// PATHFINDING)
		// TODO: Pathfinding, mine defusion
		Direction tempDir;
		Direction dirToDest = myLocation.directionTo(curDest);
		boolean foundDir = false;
		for (int i : testDirOrderFrontSide) {
			if (rc.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR])) {
				rc.move(tempDir);
				foundDir = true;
				break;
			}
		}
		if (!foundDir) { // Blocked on all sides
			// TODO: Do something
		}
		*/
		goToLocation(rc, curDest);
	}

	private static void layMineSoldierLogic(RobotController rc) throws GameActionException {

		// If current location is blank, lay a mine there
		if (rc.senseMine(myLocation) == null) {
			rc.layMine();
			return;
		}
		
		//if(!goToLocation(rc, rc.senseHQLocation()))
		//		goToLocation(rc, rc.senseEnemyHQLocation());

		// Otherwise try to go towards the HQ and lay a mine
		Direction tempDir = null;
		Direction dirToDest = myLocation.directionTo(rc.senseHQLocation());
		boolean foundDir = false;
		for (int i : testDirOrderAll) {
			if (rc.canMove(tempDir = Direction.values()[(i + dirToDest.ordinal() + NUM_DIR) % NUM_DIR]) && !isMineDir(rc, myLocation, tempDir)) {
				rc.move(tempDir);
				foundDir = true;
				break;
			}
		}

		// Try going away from HQ
		goToLocation(rc, enHQPos);

	}

	private static void armySoldierLogic(RobotController rc) throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, rc.getTeam().opponent());
		alliedRobots = rc.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, ourTeam);
		if(enemyRobots.length==0){//no enemies visible
			goToLocation(rc, findRallyPoint(rc));
		} else if (enemyRobots.length < alliedRobots.length){ //someone spotted
			int closestDist = MAX_DIST_SQUARED;
			MapLocation closestEnemy=null;
			for (Robot arobot:enemyRobots){
				RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
				int dist = arobotInfo.location.distanceSquaredTo(myLocation);
				if (dist<closestDist){
					closestDist = dist;
					closestEnemy = arobotInfo.location;
				}
			}
			goToLocation(rc, closestEnemy);
		}
		else
			goToLocation(rc, rc.senseHQLocation());
	}
	
	private static MapLocation findRallyPoint(RobotController rc) throws GameActionException {
		// TODO Auto-generated method stub
		return indexToLocation(rc.readBroadcast(broadcastOffset + RALLY_RAD_CHAN));
	}
	
	private static void setRallyPoint(RobotController rc, MapLocation rally) throws GameActionException {
		rc.broadcast(broadcastOffset + RALLY_RAD_CHAN, locationToIndex(rally));
	}
	
	private static void artilleryFireLogic(RobotController rc) throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,RobotType.ARTILLERY.attackRadiusMaxSquared,enemyTeam);
		MapLocation[] robotLocations = new MapLocation[enemyRobots.length];
		int i = 0;
		RobotInfo tempRoboInfo;
		MapLocation tempMapLoc;
		for(Robot bot : enemyRobots) {
			tempRoboInfo = rc.senseRobotInfo(bot);
			tempMapLoc = tempRoboInfo.location;
			robotLocations[i++] = tempMapLoc;			
		}
		int maxIndex = 0;
		int maxAdjacent = 0;
		for(int j = 0; j < robotLocations.length;++j){
			int numberOfAdjacent = 0;
			for(int k = 0; k < robotLocations.length;++k){
				if(robotLocations[j].isAdjacentTo(robotLocations[k])){
					++numberOfAdjacent;
				}
				if(numberOfAdjacent>maxAdjacent){
					maxAdjacent = numberOfAdjacent;
					maxIndex = j;
				}
			}
		
		}
		if((maxAdjacent>0 || Clock.getRoundNum()-lastRoundShot > LAST_ROUND_SHOT_DELAY + rc.getType().attackDelay) 
				&& robotLocations.length > 0 && rc.canAttackSquare(robotLocations[maxIndex])){
			rc.attackSquare(robotLocations[maxIndex]);
			lastRoundShot = Clock.getRoundNum();
		}
	}
	
	// Attempt to go towards whereToGo, return true if it successfully took an action
	private static boolean goToLocation(RobotController rc, MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			for (int d:testDirOrderFrontSide){
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
				if(rc.canMove(lookingAtCurrently)){
					MapLocation newLoc = rc.getLocation().add(lookingAtCurrently);
					Team mineOwner = rc.senseMine(newLoc); 
					if(mineOwner != null && mineOwner != rc.getTeam())
						rc.defuseMine(newLoc);
					else
						rc.move(lookingAtCurrently);
					return true;
				}
			}
		}
		return false;
	}

	//UTIL FUNCTIONS 
	private static int locationToIndex(MapLocation l) {
		return mapWidth * l.y + l.x;
	}

	private static MapLocation indexToLocation(int index) {
		return new MapLocation(index % mapWidth, index / mapWidth);
	}
	//Tests for mine in direction from a location
	private static boolean isMineDir(RobotController rc, MapLocation mp, Direction d) {
		return (rc.senseMine(mp.add(d)) != null);
	}
}