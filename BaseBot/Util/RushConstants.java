package BaseBot.Util;

public class RushConstants {
	// Player specific	
	
	public static final int ORGANIZE_INTERVAL = 10;
	public static final int ORGANIZE_ROUNDS = 3;
	
	public static final int ATTACK_ROUND = 0;
	

	public static final int NUM_POINT_SCOUTS=0;
	//END POINT SCOUT VARIABLES
	// HQ Consts
    public static final int LATE_GAME = 1000;
	public static final int NUM_ROBOTS_TO_CHECK_ID = 4;
	public static final double CHANCE_OF_DEFUSING_ENEMY_MINE_CONST = 0.5;
	public static final double CHANCE_OF_DEFUSING_NEUTRAL_MINE_CONST = 0.3;
	public static final int MAX_NUM_ENC_TO_CLAIM = 15;
	public static final int NUM_MINERS = 0;
	public static final int NUM_MINERS_WITH_PICKAXE = 0;
	public static final int NUM_SCOUTS = 0;
	public static final int NUM_ARMY = 20;
	public static final int NUM_ARMY_NO_FUSION = 15;
	public static final int NUM_ARMY_WITH_FUSION = 50;
	public static final int NUM_ARMY_BEFORE_RETREAT = 0;
	public static final int NUM_ARMY_BEFORE_ATTACK = 0;
	public static final int NUM_ARMY_BEFORE_ATTACK_WITH_NUKE = 0;
	public static final int NUM_ROBOT_TO_SPAWN = NUM_MINERS + NUM_SCOUTS + NUM_ARMY_NO_FUSION;
	public static final int PREFUSION_POWER_RESERVE = 70;
	public static final int POWER_RESERVE = 400;
	public static final int RUSH_NUKE_TIME = 30;
	public static final double RATIO_OF_SUPPLIERS_OVER_GENERATORS_CONST = 7f/3f;
	public static final double NUM_GENERATORSUPPLIER_PER_ARTILLERY_CONST = 5;


	public static final int NUM_ROTATED_CHANNELS = 1000;
	public static final int NUM_DUPLICATED_CHANNELS = 10000;
	public static final int RAD_NUM_DUPLICATIONS = 3;
	public static final int RAD_ROTATION_START = NUM_ROTATED_CHANNELS + RAD_NUM_DUPLICATIONS*NUM_DUPLICATED_CHANNELS;
	public static final int RAD_ROTATION_INTERVAL = 7;
	
	
	
	public static final int RALLY_RAD_SQUARED = 16;
	
	public static final int LAST_ROUND_SHOT_DELAY_CONST = 5;

	public static final int SOLDIER_ENEMY_CHECK_RAD_CONST = 10;
	
	public static final int SOLDIER_RALLY_RAD_CONST = 32;
	
	public static final float SOLDIER_OUTNUMBER_MULTIPLIER_CONST = 1.1f;
	public static final int SOLDIER_RUN_HEALTH_CONST = 0;
	public static final int SOLDIER_RUN_EVENTUALLY_HEALTH_CONST = 20;
	public static final int SOLDIER_RETURN_HEALTH_CONST = 38;
	public static final int SOLDIER_BATTLE_ENEMY_CHECK_RAD_CONST = 100;
	
	
	public static final int SOLDIER_ATTACK_RAD_CONST = 50;
	public static final int SOLDIER_JOIN_ATTACK_RAD_CONST = 100;
	public static final int ATTACK_HQ_RAD = 32;
	
	public static final int SCOUT_RAD_SQUARED_CONST = 10;
	public static final int SCOUT_DIST_CONST = 5;
	
	public static final int ENCAMPMENT_PROTECT_RAD_SQUARED = 36;
	
	public static final int HQ_ENTER_RUSH_RAD = 2;
	public static final int HQ_RUSH_TIMEOUT = 100;
	
	public static final int SCOUT_RECOMPUTE_PATH_INTERVAL_CONST = 100;
	

	public static final int MAX_POSSIBLE_SOLDIERS = 20000;
	
	public static double RATIO_ARMY_GENERATOR_CONST = 7; 
	
	
}
