package rallyBot;


import battlecode.common.*;

public class RobotPlayer{
	private static final int ATTACK_ROUND = 200;
	private static RobotController rc;
	private static MapLocation rallyPoint;
	
	public static void run(RobotController myRC){
		rc = myRC;
		rallyPoint = findRallyPoint();
		while(true){
			try{
				if (rc.getType()==RobotType.SOLDIER){
					Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
					if(enemyRobots.length==0){//no enemies nearby
						if (Clock.getRoundNum()<ATTACK_ROUND){
							goToLocation(rallyPoint, true);
						}else{
							goToLocation(rc.senseEnemyHQLocation(), true);
						}
					}else{//someone spotted
						int closestDist = 1000000;
						MapLocation closestEnemy=null;
						for (int i=0;i<enemyRobots.length;i++){
							Robot arobot = enemyRobots[i];
							RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
							int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
							if (dist<closestDist){
								closestDist = dist;
								closestEnemy = arobotInfo.location;
							}
						}
						goToLocation(closestEnemy, false);
					}
				}else{
					hqCode();
				}
			}catch (Exception e){
				System.out.println("caught exception before it killed us:");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	public static boolean goToLocation(MapLocation whereToGo, boolean defuseMines) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		int[] testDirOrderFrontSide = { 0, 1, -1, 2, -2 };
		if (rc.isActive() && dist>0) {
			Direction dir = rc.getLocation().directionTo(whereToGo);
			for (int d:testDirOrderFrontSide) {
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
				if(rc.canMove(lookingAtCurrently) && (defuseMines || !isMineDir(rc.getLocation(),lookingAtCurrently,true))) {
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
	public static boolean isMineDir(MapLocation mp, Direction d, boolean dangerOnly) {
		if ( dangerOnly )
		{
			Team mineTeam = rc.senseMine(mp.add(d));
			return (mineTeam != rc.getTeam()) && mineTeam != null;
		}		
		return rc.senseMine(mp.add(d)) != null;
	}

	private static MapLocation findRallyPoint() {
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x+3*ourLoc.x)/4;
		int y = (enemyLoc.y+3*ourLoc.y)/4;
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}

	public static void hqCode() throws GameActionException{
		if (rc.isActive()) {
			// Spawn a soldier
			Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			if (rc.canMove(dir))
				rc.spawn(dir);
		}
	}
	
}