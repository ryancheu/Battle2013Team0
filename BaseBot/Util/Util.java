package BaseBot.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;





import BaseBot.Robots.*;
import BaseBot.Robots.SoldierRobot.SoldierType;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Upgrade;
import static BaseBot.Robots.ARobot.mRC;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.NonConstants.*;

public class Util {
	
	//try to go to a location, argument as to whether to defuse mines along the way
	public static boolean goToLocation(MapLocation whereToGo) throws GameActionException {
		return goToLocation(whereToGo, true);
	}
	public static MapLocation goToLocationReturn(MapLocation whereToGo, boolean defuseMines)throws GameActionException {
		//TODO if its an hq and stuff is in the way you gotta kill it
		boolean foundMine = false, foundEnemyMine = false;

		//mRC.setIndicatorString(0, "goToLocation "+whereToGo+" "+defuseMines);
		if (mRC.isActive() && !mRC.getLocation().equals(whereToGo)) {
			Direction dir = mRC.getLocation().directionTo(whereToGo);
			
			for ( int d:testDirOrderFront ) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				MapLocation newLoc = mRC.getLocation().add(lookingAtCurrently);

				MineStatus mineStatus = getMineStatus(newLoc);
				if(mineStatus == MineStatus.DEFUSED) {
					// There's no mine here, we should move here if possible
					if(mRC.canMove(lookingAtCurrently)) {
						mRC.move(lookingAtCurrently);
						return newLoc;
					}
					continue;
				}
				
			}
			
			//DO NOT OPTIMIZE THIS OUT DAMNIT.
			for (int d:testDirOrderFrontSide) {

				if (d== 2) {
					if(foundMine && (!foundEnemyMine || hasAllyInFront(mRC.senseEnemyHQLocation())
							|| SoldierRobot.enemyNukingFast)) {
						defuseMineNear(whereToGo);
						return mRC.getLocation();
					}
				}

				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				MapLocation newLoc = mRC.getLocation().add(lookingAtCurrently);

				MineStatus mineStatus = getMineStatus(newLoc);
				if(mineStatus == MineStatus.DEFUSED) {
					// There's no mine here, we should move here if possible
					if(mRC.canMove(lookingAtCurrently)) {
						mRC.move(lookingAtCurrently);
						return newLoc;
					}
					continue;
				}

				Team mineOwner = mRC.senseMine(newLoc);
				if(mineOwner != Team.NEUTRAL) {
					foundEnemyMine = true;
				}
				foundMine = true;

				if(mineStatus == MineStatus.DEFUSING) {
					// Someone else is defusing here
					continue;
				}

				if(defuseMines) {
					if(!mRC.hasUpgrade(Upgrade.DEFUSION)) {
						// Don't do anything fancy if we don't have defusion
						mRC.defuseMine(newLoc);
						setMineStatus(newLoc, MineStatus.DEFUSING);
						return mRC.getLocation();
					}
					continue;
				}

				if(mRC.canMove(lookingAtCurrently) &&
						mineOwner != Team.NEUTRAL &&
						Math.random() < CHANCE_OF_DEFUSING_ENEMY_MINE) {
					defuseMineNear(newLoc);
					return mRC.getLocation();
				}
			}
		}

		if(defuseMines) {
			if(!foundEnemyMine || hasAllyInFront(mRC.senseEnemyHQLocation())
					|| SoldierRobot.enemyNukingFast) {
				defuseMineNear(whereToGo);
				return mRC.getLocation();
			}
				
		}

