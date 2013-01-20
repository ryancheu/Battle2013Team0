package BaseBot.Util;

public class RushConstants {
	// Battlecode constants
	public static final int MAX_DIST_SQUARED = 70 * 70;
	public static final int NUM_DIR = 8;
	//Team Memory Constants
	
	public static final int WE_NUKED = 0;
	public static final int ENEMY_NUKED = 1;
	public static final int WE_KILLED = 2;
	public static final int ENEMY_RUSH = 3;
	public static final int ENEMY_ECON = 4;
	public static final int TIEBREAKERS = 5;
	public static final int ROUND_NUM_MEMORY = 0;
	public static final int HOW_ENDED_MEMORY = 1;
	
	// Player specific	
	public static final float AVG_POSITION_RECENT_WEIGHT = 0.4f; 
	public static final float EXP_PARALLEL_SPREAD = 4;
	public static final float HORZ_PERP_SPREAD_MULTIPLIER = 1.5f;
	public static final float HORZ_PERP_SPREAD_EXP_PARA = 3;
	
	public static final int ORGANIZE_INTERVAL = 10;
	public static final int ORGANIZE_ROUNDS = 3;
	
	public static final int ATTACK_ROUND = 50;
	// Player Consts

    public static final int ENCAMPMENT_NOT_CLAIMED = -1;	
    public static final int ENCAMPMENT_CAPTURE_STARTED = 2;

	// HQ Consts
    public static final int LATE_GAME = 450;
	public static final int NUM_ROBOTS_TO_CHECK_ID = 4;
	public static final double CHANCE_OF_DEFUSING_ENEMY_MINE_CONST = 0.5;
	public static final double CHANCE_OF_DEFUSING_NEUTRAL_MINE_CONST = 0.3;
	public static final int MAX_NUM_ENC_TO_CLAIM = 15;
	public static final int NUM_MINERS = 1;
	public static final int NUM_MINERS_WITH_PICKAXE = 0;
	public static final int NUM_SCOUTS = 0;
	public static final int NUM_ARMY = 20;
	public static final int NUM_ARMY_NO_FUSION = 15;
	public static final int NUM_ARMY_WITH_FUSION = 50;
	public static final int NUM_ARMY_BEFORE_RETREAT = 1;
	public static final int NUM_ARMY_BEFORE_ATTACK = 10;
	public static final int NUM_ARMY_BEFORE_ATTACK_WITH_NUKE = 10;
	public static final int CENSUS_INTERVAL = 10;
	public static final int NUM_ROBOT_TO_SPAWN = NUM_MINERS + NUM_SCOUTS + NUM_ARMY_NO_FUSION;
	public static final int PREFUSION_POWER_RESERVE = 70;
	public static final int POWER_RESERVE = 400;
	public static final int RUSH_NUKE_TIME = 30;
	public static final int MAX_NUMBER_OF_ENCAMPMENTS = 15;
	public static final int NUM_SOLDIERTYPES = 4;
	public static final int NUM_OF_CENSUS_GENERATORTYPES = 1;
	public static final double RATIO_OF_SUPPLIERS_OVER_GENERATORS_CONST = 9.0;
	public static int Map_Width = 60;
	public static int Map_Height = 60;

	// Radio Consts
	public static final int TEAM_A_BROADCAST_OFFSET = 1234;
	public static final int TEAM_B_BROADCAST_OFFSET = 4321;

	public static final int NUM_ROTATED_CHANNELS = 1000;
	public static final int NUM_DUPLICATED_CHANNELS = 10000;
	public static final int RAD_NUM_DUPLICATIONS = 3;
	public static final int RAD_ROTATION_START = NUM_ROTATED_CHANNELS + RAD_NUM_DUPLICATIONS*NUM_DUPLICATED_CHANNELS;
	public static final int RAD_ROTATION_INTERVAL = 7;
	
	
	//SOLDIER WAY POINT RALLY CHAN ORGANIZATION:
	//12 bits - round waypoints last updated
	//14 bits - start waypoint transmission channel
	//6 bits - number of waypoints in rally

