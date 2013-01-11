package MineTurtle.Util;

import java.util.LinkedList;
import java.util.PriorityQueue;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import static MineTurtle.Util.Constants.*;

public class Util {
	
	//try to go to a location, argument as to whether to defuse mines along the way
	public static boolean goToLocation(RobotController rc, MapLocation whereToGo) throws GameActionException {
		return goToLocation(rc,whereToGo,true);
	}
	public static boolean goToLocation(RobotController rc, MapLocation whereToGo, boolean defuseMines) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		
		if (rc.isActive() && dist>0) {
			Direction dir = rc.getLocation().directionTo(whereToGo);
			for (int d:Constants.testDirOrderFrontSide) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				if(rc.canMove(lookingAtCurrently) && (defuseMines || !isMineDir(rc,rc.getLocation(),lookingAtCurrently,true))) {
					MapLocation newLoc = rc.getLocation().add(lookingAtCurrently);
					Team mineOwner = rc.senseMine(newLoc); 
					if(mineOwner != null && mineOwner != rc.getTeam()) {						 
						rc.defuseMine(newLoc);
					}
					else {
						rc.move(lookingAtCurrently);
					}
					return true;
				}
			}
		}
		return false;
	}

	public static MapLocation[] findWaypoints(RobotController rc, MapLocation start, MapLocation target){
		if(!Pathfinder.isStarted())
			Pathfinder.startComputation(rc, start);
		else if(!Pathfinder.isDone())
			Pathfinder.continueComputation();
		else
			return Pathfinder.findWaypoints(rc, target);
		return null;
	}
	
	public static void precomputeWaypoints(RobotController rc, MapLocation start){
		if(!Pathfinder.isStarted())
			Pathfinder.startComputation(rc, start);
		else if(!Pathfinder.isDone())
			Pathfinder.continueComputation();
	}
	
	public static MapLocation findNextWaypoint(RobotController rc, MapLocation[] waypoints){
		int closestWaypoint = -1;
		int closestWaypointDistance = MAX_DIST_SQUARED;
		for(int i=0; i<waypoints.length; i++){
			MapLocation waypoint = waypoints[i];
			int dist = rc.getLocation().distanceSquaredTo(waypoint); 
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
		int prevDist = rc.getLocation().distanceSquaredTo(prev);
		int nextDist = rc.getLocation().distanceSquaredTo(next);
		if(prevDist > nextDist || closestWaypointDistance < prevDist/4)
			return next;
		else
			return current;
	}
	
	public static boolean checkIDs(RobotController rc, Radio mRadio) throws GameActionException{
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
	
	public static void setNumberOfEncampments(RobotController rc) throws GameActionException{
		//should use number of encampments, number of closer encampments, 
		MapLocation[] allEncampments = rc.senseEncampmentSquares(rc.getLocation(), MAX_DIST_SQUARED, Team.NEUTRAL);
		int encampmentsLength = allEncampments.length;
		int encampmentsCloserLength = 0;
		int rushDistance = rc.senseHQLocation().distanceSquaredTo(rc.senseEnemyHQLocation());
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
	
	public static int locationToIndex(RobotController rc,MapLocation l) {
		return rc.getMapWidth() * l.y + l.x;
	}

	public static MapLocation indexToLocation(RobotController rc,int index) {
		return new MapLocation(index % rc.getMapWidth(), index / rc.getMapWidth());
	}
	
	//Tests for mine in direction from a location
	public static boolean isMineDir(RobotController rc, MapLocation mp, Direction d) {
		return (rc.senseMine(mp.add(d)) != null);
	}
	//Tests for mine in direction from a location
	public static boolean isMineDir(RobotController rc, MapLocation mp, Direction d, boolean dangerOnly) {
		if ( dangerOnly )
		{
			Team mineTeam = rc.senseMine(mp.add(d));
			return (mineTeam != rc.getTeam()) && mineTeam != null;
		}		
		return rc.senseMine(mp.add(d)) != null;
	}
		
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
	
	public static void startComputation(RobotController rc, MapLocation start){
		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();
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
		mines = rc.senseNonAlliedMineLocations(start, MAX_DIST_SQUARED);
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
	
	public static MapLocation[] findWaypoints(RobotController rc, MapLocation target){
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
