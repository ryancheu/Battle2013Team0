package BaseBot.Util;

public class NonConstants {
	/*All constants in here are things that are changed runtime by an HQ or are used by
	 * someone other than the HQ at any point
	 * 
	 */

	public static double RATIO_OF_SUPPLIERS_OVER_GENERATORS = -1;
	
	public static double CHANCE_OF_DEFUSING_ENEMY_MINE = 0.5;
	public static double CHANCE_OF_DEFUSING_NEUTRAL_MINE = 0.3;

	public static int LAST_ROUND_SHOT_DELAY = 5;

	public static int SOLDIER_ENEMY_CHECK_RAD = 10;
	
	public static int SOLDIER_RALLY_RAD = 32;
	
	public static float SOLDIER_OUTNUMBER_MULTIPLIER = 1.1f;
	public static int SOLDIER_RUN_HEALTH = 0;
	public static int SOLDIER_RUN_EVENTUALLY_HEALTH = 20;
	public static int SOLDIER_RETURN_HEALTH = 38;
	public static int SOLDIER_BATTLE_ENEMY_CHECK_RAD = 100;
	

	public static int SOLDIER_ATTACK_RAD = 40;
	public static int SOLDIER_JOIN_ATTACK_RAD = 100;

	public static int SCOUT_RAD_SQUARED = 10;
	public static int SCOUT_DIST = 5;
	
	
	public static int SCOUT_RECOMPUTE_PATH_INTERVAL = 100;
	
	public static int NUM_PREFUSION_ENC = 4;
	public static int numEncToClaim = 40; //intialized to 40 but later changed by util 
	public static int midGameEncToClaim = 39; //initialized to 39 but later changed by util
	
}
