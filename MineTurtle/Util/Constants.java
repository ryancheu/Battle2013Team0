package MineTurtle.Util;

public class Constants {
	// Battlecode constants
	public static final int MAX_DIST_SQUARED = 70 * 70;
	public static final int NUM_DIR = 8;

	// Player specific

	// Player Consts
	public static final int NUM_ROBOTS_TO_CHECK_ID = 4;
	public static int NUM_ENC_TO_CLAIM = 4;
	public static final int NUM_MINERS = 4;
	public static final int NUM_SCOUTS = 1;
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
}
