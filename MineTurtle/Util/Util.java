package MineTurtle.Util;

import java.util.LinkedList;

import java.util.PriorityQueue;




import MineTurtle.Robots.ARobot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Team;
import static MineTurtle.Robots.ARobot.mRC;
import static MineTurtle.Util.Constants.*;

public class Util {
	
	//try to go to a location, argument as to whether to defuse mines along the way
	public static boolean goToLocation(MapLocation whereToGo) throws GameActionException {
		return goToLocation(whereToGo,true);
	}
	public static boolean goToLocation(MapLocation whereToGo, boolean defuseMines) throws GameActionException {
		int dist = mRC.getLocation().distanceSquaredTo(whereToGo);
		
		if (mRC.isActive() && dist>0) {
			Direction dir = mRC.getLocation().directionTo(whereToGo);
			for (int d:Constants.testDirOrderFrontSide) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				if(mRC.canMove(lookingAtCurrently) && (defuseMines || !isMineDir(mRC.getLocation(),lookingAtCurrently,true))) {
					MapLocation newLoc = mRC.getLocation().add(lookingAtCurrently);
					Team mineOwner = mRC.senseMine(newLoc); 
					if(mineOwner != null && mineOwner != mRC.getTeam()) {	 
						mRC.defuseMine(newLoc);
					}
					else {
						mRC.move(lookingAtCurrently);
					}
					return true;
				}
			}
		}
		return false;
	}

	public static MapLocation[] findWaypoints(MapLocation start, MapLocation target){
		if(!Pathfinder.isStarted())
			Pathfinder.startComputation(start);
		else if(!Pathfinder.isDone())
			Pathfinder.continueComputation();
		else
			return Pathfinder.findWaypoints(target);
		return null;
	}
	
	public static void precomputeWaypoints(MapLocation start){
		if(!Pathfinder.isStarted())
			Pathfinder.startComputation(start);
		else if(!Pathfinder.isDone())
			Pathfinder.continueComputation();
	}
	
	public static MapLocation findNextWaypoint(MapLocation[] waypoints){
		return findNextWaypoint(waypoints, mRC.getLocation());
	}
	
	public static MapLocation findNextWaypoint(MapLocation[] waypoints, MapLocation from){
		int closestWaypoint = -1;
		int closestWaypointDistance = MAX_DIST_SQUARED;
		for(int i=0; i<waypoints.length; i++){
			MapLocation waypoint = waypoints[i];
			int dist = from.distanceSquaredTo(waypoint); 
			if(dist <= closestWaypointDistance){
				closestWaypoint = i;
				closestWaypointDistance = dist;
			}
		}
		MapLocation prev, current = waypoints[closestWaypoint], next;
		if(closestWaypoint < waypoints.length-1)
			next = waypoints[closestWaypoint + 1];
		else
			return current;
		if(closestWaypoint > 0)
			prev = waypoints[closestWaypoint - 1];
		else
			return next;
		int prevDist = from.distanceSquaredTo(prev);
		int nextDist = from.distanceSquaredTo(next);
		if(prevDist > nextDist || closestWaypointDistance < prevDist/4)
			return next;
		else
			return current;
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
	
	public static void setNumberOfEncampments() throws GameActionException{
		//should use number of encampments, number of closer encampments, 
		MapLocation[] allEncampments = mRC.senseEncampmentSquares(mRC.getLocation(), MAX_DIST_SQUARED, Team.NEUTRAL);
		int encampmentsLength = allEncampments.length;
		int encampmentsCloserLength = 0;
		int rushDistance = mRC.senseHQLocation().distanceSquaredTo(mRC.senseEnemyHQLocation());
		MapLocation[] encampmentsCloser = new MapLocation[allEncampments.length];
		
		for(int e = 0; e < allEncampments.length; e++){
			if(allEncampments[e].distanceSquaredTo(mRC.senseEnemyHQLocation()) > allEncampments[e].distanceSquaredTo(mRC.senseHQLocation())){
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
		
		if(NUM_ENC_TO_CLAIM > 15)
			NUM_ENC_TO_CLAIM = 15;
		
		/*
		 * data for rush distance:
		 * 8978 - so huge
		 * 3242 - huge
		 * 1570 - moderate
		 * 800 - small
		 * 1170 - moderate
		 */
		
	}
	
	public static int locationToIndex(MapLocation l) {
		return mRC.getMapWidth() * l.y + l.x;
	}

	public static MapLocation indexToLocation(int index) {
		return new MapLocation(index % mRC.getMapWidth(), index / mRC.getMapWidth());
	}
	
	//Tests for mine in direction from a location
	public static boolean isMineDir(MapLocation mp, Direction d) {
		return (mRC.senseMine(mp.add(d)) != null);
	}
	//Tests for mine in direction from a location
	public static boolean isMineDir(MapLocation mp, Direction d, boolean dangerOnly) {
		if ( dangerOnly )
		{
			Team mineTeam = mRC.senseMine(mp.add(d));
			return (mineTeam != mRC.getTeam()) && mineTeam != null;
		}		
		return mRC.senseMine(mp.add(d)) != null;
	}
	
	
	//returns the number of enemy/allied robots if a robot were to go in each direction.  
	//number of allied is in 10s place, number of enemies is in 1s, a 100 means the direction is blocked
	public static int[] getNeighborStats() throws GameActionException {		
		Robot[] NearbyRobots =  mRC.senseNearbyGameObjects(Robot.class, 2*2 + 2*2); //2 in either direction
		
		MapLocation roboLoc = mRC.getLocation();				
		int[] eachDirectionStats = { 0,0,0,0,0,0,0,0 }; // This is NUM_DIR zeros
		MapLocation[] directionLocs = new MapLocation[NUM_DIR];
		for (int i = 0; i < NUM_DIR; i++) {
			directionLocs[i] = roboLoc.add(Direction.values()[i]);
		}
		int tempDist;
		for ( Robot r : NearbyRobots) {
			for ( int i = 0; i < NUM_DIR; ++i ) {
				if ( (tempDist = mRC.senseRobotInfo(r).location.distanceSquaredTo(directionLocs[i])) < 2 ) { //means directly next to us
					
					if ( tempDist == 0 ) {
						eachDirectionStats [i] += 100;						
					}
					if (r.getTeam() == mRC.getTeam()) { 
						eachDirectionStats[i] += 10;  //Do we really need this data?
					}
					else {
						eachDirectionStats[i] += 1;
					}
				}
			}
		}
		return eachDirectionStats;
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
	
	public static void startComputation(MapLocation start){
		mapWidth = mRC.getMapWidth();
		mapHeight = mRC.getMapHeight();
		squareSize = (int) Math.sqrt(mapWidth * mapHeight) / 10;
		System.out.println(squareSize);
		gridWidth = (mapWidth + squareSize - 1)/squareSize;
		gridHeight = (mapHeight + squareSize - 1)/squareSize;
		startSquare = new MapLocation(start.x/squareSize, start.y/squareSize);
		distances = new int[gridWidth][gridHeight];
		costs = new int[gridWidth][gridHeight];
		parents = new MapLocation[gridWidth][gridHeight];
		visited = new boolean[gridWidth][gridHeight];
		done = false;
		mines = mRC.senseNonAlliedMineLocations(start, MAX_DIST_SQUARED);
		for(int i=0; i<gridWidth; i++)
			for(int j=0; j<gridHeight; j++){
				costs[i][j] = squareSize;
				distances[i][j] = MAX_DIST_SQUARED*GameConstants.MINE_DEFUSE_DELAY;
				visited[i][j] = false;
				parents[i][j] = null;
				if(i == gridWidth - 1)
					costs[i][j] += GameConstants.MINE_DEFUSE_DELAY * (mapWidth%squareSize);
				if(j == gridHeight - 1)
					costs[i][j] += GameConstants.MINE_DEFUSE_DELAY * (mapHeight%squareSize);
			}
		for(MapLocation mine:mines){
			costs[mine.x/squareSize][mine.y/squareSize] += GameConstants.MINE_DEFUSE_DELAY/squareSize;
		}
		distances[startSquare.x][startSquare.y] = 0;
		que = new PriorityQueue<Pair<Integer, MapLocation>>();
		que.add(Pair.of(0, startSquare));
		started = true;
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
			waypoints.addFirst(new MapLocation(square.x*squareSize + squareSize/2, square.y*squareSize + squareSize/2));
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
