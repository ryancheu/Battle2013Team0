package turtle;

import battlecode.common.*;

public class RobotPlayer{
	
	private static RobotController rc;
	
	private static int radioRallyX=1214, radioRallyY=1215;
	
	public static void run(RobotController myRC){
		rc = myRC;
		if(rc.getTeam() == Team.A){
			radioRallyX *= 2;
			radioRallyY *= 2;
		}
		while(true){
			try{
				if (rc.getType()==RobotType.SOLDIER && rc.isActive()){
					Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
					if(enemyRobots.length==0){//no enemies nearby
						if(Math.random()<0.05 && rc.senseMine(rc.getLocation())==null)
							rc.layMine();
						else
							goToLocation(findRallyPoint());
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
						goToLocation(closestEnemy);
					}
				}else if(rc.getType() == RobotType.HQ){
					hqCode();
				}
			}catch (Exception e){
				System.out.println("caught exception before it killed us:");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	private static void goToLocation(MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			int[] directionOffsets = {0,1,-1,2,-2};
			lookAround: for (int d:directionOffsets){
				Direction lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
				if(rc.canMove(lookingAtCurrently)){
					MapLocation newLoc = rc.getLocation().add(lookingAtCurrently);
					Team mineOwner = rc.senseMine(newLoc); 
					if(mineOwner != null && mineOwner != rc.getTeam())
						rc.defuseMine(newLoc);
					else
						rc.move(lookingAtCurrently);
					break lookAround;
				}
			}
		}
	}

	private static MapLocation findRallyPoint() throws GameActionException {
		/*MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x+3*ourLoc.x)/4;
		int y = (enemyLoc.y+3*ourLoc.y)/4;*/
		int x = rc.readBroadcast(radioRallyX);
		int y = rc.readBroadcast(radioRallyY);
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}
	
	private static void setRallyPoint(MapLocation rally) throws GameActionException {
		rc.broadcast(radioRallyX, rally.x);
		rc.broadcast(radioRallyY, rally.y);
	}

	private static boolean isBeingNuked = false;
	
	public static void hqCode() throws GameActionException{
		if (rc.isActive()) {
			int numOwnSoldiers = rc.senseNearbyGameObjects(Robot.class, 1000000, rc.getTeam()).length;
			if(numOwnSoldiers < 25){
				// Spawn a soldier
				Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				if (rc.canMove(dir))
					rc.spawn(dir);
				else{
					for(Direction d:Direction.values())
						if((d.dx != 0 || d.dy != 0) && rc.canMove(d)){
							rc.spawn(d);
							break;
						}
				}
			}
			else{
				//rc.researchUpgrade(Upgrade.NUKE);
			}
			if(!isBeingNuked && rc.checkResearchProgress(Upgrade.NUKE) <= 200 && rc.senseEnemyNukeHalfDone())
				isBeingNuked = true;
			if(isBeingNuked){
				int avgX = 0, avgY = 0, numSoldiers = 0;
				Robot[] ourRobots = rc.senseNearbyGameObjects(Robot.class, 1000000, rc.getTeam());
				for(Robot bot:ourRobots){
					RobotInfo info = rc.senseRobotInfo(bot);
					if(info.type == RobotType.SOLDIER){
						numSoldiers ++;
						avgX += info.location.x;
						avgY += info.location.y;
					}
				}
				avgX /= numSoldiers;
				avgY /= numSoldiers;
				MapLocation enemyLoc = rc.senseEnemyHQLocation();
				setRallyPoint(new MapLocation((9*avgX + enemyLoc.x)/10, (9*avgY + enemyLoc.y)/10));
			}
			else
				setRallyPoint(rc.getLocation());
		}
	}
	
}