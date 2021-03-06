package nickTest0;

import battlecode.common.*;
/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously. 
 */
public class RobotPlayer {		

	private static RobotController rc;
	private static MapLocation rallyPoint;
	public static void run(RobotController myRC) {
		rc = myRC;
		rallyPoint = findRallyPoint();
		int lastRoundShot = 0;
		while (true) {
			try {				
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());												
						if (rc.canMove(dir))
							rc.spawn(dir);
					}
				} 
				else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
						if(!rc.senseEncampmentSquare(rc.getLocation())){
							//
							MapLocation[] encampmentsList = rc.senseEncampmentSquares(rc.getLocation(),10000,Team.NEUTRAL);
							System.out.println(encampmentsList.length);
							MapLocation[] encampments = new MapLocation[encampmentsList.length];
							int encampmentsLength = 0;
							for(int e = 0; e < encampmentsList.length; e++){
								if(encampmentsList[e].distanceSquaredTo(rc.senseEnemyHQLocation()) > encampmentsList[e].distanceSquaredTo(rc.senseHQLocation())){
									encampments[encampmentsLength] = encampmentsList[e];
									encampmentsLength++;
								}
							}
							if(encampmentsLength>0){
								int closestDist = 1000000;
								MapLocation closestEncamp=null;
								for (int i=0;i<encampmentsLength;i++){
									MapLocation encampPos = encampments[i];
									int dist = encampPos.distanceSquaredTo(rc.getLocation());
									boolean mightNotBeSomeoneOnSquare = true;
									if(rc.canSenseSquare(encampPos))
										mightNotBeSomeoneOnSquare = (null == rc.senseObjectAtLocation(encampPos));
									if (dist<closestDist && mightNotBeSomeoneOnSquare){
										closestDist = dist;
										closestEncamp = encampPos;
									}
								}
								MapLocation target = closestEncamp;
								
								if(closestEncamp!=null){
									Direction dir = rc.getLocation().directionTo(target);
									if(rc.canMove(dir)) {
										rc.move(dir);
										rc.setIndicatorString(0, "Last direction moved: "+dir.toString());
									}
								}
								else{
									goToLocation(rallyPoint);
								}
								
							}
							else{
								Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
								if(enemyRobots.length==0)
									goToLocation(rallyPoint);
								else{//someone spotted
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
							}
						}
						else{
							if(rc.getTeamPower() > rc.senseCaptureCost()){
							
								rc.captureEncampment(RobotType.ARTILLERY);
							}
						}
					}
				}
				else if (rc.getType() == RobotType.ARTILLERY){
					if (rc.isActive()){
						
						Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,63,rc.getTeam().opponent());
						MapLocation[] robotLocations = new MapLocation[enemyRobots.length];
						int i = 0;
						for(Robot bot : enemyRobots){
							
							RobotInfo irobot = rc.senseRobotInfo(bot);
							MapLocation rob = irobot.location;
							robotLocations[i] = rob;
							i++;
						}
						int maxIndex = 0;
						int maxAdjacent = 0;
						for(int j = 0; j < robotLocations.length;j++){
							int numberOfAdjacent = 0;
							for(int k = 0; k < robotLocations.length;k++){
								if(robotLocations[j].isAdjacentTo(robotLocations[k])){
									numberOfAdjacent++;
								}
								if(numberOfAdjacent>maxAdjacent){
									maxAdjacent = numberOfAdjacent;
									maxIndex = j;
								}
							}
						
						}
						if((maxAdjacent>0 || Clock.getRoundNum()-lastRoundShot > 5 + rc.getType().attackDelay) && rc.canAttackSquare(robotLocations[maxIndex])){
							rc.attackSquare(robotLocations[maxIndex]);
							lastRoundShot = Clock.getRoundNum();
						}

					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			// End turn
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

	private static MapLocation findRallyPoint() {
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x+3*ourLoc.x)/4;
		int y = (enemyLoc.y+3*ourLoc.y)/4;
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}
}