		return mRC.getLocation();
	}
	public static boolean goToLocation(MapLocation whereToGo, boolean defuseMines) 
			throws GameActionException {
		//TODO if its an hq and stuff is in the way you gotta kill it
		boolean foundMine = false, foundEnemyMine = false;

		//mRC.setIndicatorString(0, "goToLocation "+whereToGo+" "+defuseMines);
		if (mRC.isActive() && !mRC.getLocation().equals(whereToGo)) {
			Direction dir = mRC.getLocation().directionTo(whereToGo);
			for ( int d:testDirOrderFront ) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				MapLocation newLoc = mRC.getLocation().add(lookingAtCurrently);

				MineStatus mineStatus = getMineStatus(newLoc);
				if(mineStatus == MineStatus.DEFUSED) {
					// There's no mine here, we should move here if possible
					if(mRC.canMove(lookingAtCurrently)) {
						mRC.move(lookingAtCurrently);
						return true;
					}
					continue;
				}
				
			}
			for (int d:testDirOrderFrontSide) {

				if (d == 2) {
					if(foundMine && (!foundEnemyMine || hasAllyInFront(mRC.senseEnemyHQLocation())
							|| SoldierRobot.enemyNukingFast)) {
						return defuseMineNear(whereToGo);
					}
				}

				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				MapLocation newLoc = mRC.getLocation().add(lookingAtCurrently);

				MineStatus mineStatus = getMineStatus(newLoc);
				if(mineStatus == MineStatus.DEFUSED) {
					// There's no mine here, we should move here if possible
					if(mRC.canMove(lookingAtCurrently)) {
						mRC.move(lookingAtCurrently);
						return true;
					}
					continue;
				}

				Team mineOwner = mRC.senseMine(newLoc);
				if(mineOwner != Team.NEUTRAL) {
					foundEnemyMine = true;
				}
				foundMine = true;

				if(mineStatus == MineStatus.DEFUSING) {
					// Someone else is defusing here
					continue;
				}

				if(defuseMines) {
					if(!mRC.hasUpgrade(Upgrade.DEFUSION)) {
						// Don't do anything fancy if we don't have defusion
						mRC.defuseMine(newLoc);
						setMineStatus(newLoc, MineStatus.DEFUSING);
						return true;
					}
					continue;
				}

				if(mRC.canMove(lookingAtCurrently) &&
						mineOwner != Team.NEUTRAL &&
						Math.random() < CHANCE_OF_DEFUSING_ENEMY_MINE) {
					defuseMineNear(newLoc);
					return true;
				}
			}
		}

		if(defuseMines) {
			if(!foundEnemyMine || hasAllyInFront(mRC.senseEnemyHQLocation())
					|| SoldierRobot.enemyNukingFast)
				return defuseMineNear(whereToGo);
		}

		return false;
	}
	public static boolean defuseMineNear(MapLocation target) throws GameActionException {
		return defuseMineNear(target, null);
	}
	
	public static boolean defuseMineNear(MapLocation target, Team team) throws GameActionException {
		int range = 2;
		if(mRC.hasUpgrade(Upgrade.DEFUSION)) {
			range = RobotType.SOLDIER.sensorRadiusSquared;
			if (mRC.hasUpgrade(Upgrade.VISION)) {
				range += GameConstants.VISION_UPGRADE_BONUS;
			}
		}

		MapLocation best = null;
		for(MapLocation loc = mRC.getLocation(); mRC.getLocation().distanceSquaredTo(loc) <= range;
				loc = loc.add(loc.directionTo(target))) {
			if(getMineStatus(loc) == MineStatus.NOT_DEFUSED) {
				if(team == null || mRC.senseMine(loc) == team) {
					best = loc;
				}
			}
			if(loc.equals(target))
				break;
		}
		if(best != null) {
			mRC.defuseMine(best);
			setMineStatus(best, MineStatus.DEFUSING);
			mRC.setIndicatorString(2, "defusing "+best);
			return true;
		}
		
		MapLocation[] mines;
		if(team == null)
			mines = mRC.senseNonAlliedMineLocations(mRC.getLocation(), range);
		else
			mines = mRC.senseMineLocations(mRC.getLocation(), range, team);
		best = null;
		int minDist = MAX_DIST_SQUARED, tempDist;
		for(int n=mines.length; --n>=0;) {
			tempDist = target.distanceSquaredTo(mines[n]);
			if(tempDist < minDist && getMineStatus(mines[n]) == MineStatus.NOT_DEFUSED) {
				minDist = tempDist;
				best = mines[n];
			}
		}
		if(best != null && minDist < mRC.getLocation().distanceSquaredTo(target)) {
			mRC.defuseMine(best);
			setMineStatus(best, MineStatus.DEFUSING);
			mRC.setIndicatorString(2, "defusing2 "+best);
			return true;
		}
		return false;
	}
	
	public static boolean hasAllyInFront(MapLocation target) throws GameActionException {
		Direction dir = mRC.getLocation().directionTo(target);
		
		for (int d = testDirOrderFront.length; --d >= 0; ) {
			Direction lookingAtCurrently = DIRECTION_REVERSE[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
			MapLocation newLoc = mRC.getLocation().add(lookingAtCurrently);
			GameObject obj = mRC.senseObjectAtLocation(newLoc);
			if(obj != null && obj.getTeam() == ARobot.mTeam) {
				return true;
			}
		}
		return false;
	}
	
	public static int getRealDistance(MapLocation a, MapLocation b) {
		return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
	}
	
	public static void setMineStatus(MapLocation mine, MineStatus status) throws GameActionException {
		ARobot.mRadio.writeChannel(RadioChannels.MINE_STATUS_START + locationToIndex(mine),
				FIRST_BYTE_KEY
				| (Clock.getRoundNum() << 2)
				| status.ordinal());
		if(status == MineStatus.DEFUSING) {
			SoldierRobot.lastDefusion = mine;
		}
	}
	
	public static MineStatus getMineStatus(MapLocation mine) throws GameActionException {
		Team owner = mRC.senseMine(mine);
		if (owner == ARobot.mTeam) {
			// It's our own mine
			return MineStatus.DEFUSED;
		}
		int distToEnemy = getRealDistance(mRC.senseEnemyHQLocation(), mine); 
		if (owner == null && distToEnemy > SoldierRobot.enemyMineRadius) {
			// We don't see a mine and we're far from the enemy HQ
			return MineStatus.DEFUSED;
		}
		
		// Either we see a mine or we're near the enemy HQ
		
		int value = ARobot.mRadio.readChannel(RadioChannels.MINE_STATUS_START + locationToIndex(mine));
		if ((value & FIRST_BYTE_KEY_MASK) != FIRST_BYTE_KEY) {
			value = 0;
		}
		else {
			value ^= FIRST_BYTE_KEY;
		}
		int roundNum = (value >> 2);
		MineStatus status;/*
		//this is where my changes start
		if(value < MineStatus.values().length){
			//this was here before my changes
			 * 
			 */
		status = MineStatus.values()[(value & II_BIT_MASK)];
		/*
		}
		
		else{
			return MineStatus.NOT_DEFUSED;
		}
		*/
		//this is where my changes end
		if (status == MineStatus.DEFUSING
				&& Clock.getRoundNum() - roundNum <= GameConstants.MINE_DEFUSE_DELAY) {
			return MineStatus.DEFUSING;
		}
		if (status == MineStatus.DEFUSING) {
			// We died trying to defuse this mine
			return MineStatus.NOT_DEFUSED;
		}
		
		if (owner == ARobot.mEnemy || owner == Team.NEUTRAL) {
			return MineStatus.NOT_DEFUSED;
		}
		
		return status;
	}

	public static MapLocation[] findWaypoints(MapLocation start, MapLocation target,boolean scouts){
		if(!Pathfinder.isStarted())
			Pathfinder.startComputation(start,scouts);
		else if(!Pathfinder.isDone())
			Pathfinder.continueComputation();
		else
			return Pathfinder.findWaypoints(target);
		return null;
	}
	
	public static void precomputeWaypoints(MapLocation start){
		if(!Pathfinder.isStarted())
			Pathfinder.startComputation(start,false);
		else if(!Pathfinder.isDone())
			Pathfinder.continueComputation();
	}
	
	public static MapLocation findNextWaypoint(MapLocation[] waypoints){
		return findNextWaypoint(waypoints, mRC.getLocation());
	}
	
	public static MapLocation findNextWaypoint(MapLocation[] waypoints, MapLocation from){
		return waypoints[findNextWaypointIndex(waypoints, from)];
	}
	
	public static int findNextWaypointIndex(MapLocation[] waypoints, MapLocation from){
		int closestWaypoint = -1;
		int closestWaypointDistance = MAX_DIST_SQUARED;
		int waypointsLength = waypoints.length;
		for (int i = waypoints.length; --i >= 0; ) {		
			MapLocation waypoint = waypoints[i];
			int dist = from.distanceSquaredTo(waypoint); 
			if(dist <= closestWaypointDistance){
				closestWaypoint = i;
				closestWaypointDistance = dist;
			}
		}
		if(closestWaypoint == waypointsLength-1) {
			return closestWaypoint;
		}
		if(closestWaypoint == 0) {
			return closestWaypoint + 1;
		}
		try {
			int prevDist = from.distanceSquaredTo(waypoints[closestWaypoint - 1]);
			int nextDist = from.distanceSquaredTo(waypoints[closestWaypoint + 1]);
			if(prevDist > nextDist || closestWaypointDistance < prevDist/4) {			 
				return closestWaypoint + 1;
			}
			else {
				return closestWaypoint;
			}
		}
		catch (Exception e) {
			return 0;
		}
	}
	
	public static MapLocation[] convertWaypoints(MapLocation[] waypoints, MapLocation from, MapLocation to) {
		int fromIndex = findNearestWaypointIndex(waypoints, from);
		int toIndex = findNearestWaypointIndex(waypoints, to);
		if(fromIndex <= toIndex) {
			int length = toIndex - fromIndex + 1;
			MapLocation[] subarray = new MapLocation[length + 1];
			for(int n = 0; n<length; ++n) {
				subarray[n] = waypoints[fromIndex + n];
			}
			subarray[length] = to;
			return subarray;
		}
		else {
			int length = fromIndex - toIndex + 1;
			MapLocation[] reversed = new MapLocation[length + 1];
			for(int n = 0; n<length; ++n) {
				reversed[n] = waypoints[fromIndex - n];
			}
			reversed[length] = to;
			return reversed;
		}
	}
	
	public static int findNearestWaypointIndex(MapLocation[] waypoints, MapLocation loc) {
		int closestWaypoint = -1;
		int closestWaypointDistance = MAX_DIST_SQUARED;
		for(int i=waypoints.length; --i>=0;){
			MapLocation waypoint = waypoints[i];
			int dist = loc.distanceSquaredTo(waypoint); 
			if(dist <= closestWaypointDistance){
				closestWaypoint = i;
				closestWaypointDistance = dist;
			}
		}
		return closestWaypoint;
	}
	
	/*
	public static boolean checkIDs(Radio mRadio) throws GameActionException{
		int [] IDs = new int[NUM_ROBOTS_TO_CHECK_ID];
		int min = MAX_DIST_SQUARED;//big number
		int minIndex = 0;
		for(int i = 0; i < NUM_ROBOTS_TO_CHECK_ID; ++i){
			IDs[i] = mRadio.readChannel(LAST_FOUR_BOT_ID_RAD_CHAN_START + i);
			if(IDs[i] < min){
				//makes sure that you start from the lowest ID
				min = IDs[i];
				minIndex = i;
			}
		}
		for(int i = minIndex ; i < NUM_ROBOTS_TO_CHECK_ID + minIndex ; ++i){
			if(IDs[i % NUM_ROBOTS_TO_CHECK_ID] + 1 != IDs[(i + 1) % NUM_ROBOTS_TO_CHECK_ID]){
				return false;
				//they were not consecutive
			}
		}
		return true;
	}
	*/
	public static void setMapWidthAndHeight(){
		Map_Width = mRC.getMapWidth();
		Map_Height = mRC.getMapHeight();
	}
	
	public static void setNumberOfPreFusionEnc() throws GameActionException{
		
		
		//NUM_ENC_TO_CLAIM=allEncampments.length/4;
		//some function of encampmentsLength,encampmentsCloserLength, rushDistance
		MapLocation HQ = mRC.senseHQLocation();
		MapLocation EnemyHQ = mRC.senseEnemyHQLocation();
		int w = Math.abs(HQ.x - EnemyHQ.x);
		int h = Math.abs(HQ.y - EnemyHQ.y);
		int A = Math.max(w, h);
		NUM_PREFUSION_ENC = (int) Math.ceil(A/10) + NUM_EXTRA_ENCAMPMENTS_BEFORE_FUSION;
		/*
		 * data for rush distance:
		 * 8978 - so huge
		 * 3242 - huge
		 * 1570 - moderate
		 * 800 - small
		 * 1170 - moderate
		 */
		
	}

	public static MapLocation findMedianSoldier() throws GameActionException {
		return findMedianSoldier(mRC.senseNearbyGameObjects(Robot.class, MAX_DIST_SQUARED, ARobot.mTeam));
	}
	
	public static MapLocation findMedianSoldier(Robot[] robots) throws GameActionException {
		int[] armyIndexes = new int[robots.length];
		int numArmy = 0;
		int roboLength =robots.length;
		for(int n=0; n<roboLength; ++n) {
			if(mRC.senseRobotInfo(robots[n]).type == RobotType.SOLDIER) {
				armyIndexes[numArmy++] = n;
			}
		}
		return findMedianSoldier(robots, armyIndexes, numArmy);
	}
	
	public static MapLocation findMedianSoldier(Robot[] robots, SoldierType[] soldierTypes) throws GameActionException {
		int[] armyIndexes = new int[robots.length];
		int numArmy = 0;
		int roboLength =robots.length;
		for(int n=0; n<roboLength; ++n) {
			if(soldierTypes[robots[n].getID()] == SoldierType.ARMY) {
				armyIndexes[numArmy++] = n;
			}
		}
		return findMedianSoldier(robots, armyIndexes, numArmy);
	}
	
	private static MapLocation findMedianSoldier(Robot[] robots, int[] armyIndexes, int numArmy) throws GameActionException {
		if(numArmy == 0) {
			return mRC.senseHQLocation();
		}
		int[] xs = new int[MEDIAN_SAMPLE_SIZE];
		int[] ys = new int[MEDIAN_SAMPLE_SIZE];
		for(int n=0; n<MEDIAN_SAMPLE_SIZE; ++n){
			Robot bot = robots[armyIndexes[ARobot.rand.nextInt(numArmy)]];
			RobotInfo info = mRC.senseRobotInfo(bot);
			xs[n] = info.location.x;
			ys[n] = info.location.y;
		}
		Arrays.sort(xs, 0, MEDIAN_SAMPLE_SIZE);
		Arrays.sort(ys, 0, MEDIAN_SAMPLE_SIZE);
		return new MapLocation(xs[MEDIAN_SAMPLE_SIZE/2], ys[MEDIAN_SAMPLE_SIZE/2]);
	}
	
	public static int locationToIndex(MapLocation l) {
		return l.x + l.y* NonConstants.Map_Width;
	}
	
	public static MapLocation indexToLocation(int index) {								
		return new MapLocation(index%NonConstants.Map_Width, index/NonConstants.Map_Width);
	}

	//Tests for mine in direction from a location
	public static boolean isMineDir(MapLocation mp, Direction d) {
		return (mRC.senseMine(mp.add(d)) != null);
	}
	//test for dangerous mines
	public static boolean isMineDirTrueDanger(MapLocation mp, Direction d) {
		Team t = mRC.senseMine(mp.add(d));
		return !(t == null || t == ARobot.mTeam);
	}

	public static boolean isMineDirDanger(MapLocation mp) throws GameActionException {				
		return (getMineStatus(mp) != MineStatus.DEFUSED);		
	}
	
	//Use these instead of just printing so we can disable easier	
	public static void print(String text)
	{
		System.out.println(text);
	}
	public static void print(int number)
	{
		System.out.println("" + number);
	}
}

