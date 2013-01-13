package MineTurtle.Util;

public class Constants {
	// Battlecode constants
	public static final int MAX_DIST_SQUARED = 70 * 70;
	public static final int NUM_DIR = 8;
	
	// Player specific	
	public static final float AVG_POSITION_RECENT_WEIGHT = 0.4f; 
	public static final float EXP_PARALLEL_SPREAD = 4;
	public static final float HORZ_PERP_SPREAD_MULTIPLIER = 1.5f;
	
	public static final int ORGANIZE_INTERVAL = 10;
	public static final int ORGANIZE_ROUNDS = 3;
	// Player Consts
	

	// HQ Consts
	public static final int NUM_ROBOTS_TO_CHECK_ID = 4;
	public static int NUM_ENC_TO_CLAIM = 4;
	public static final int NUM_MINERS = 1;
	public static final int NUM_SCOUTS = 1;
	public static final int NUM_ARMY = 20;
	public static final int NUM_ARMY_NO_FUSION = 15;
	public static final int NUM_ARMY_WITH_FUSION = 50;
	public static final int NUM_ARMY_BEFORE_RETREAT = 15;
	public static final int NUM_ARMY_BEFORE_ATTACK = 20;
	public static final int CENSUS_INTERVAL = 10;
	public static final int NUM_ROBOT_TO_SPAWN = NUM_ENC_TO_CLAIM + NUM_MINERS + NUM_SCOUTS + NUM_ARMY_NO_FUSION;
	public static final int NUM_PREFUSION_ENC = 4;
	public static final int PREFUSION_POWER_RESERVE = 150;
	public static final int POWER_RESERVE = 400;
	public static final int RUSH_NUKE_TIME = 30;

	// Radio Consts
	public static final int TEAM_A_BROADCAST_OFFSET = 1234;
	public static final int TEAM_B_BROADCAST_OFFSET = 4321;

	public static final int NUM_ROTATED_CHANNELS = 1000;
	public static final int NUM_DUPLICATED_CHANNELS = 1000;
	public static final int RAD_NUM_DUPLICATIONS = 3;
	public static final int RAD_ROTATION_START = NUM_ROTATED_CHANNELS + RAD_NUM_DUPLICATIONS*NUM_DUPLICATED_CHANNELS;
	public static final int RAD_ROTATION_INTERVAL = 7;
	
	// Channels that use rotation (must be resent every round)
	// Use this for values that change frequently
	public static final int COUNT_MINERS_RAD_CHAN = 100; // 1 channel
	public static final int RALLY_RAD_CHAN = 101; // 1 channel
	// public static final int ARMY_MESSAGE_SIGNAL_CHAN = 102; //1 channel
	public static final int NEXT_SOLDIER_TYPE_CHAN = 103; // 1 channel
	public static final int MEDBAY_LOCATION_CHAN = 104;
	public static final int ENEMY_AVG_POS_RAD_CHANNEL = 105;
	public static final int SOLDIER_WAYPOINT_RALLY_CHAN = 106;
	public static final int CENSUS_RAD_CHAN_START = 107; // SoldierType.length channels
	public static final int HQ_ATTACK_RALLY_CHAN_START = 200; // 30 channels
	public static final int NUM_SCOUT_WAYPOINTS_RAD_CHAN = 300;
	public static final int SCOUT_WAYPOINTS_CHAN_START = 301;
	public static final int ENEMY_LOCATION_CHAN = 400; // 1 chan
	
	// Channels that use duplication
	// Use this for values that change rarely
	public static final int MEDBAY_CLAIMED_RAD_CHAN = 1001; // 1 channel
	public static final int ENC_CLAIM_RAD_CHAN_START = MEDBAY_CLAIMED_RAD_CHAN + 1;// NUM_ENC_TO_CLAIM channels
	//ENC_CLAIM_RAD_CHAN_START MUST be the last Radio channel otherwise encampment grabbing doesn't work

	
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
	
	public static final int LAST_ROUND_SHOT_DELAY = 5;

	public static final int SOLDIER_ENEMY_CHECK_RAD = 10;
	
	public static final int SOLDIER_RALLY_RAD = 32;
	
	public static final float SOLDIER_OUTNUMBER_MULTIPLIER = 1.1f;
	public static final int SOLDIER_RUN_HEALTH = 10;
	public static final int SOLDIER_RETURN_HEALTH = 38;
	public static final int SOLDIER_BATTLE_ENEMY_CHECK_RAD = 100;
	
	
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
			
	
	public static final int SOLDIER_ATTACK_RAD = 64;
}
