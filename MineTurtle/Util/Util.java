package MineTurtle.Util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import static MineTurtle.Util.Constants.*;


public class Util {
	public static boolean goToLocation(RobotController rc, MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		
		if (rc.isActive() && dist>0) {
			Direction dir = rc.getLocation().directionTo(whereToGo);
			for (int d:Constants.testDirOrderFrontSide) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+NUM_DIR)%NUM_DIR];
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