class Pathfinder{
	
	private static int mapWidth, mapHeight, squareSize, gridWidth, gridHeight, distances[][], costs[][];
	private static MapLocation startSquare, mines[], parents[][];
	private static boolean visited[][];
	private static PriorityQueue<Pair<Integer, MapLocation>> que;
	private static boolean started = false, done = false;

	public static void startComputation(MapLocation start, boolean scout){
		if(start == null){

			System.out.println("in pathfinding but it was null");

		}
		else{
			mapWidth = Map_Width;
			mapHeight = Map_Height;
			squareSize = (int) Math.sqrt(mapWidth * mapHeight) / 10; //size of one pathfinding square?
			gridWidth = (mapWidth + squareSize - 1)/squareSize;//width of the grid in pathfinding squares?
			gridHeight = (mapHeight + squareSize - 1)/squareSize;//height of the grid in pathfinding squares?
			startSquare = new MapLocation(start.x/squareSize, start.y/squareSize); //pick the square that we are in?
			distances = new int[gridWidth][gridHeight];//distances to spots on grid
			costs = new int[gridWidth][gridHeight];//costs of a spot on the grid
			parents = new MapLocation[gridWidth][gridHeight];//I don't even know
			visited = new boolean[gridWidth][gridHeight];//places we've been already
			done = false;//are we finished?
			mines = mRC.senseNonAlliedMineLocations(start, MAX_DIST_SQUARED);//get a list of mines
			for(int i=0; i<gridWidth; i++)
				for(int j=0; j<gridHeight; j++){
					costs[i][j] = squareSize;
					//are we scouting? avoid the center
					if(scout)
					{
						//add a metric to make us avoid the center
						int derp = (int)Math.pow(((gridWidth+gridHeight))/(Math.pow(Math.abs(i-gridWidth/2),2)+Math.pow(Math.abs(j-gridHeight/2),2)+1),3);
						costs[i][j] += derp;
					}
					distances[i][j] = MAX_DIST_SQUARED*GameConstants.MINE_DEFUSE_DELAY;
					visited[i][j] = false;
					parents[i][j] = null;
					if(i == gridWidth - 1)
						costs[i][j] += GameConstants.MINE_DEFUSE_DELAY * (mapWidth%squareSize);
					if(j == gridHeight - 1)
						costs[i][j] += GameConstants.MINE_DEFUSE_DELAY * (mapHeight%squareSize);
				}
			int mineCost = (GameConstants.MINE_DEFUSE_DELAY + squareSize - 1)/squareSize;
			if(mRC.hasUpgrade(Upgrade.DEFUSION)) { 
				mineCost = (GameConstants.MINE_DEFUSE_DEFUSION_DELAY + squareSize - 1)/squareSize;
			}
			for(int i =mines.length;--i>=0;){
				costs[mines[i].x/squareSize][mines[i].y/squareSize] += mineCost;
			}
			distances[startSquare.x][startSquare.y] = 0;
			que = new PriorityQueue<Pair<Integer, MapLocation>>();
			que.add(Pair.of(0, startSquare));
			started = true;
		}
	}
	