	public static final int WAYPOINT_ROUND_BITS = 12;
	public static final int WAYPOINT_START_CHAN_BITS = 14;
	public static final int WAYPOINT_NUM_RALLY_BITS = 6;

	
	public static final int[] testDirOrderAll = { 0, 1, -1, 2, -2, 3, -3, 4 };
	public static final int[] testDirOrderFront = { 0, 1, -1 };
	public static final int[] testDirOrderFrontSide = { 0, 1, -1, 2, -2 };	
	
	public static final int ATTACK_HQ_SIGNAL = 100;
	public static final int RETREAT_SIGNAL = 200;
	
	public static final int RALLY_RAD_SQUARED = 16;
	
	public static final int LAST_ROUND_SHOT_DELAY_CONST = 5;

	public static final int SOLDIER_ENEMY_CHECK_RAD_CONST = 10;
	
	public static final int SOLDIER_RALLY_RAD_CONST = 32;
	
	public static final float SOLDIER_OUTNUMBER_MULTIPLIER_CONST = 1.1f;
	public static final int SOLDIER_RUN_HEALTH_CONST = 0;
	public static final int SOLDIER_RUN_EVENTUALLY_HEALTH_CONST = 20;
	public static final int SOLDIER_RETURN_HEALTH_CONST = 38;
	public static final int SOLDIER_BATTLE_ENEMY_CHECK_RAD_CONST = 100;
	
	
	//FLAGS 	
	
	public static final int I_BIT_MASK = 1;
	public static final int II_BIT_MASK = I_BIT_MASK << 1 | 1;
	public static final int III_BIT_MASK = II_BIT_MASK << 1 | 1;
	public static final int	IV_BIT_MASK = III_BIT_MASK << 1 |1; 
	public static final int V_BIT_MASK = IV_BIT_MASK << 1 | 1;
	public static final int VI_BIT_MASK = V_BIT_MASK << 1 | 1;
	public static final int VII_BIT_MASK = VI_BIT_MASK << 1 | 1;
	public static final int VIII_BIT_MASK = VII_BIT_MASK << 1 | 1;
	public static final int IX_BIT_MASK = VIII_BIT_MASK << 1 | 1;
	public static final int X_BIT_MASK = IX_BIT_MASK << 1 | 1;
	public static final int XI_BIT_MASK = X_BIT_MASK << 1 | 1;
	public static final int XII_BIT_MASK = XI_BIT_MASK << 1 | 1;
	public static final int XIII_BIT_MASK = XII_BIT_MASK << 1 | 1;
	public static final int XIV_BIT_MASK = XIII_BIT_MASK << 1 | 1;
	public static final int XV_BIT_MASK = XIV_BIT_MASK << 1 | 1;
	public static final int XVI_BIT_MASK = XV_BIT_MASK << 1 | 1;
	public static final int XVII_BIT_MASK = XVI_BIT_MASK << 1 | 1;
	
	
	public static final int[] BIT_MASKS = { 0,
											I_BIT_MASK,
											II_BIT_MASK,
											III_BIT_MASK,
											IV_BIT_MASK,
											V_BIT_MASK,
											VI_BIT_MASK,
											VII_BIT_MASK,
											VIII_BIT_MASK,
											IX_BIT_MASK,
											X_BIT_MASK,
											XI_BIT_MASK,
											XII_BIT_MASK,
											XIII_BIT_MASK,
											XIV_BIT_MASK,
											XV_BIT_MASK,
											XVI_BIT_MASK,
											XVII_BIT_MASK };	
			

	public static final int SOLDIER_ATTACK_RAD_CONST = 40;
	public static final int SOLDIER_JOIN_ATTACK_RAD_CONST = 100;
	public static final int ATTACK_HQ_RAD = 32;
	
	public static final int SCOUT_RAD_SQUARED_CONST = 10;
	public static final int SCOUT_DIST_CONST = 5;
	
	public static final int ENCAMPMENT_PROTECT_RAD_SQUARED = 36;
	
	public static final int HQ_ENTER_RUSH_RAD = 2;
	public static final int HQ_RUSH_TIMEOUT = 100;
	
	public static final int SCOUT_RECOMPUTE_PATH_INTERVAL_CONST = 100;
	
	public static final int GOTO_ENCAMPMENT_MAX_ROUNDS = 100;

	public static final int MEDIAN_SAMPLE_SIZE = 9;

	public static final int MAX_POSSIBLE_SOLDIERS = 2000;
	
	
}
