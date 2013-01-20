
package BaseBot.Util;

import battlecode.common.Direction;

public class EconConstants {
	
	public static final int ORGANIZE_INTERVAL = 10;
	public static final int ORGANIZE_ROUNDS = 3;
	
	public static final int ATTACK_ROUND = 2000;
	
	// HQ Consts
    public static final int LATE_GAME = 450;
	public static final int NUM_ROBOTS_TO_CHECK_ID = 4;
	public static final double CHANCE_OF_DEFUSING_ENEMY_MINE_CONST = 0.5;
	public static final double CHANCE_OF_DEFUSING_NEUTRAL_MINE_CONST = 0.3;
	public static final int NUM_MINERS = 1;
	public static final int NUM_MINERS_WITH_PICKAXE = 0;
	public static final int NUM_SCOUTS = 1;
	public static final int NUM_ARMY = 20;
	public static final int NUM_ARMY_NO_FUSION = 15;
	public static final int NUM_ARMY_WITH_FUSION = 50;
	public static final int NUM_ARMY_BEFORE_RETREAT = 15;
	public static final int NUM_ARMY_BEFORE_ATTACK = 20;
	public static final int NUM_ARMY_BEFORE_ATTACK_WITH_NUKE = 10;
	public static final int NUM_ROBOT_TO_SPAWN = NUM_MINERS + NUM_SCOUTS + NUM_ARMY_NO_FUSION;
	
	public static final int PREFUSION_POWER_RESERVE = 150;
	public static final int POWER_RESERVE = 400;
	public static final int RUSH_NUKE_TIME = 30;
	public static final double RATIO_OF_SUPPLIERS_OVER_GENERATORS_CONST = 6.0/3.0;


	public static final int NUM_ROTATED_CHANNELS = 1000;
	public static final int NUM_DUPLICATED_CHANNELS = 10000;
	public static final int RAD_NUM_DUPLICATIONS = 3;
	public static final int RAD_ROTATION_START = NUM_ROTATED_CHANNELS + RAD_NUM_DUPLICATIONS*NUM_DUPLICATED_CHANNELS;
	public static final int RAD_ROTATION_INTERVAL = 7;
	
	
	//SOLDIER WAY POINT RALLY CHAN ORGANIZATION:
	//12 bits - round waypoints last updated
	//14 bits - start waypoint transmission channel
	//6 bits - number of waypoints in rally

	
	public static final int RALLY_RAD_SQUARED = 16;
	
	public static final int LAST_ROUND_SHOT_DELAY_CONST = 5;

	public static final int SOLDIER_ENEMY_CHECK_RAD_CONST = 10;
	
	public static final int SOLDIER_RALLY_RAD_CONST = 32;
	
	public static final float SOLDIER_OUTNUMBER_MULTIPLIER_CONST = 1.1f;
	public static final int SOLDIER_RUN_HEALTH_CONST = 10;
	public static final int SOLDIER_RUN_EVENTUALLY_HEALTH_CONST = 25;
	public static final int SOLDIER_RETURN_HEALTH_CONST = 38;
	public static final int SOLDIER_BATTLE_ENEMY_CHECK_RAD_CONST = 100;
	
	

	public static final int SOLDIER_ATTACK_RAD_CONST = 40;
	public static final int SOLDIER_JOIN_ATTACK_RAD_CONST = 100;
	
	
	public static final int SCOUT_RAD_SQUARED_CONST = 10;
	public static final int SCOUT_DIST_CONST = 5;
	
	
	public static final int HQ_ENTER_RUSH_RAD = 2;
	public static final int HQ_RUSH_TIMEOUT = 100;
	
	public static final int SCOUT_RECOMPUTE_PATH_INTERVAL_CONST = 100;
	
	

	public static final int MAX_POSSIBLE_SOLDIERS = 2000;
	
	
	
}
