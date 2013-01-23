package BaseBot.Robots.Types;



import static BaseBot.Robots.ARobot.mRC;
import static BaseBot.Util.Constants.*;
import static BaseBot.Util.NonConstants.*;
import static BaseBot.Util.Util.*;

import BaseBot.Robots.ARobot;
import BaseBot.Robots.SoldierRobot;
import BaseBot.Robots.SoldierRobot.SoldierState;
import BaseBot.Robots.SoldierRobot.SoldierType;
import BaseBot.Util.NonConstants;
import BaseBot.Util.RadioChannels;
import battlecode.common.*;

public class SoldierScoutType {

private static MapLocation[] waypoints;
private static MapLocation dest;
private static boolean foundPathToEnemy = false;
private static int enemyPathLastComputed = -SCOUT_RECOMPUTE_PATH_INTERVAL;
private static int timeout = 0;
private static MapLocation firstRallyPoint;

public static void run() throws GameActionException {

if ( mRC.isActive() ) {
switch(SoldierRobot.getState())
{
case COMPUTE_SCOUT_PATH: {
computeScoutPath();
break;
}
case SCOUT: {
scoutState();
break;
}
default:
break;	
}
}

if(waypoints == null && dest != null)
waypoints = findWaypoints(foundPathToEnemy ? mRC.getLocation() : firstRallyPoint,
dest);
}

private static void pickDestination() throws GameActionException {
if(Clock.getRoundNum() - enemyPathLastComputed > SCOUT_RECOMPUTE_PATH_INTERVAL) {
dest = SoldierRobot.enemyHQLoc;
foundPathToEnemy = false;
int value = ARobot.mRadio.readChannel(RadioChannels.HQ_ATTACK_RALLY_START);
if((value & FIRST_BYTE_KEY_MASK) == FIRST_BYTE_KEY) {
firstRallyPoint = indexToLocation(value ^ FIRST_BYTE_KEY);
}
else {
firstRallyPoint = mRC.senseHQLocation();
}
}
else {
MapLocation[] encampments = mRC.senseEncampmentSquares(mRC.senseEnemyHQLocation(), (Map_Height/2)*(Map_Height/2) + (Map_Width/2)*(Map_Width/2), null);	
dest = encampments[ARobot.rand.nextInt(encampments.length)];
/*
if(dest.distanceSquaredTo(mRC.getLocation())
> dest.distanceSquaredTo(mRC.senseEnemyHQLocation())) {
encampments = mRC.senseAlliedEncampmentSquares();
if (encampments.length > 0)
dest = encampments[ARobot.rand.nextInt(encampments.length)];
else
dest = SoldierRobot.HQLoc;
}
*/
if(mRC.canSenseSquare(dest))
dest = dest.add(dest.directionTo(SoldierRobot.enemyHQLoc), SCOUT_DIST);
timeout = SCOUT_RECOMPUTE_PATH_INTERVAL;
}
}

private static void computeScoutPath() throws GameActionException {
if(waypoints != null){
if(!foundPathToEnemy) {
SoldierRobot.mRadio.writeChannel(RadioChannels.NUM_SCOUT_WAYPOINTS, waypoints.length);
for(int n=0; n<waypoints.length; ++n){
SoldierRobot.mRadio.writeChannel(RadioChannels.SCOUT_WAYPOINTS_START + n, locationToIndex(waypoints[n]));
}
waypoints = null;
dest = null;
foundPathToEnemy = true;
enemyPathLastComputed = Clock.getRoundNum();
}
else {
SoldierRobot.switchState(SoldierState.SCOUT);
return;
}
}

if(dest == null) {
pickDestination();
}

Robot[] nearbyEnemies = mRC.senseNearbyGameObjects(Robot.class,
RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS, SoldierRobot.mEnemy);

if(nearbyEnemies.length > 0) {
runAway(nearbyEnemies);
return;
}

// Lay mines until we find the waypoints
if (mRC.senseMine(mRC.getLocation()) == null) {
mRC.layMine();
return;
}

// Try going towards destination directly
goToLocation(dest);
}

private static void scoutState() throws GameActionException {

Robot[] nearbyEnemies = mRC.senseNearbyGameObjects(Robot.class,
RobotType.SOLDIER.sensorRadiusSquared + GameConstants.VISION_UPGRADE_BONUS, SoldierRobot.mEnemy);

if(nearbyEnemies.length > 0) {
runAway(nearbyEnemies);
return;
}
if (SoldierRobot.mRadio.readChannel(RadioChannels.ENEMY_FASTER_NUKE) == 0 ) {
if((nearbyEnemies.length == 0 && mRC.getLocation().distanceSquaredTo(dest) < SCOUT_RAD_SQUARED)
|| --timeout <= 0) {
waypoints = null;
dest = null;
SoldierRobot.switchState(SoldierState.COMPUTE_SCOUT_PATH);
return;
}
goToLocation(findNextWaypoint(waypoints));
mRC.setIndicatorString(2, findNextWaypoint(waypoints).toString());
}
else {
if((nearbyEnemies.length == 0 && mRC.getLocation().distanceSquaredTo(dest) <= 0)) {
waypoints = null;
dest = null;
SoldierRobot.switchType(SoldierType.ARMY);
SoldierRobot.switchState(SoldierState.GOTO_RALLY);
return;
}
goToLocation(findNextWaypoint(waypoints));
mRC.setIndicatorString(2, findNextWaypoint(waypoints).toString());
}

}

private static void runAway(Robot[] nearbyEnemies) throws GameActionException {
int closestDist = MAX_DIST_SQUARED;
int tempDist;
RobotInfo tempRobotInfo;
MapLocation closestEnemy=null;
for (Robot arobot:nearbyEnemies) {
tempRobotInfo = mRC.senseRobotInfo(arobot);
if(tempRobotInfo.type != RobotType.SOLDIER)
continue;
tempDist = tempRobotInfo.location.distanceSquaredTo(mRC.getLocation());
if (tempDist<closestDist) {
closestDist = tempDist;
closestEnemy = tempRobotInfo.location;
}
}
if(closestEnemy != null){
// Run away from enemy soldiers
goToLocation(mRC.getLocation().add(mRC.getLocation().directionTo(closestEnemy).opposite()), false);
mRC.setIndicatorString(2, "Run away!");
return;
}
else {
// Attack enemy encampments / HQs
MapLocation tempLoc;
for (Robot arobot:nearbyEnemies) {
tempLoc = mRC.senseLocationOf(arobot);
tempDist = mRC.getLocation().distanceSquaredTo(tempLoc);
if (tempDist < closestDist) {
closestDist = tempDist;
closestEnemy = tempLoc;
}
}
goToLocation(closestEnemy, true);
mRC.setIndicatorString(2, "Attack!");
return;
}
}
}