	public static void continueComputation(){
		//System.out.println("b" + Clock.getBytecodesLeft());
		if(!started)
			return;
		while(!que.isEmpty()){
			if(Clock.getBytecodesLeft() < 2000)
				return;
			MapLocation loc = que.poll().b;
			if(visited[loc.x][loc.y])
				continue;
			visited[loc.x][loc.y] = true;
			int thisDist = distances[loc.x][loc.y] + costs[loc.x][loc.y];
			int nextX, nextY, dy;
			for(int dx=-1; dx<=1; ++dx)
				for(dy=-1; dy<=1; ++dy){
					nextX = loc.x + dx;
					nextY = loc.y + dy;
					if(nextX < 0 || nextX >= gridWidth || nextY < 0 || nextY >= gridHeight)
						continue;
					if(distances[nextX][nextY] > thisDist){
						distances[nextX][nextY] = thisDist;
						que.add(Pair.of(thisDist, new MapLocation(nextX, nextY)));
						parents[nextX][nextY] = loc; 
					}
				}
			//System.out.println("c" + Clock.getBytecodesLeft());
		}
		done = true;
	}
	
	public static MapLocation[] findWaypoints(MapLocation target){
		if(!done)
			return null;
		MapLocation targetSquare = new MapLocation(target.x/squareSize, target.y/squareSize);
		/*boolean mineMap[][] = new boolean[mapWidth][mapHeight];
		for(MapLocation mine:rc.senseNonAlliedMineLocations(target, MAX_DIST_SQUARED))
			mineMap[mine.x][mine.y] = true;*/
		LinkedList<MapLocation> waypoints = new LinkedList<MapLocation>();
		for(MapLocation square = targetSquare; square != null; square = parents[square.x][square.y]){
			/*System.out.println("d "+Clock.getBytecodesLeft());
			int avgX = 0, avgY = 0, numEmpty = 0;
			int minX = square.x*squareSize, minY = square.y*squareSize;
			int maxX = Math.min(minX + squareSize, mapWidth), maxY = Math.min(minY + squareSize, mapHeight);
			for(int x=minX; x<maxX; ++x)
				for(int y=minY; y<maxY; ++y)
					if(!mineMap[x][y]){
						avgX += x;
						avgY += y;
						++numEmpty;
					}
			if(numEmpty > 0){
				avgX /= numEmpty;
				avgY /= numEmpty;
			}
			else{
				avgX = square.x + squareSize/2;
				avgY = square.y + squareSize/2;
			}
			waypoints.addFirst(new MapLocation(avgX, avgY));*/
			int x = square.x*squareSize + squareSize/2;
			int y = square.y*squareSize + squareSize/2;
			if(x >= mapWidth)
				x = mapWidth - 1;
			if(y >= mapHeight)
				y = mapHeight - 1;
			waypoints.addFirst(new MapLocation(x, y));
		}
		MapLocation waypointsArray[] = waypoints.toArray(new MapLocation[0]);
		waypointsArray[waypointsArray.length - 1] = target;
		return waypointsArray;
	}

