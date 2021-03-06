package AttackingTest.Util;

public class Constants {
	// Battlecode constants
	public static final int MAX_DIST_SQUARED = 70 * 70;
	public static final int NUM_DIR = 8;

	// Player specific

	// Player Consts
	public static final int NUM_ROBOTS_TO_CHECK_ID = 0;
	public static int NUM_ENC_TO_CLAIM = 0;
	public static final int NUM_MINERS = 0;
	public static final int NUM_SCOUTS = 0;
	public static final int NUM_ARMY = 20;
	public static final int CENSUS_INTERVAL = 10;

	// HQ Consts
	public static final int NUM_ROBOT_TO_SPAWN = NUM_ENC_TO_CLAIM + NUM_MINERS + NUM_SCOUTS + NUM_ARMY;

	// Radio Consts
	public static final int TEAM_A_BROADCAST_OFFSET = 1234;
	public static final int TEAM_B_BROADCAST_OFFSET = 4321;
	public static final int COUNT_MINERS_RAD_CHAN = 100; // 1 channel
	public static final int SPAWN_MINER_RAD_CHAN = COUNT_MINERS_RAD_CHAN + 1; // 1 channel
	public static final int SPAWN_SCOUT_RAD_CHAN = SPAWN_MINER_RAD_CHAN + 1; // 1 channel
	public static final int RALLY_RAD_CHAN = SPAWN_SCOUT_RAD_CHAN + 1; // 1 channel
	public static final int ARMY_MESSAGE_SIGNAL_CHAN = RALLY_RAD_CHAN + 1; //1 channel
	public static final int LAST_FOUR_BOT_ID_RAD_CHAN_START = ARMY_MESSAGE_SIGNAL_CHAN + 1; //4 channels
	public static final int CURRENT_BOT_ID_CHAN = LAST_FOUR_BOT_ID_RAD_CHAN_START + 4; //1 channel
	public static final int MEDBAY_CLAIMED_RAD_CHAN = CURRENT_BOT_ID_CHAN + 1;
	public static final int ENC_CLAIM_RAD_CHAN_START = MEDBAY_CLAIMED_RAD_CHAN + 1;// NUM_ENC_TO_CLAIM channels
	//ENC_CLAIM_RAD_CHAN_START MUST be the last Radio channel otherwise encampment grabbing doesn't work
	
	public static final int[] testDirOrderAll = { 0, 1, -1, 2, -2, 3, -3, 4 };
	public static final int[] testDirOrderFront = { 0, 1, -1 };
	public static final int[] testDirOrderFrontSide = { 0, 1, -1, 2, -2 };	
	
	public static final int ATTACK_HQ_SIGNAL = 100;
	public static final int RETREAT_SIGNAL = 200;
	
	public static final int RALLY_RAD_SQUARED = 16;
	
	public static final int LAST_ROUND_SHOT_DELAY = 5;

	public static final int SOLDIER_ENEMY_CHECK_RAD = 10;
	
	public static final int SOLDIER_RALLY_RAD = 10;
	public static final int SOLDIER_WAYPOINT_RALLY_CHAN = 1000;
	public static final int MEDBAY_LOCATION_CHAN = 2000;
	public static final int HQ_ATTACK_RALLY_CHAN_START = 1500;
	
	public static final int NUM_CHANNELS = 10000;
	
	public static final float SOLDIER_OUTNUMBER_MULTIPLIER = 1.1f;
	public static final int SOLDIER_RUN_HEALTH = 10;
	public static final int SOLDIER_RETURN_HEALTH = 38;
	public static final int SOLDIER_BATTLE_ENEMY_CHECK_RAD = 100;
	
	public static final int ENEMY_AVG_POS_RAD_CHANNEL = 2500;
	
	//SOLDIER WAY POINT RALLY CHAN ORGANIZATION:
	//12 bits - round waypoints last updated
	//14 bits - start waypoint transmission channel
	//6 bits - number of waypoints in rally

	public static final int WAYPOINT_ROUND_BITS = 12;
	public static final int WAYPOINT_START_CHAN_BITS = 14;
	public static final int WAYPOINT_NUM_RALLY_BITS = 6;
	
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
