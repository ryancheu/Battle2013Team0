package MineTurtle.Util;

import java.util.LinkedList;
import java.util.PriorityQueue;

import team116.Util.Constants;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import static MineTurtle.Util.Constants.*;
import static team116.Util.Constants.NUM_DIR;


public class Util {
	
	//try to go to a location, argument as to whether to defuse mines along the way
	public static boolean goToLocation(RobotController rc, MapLocation whereToGo) throws GameActionException {
		return goToLocation(rc,whereToGo,false);
	}
	public static boolean goToLocation(RobotController rc, MapLocation whereToGo, boolean defuseMines) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		
		if (rc.isActive() && dist>0) {
			Direction dir = rc.getLocation().directionTo(whereToGo);
			for (int d:Constants.testDirOrderFrontSide) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
				if(rc.canMove(lookingAtCurrently) && (defuseMines || !isMineDir(rc,rc.getLocation(),lookingAtCurrently))) {
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
		//System.out.println("getDistances: start: "+start+", target: "+target);
		int mapWidth = rc.getMapWidth();
		int mapHeight = rc.getMapHeight();
		int squareSize = (int) Math.sqrt(mapWidth * mapHeight) / 10;
		System.out.println(squareSize);
		int gridWidth = (mapWidth + squareSize - 1)/squareSize;
		int gridHeight = (mapHeight + squareSize - 1)/squareSize;
		MapLocation startSquare = new MapLocation(start.x/squareSize, start.y/squareSize);
		MapLocation targetSquare = new MapLocation(target.x/squareSize, target.y/squareSize);
		int distances[][] = new int[gridWidth][gridHeight];
		int costs[][] = new int[gridWidth][gridHeight];
		MapLocation parents[][] = new MapLocation[gridWidth][gridHeight];
		boolean visited[][] = new boolean[gridWidth][gridHeight];
		MapLocation[] mines = rc.senseNonAlliedMineLocations(start, MAX_DIST_SQUARED);
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
		PriorityQueue<Pair<Integer, MapLocation>> que = new PriorityQueue<Pair<Integer, MapLocation>>();
		que.add(Pair.of(0, startSquare));
		while(!que.isEmpty()){
			MapLocation loc = que.poll().b;
			if(loc.x == targetSquare.x && loc.y == targetSquare.y)
				break;
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
						que.add(Pair.of(thisDist +
								squareSize*Math.max(Math.abs(targetSquare.x - nextX),Math.abs(targetSquare.y - nextY)),
								new MapLocation(nextX, nextY)));
						parents[nextX][nextY] = loc; 
					}
				}
		}
		LinkedList<MapLocation> wayPoints = new LinkedList<MapLocation>();
		for(MapLocation square = targetSquare; square != null; square = parents[square.x][square.y])
			wayPoints.addFirst(new MapLocation(square.x*squareSize+squareSize/2, square.y*squareSize+squareSize/2));
		for(MapLocation p:wayPoints.toArray(new MapLocation[0]))
			System.out.print(p+" ");
		System.out.println();
		return wayPoints.toArray(new MapLocation[0]);
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
		if(closestWaypoint < waypoints.length-1)
			return waypoints[closestWaypoint + 1];
		else
			return waypoints[waypoints.length - 1];
	}
	
	public static boolean checkIDs(RobotController rc) throws GameActionException{
		int [] IDs = new int[NUM_ROBOTS_TO_CHECK_ID];
		int min = MAX_DIST_SQUARED;//big number
		int minIndex = 0;
		for(int i = 0; i < NUM_ROBOTS_TO_CHECK_ID; ++i){
			IDs[i] = rc.readBroadcast(LAST_FOUR_BOT_ID_RAD_CHAN_START + i);
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
		
	public static void print(String text)
	{
		System.out.println(text);
	}
	public static void print(int number)
	{
		System.out.println("" + number);
	}
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