	public static boolean isStarted(){
		return started;
	}
	
	public static boolean isDone(){
		return done;
	}
	
	/*
	public static void adjustWaypoints(RobotController rc, MapLocation[] waypoints){
		int mapWidth = rc.getMapWidth();
		int mapHeight = rc.getMapHeight();
		boolean mineMap[][] = new boolean[mapWidth][mapHeight];
		for(MapLocation mine:rc.senseNonAlliedMineLocations(waypoints[0], MAX_DIST_SQUARED))
			mineMap[mine.x][mine.y] = true;
		for(int n=0; n<waypoints.length - 1; n++){
			if(!mineMap[waypoints[n].x][waypoints[n].y])
				continue;
			Direction dir = waypoints[n].directionTo(waypoints[n+1]);
			for (int d:Constants.testDirOrderAll) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				if(!mineMap[waypoints[n].x + lookingAtCurrently.dx][waypoints[n].y + lookingAtCurrently.dy]){
					waypoints[n] = waypoints[n].add(lookingAtCurrently);
					break;
				}
			}
		}
	}
	*/
	
}

class Pair<A extends Comparable<A>, B> implements Comparable<Pair<A, B>>{

	public final A a;
	public final B b;
	
	private Pair(A a, B b){
		this.a = a;
		this.b = b;
	}
	
    public static <A extends Comparable<A>, B> Pair<A, B> of(A a, B b) {
        return new Pair<A, B>(a, b);
    }
	
	@Override
	public int compareTo(Pair<A, B> o) {
		return a.compareTo(o.a);
		//return cmp == 0 ? b.compareTo(o.b) : cmp;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair))
			return false;
		return a.equals(((Pair<?, ?>)obj).a) && b.equals(((Pair<?, ?>)obj).b);
	}
	
}
class LocationAndIndex {
	public MapLocation mp;
	public int i;
	public LocationAndIndex(MapLocation aMp, int index) {
		this.mp = aMp; 
		this.i = index;
	}
}


