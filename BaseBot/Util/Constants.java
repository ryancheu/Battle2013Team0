package BaseBot.Util;

import battlecode.common.Direction;

public class Constants {
	// Battlecode constants
	public static final int MAX_DIST_SQUARED = 70 * 70;
	public static final int NUM_DIR = 8;
	public static final int ATTACK_HQ_RAD = 32;
	
	// Player specific	
	public static final float AVG_POSITION_RECENT_WEIGHT = 0.4f; 
	public static final float EXP_PARALLEL_SPREAD = 4;
	public static final float HORZ_PERP_SPREAD_MULTIPLIER = 1.5f;
	public static final float HORZ_PERP_SPREAD_EXP_PARA = 3;

	// Radio Consts
	public static final int TEAM_A_BROADCAST_OFFSET = 1234;
	public static final int TEAM_B_BROADCAST_OFFSET = 4321;

	public static final int ENCAMPMENT_PROTECT_RAD_SQUARED = 36;
	public static final int HQ_PROTECT_RAD_SQUARED = 36;

	public static final int MAX_NUM_ENC_TO_CLAIM = 15;

	public static final int MAX_NUMBER_OF_ENCAMPMENTS = 15;
	//Team Memory Constants

	public static final int WE_NUKED = 0;
	public static final int ENEMY_NUKED = 1;
	public static final int WE_KILLED = 2;
	public static final int ENEMY_RUSH = 3;
	public static final int ENEMY_ECON = 4;
	public static final int TIEBREAKERS = 5;
	public static final int ROUND_NUM_MEMORY = 0;
	public static final int HOW_ENDED_MEMORY = 1;

	public static final int SOLDIER_BATTLE_FORMATION_DIST = 4;

	public static final int CENSUS_INTERVAL = 10;

	
	public static final int[] testDirOrderAll = { 0, 1, -1, 2, -2, 3, -3, 4 };
	public static final int[] testDirOrderFront = { 0, 1, -1 };
	public static final int[] testDirOrderFrontSide = { 0, 1, -1, 2, -2 };	
	//Directions in reverse order
		public static final Direction[] DIRECTION_REVERSE = { Direction.NORTH_WEST,
																Direction.WEST,
																Direction.SOUTH_WEST,
																Direction.SOUTH,
																Direction.SOUTH_EAST, 
																Direction.EAST, 
																Direction.NORTH_EAST,
																Direction.NORTH};
		
		
	
	// Player Consts

    public static final int ENCAMPMENT_NOT_CLAIMED = -1;	
    public static final int ENCAMPMENT_CAPTURE_STARTED = 2;
    
    public static final int GOTO_ENCAMPMENT_MAX_ROUNDS = 100;

	public static final int MEDIAN_SAMPLE_SIZE = 9;
	public static final int NUM_SOLDIERTYPES = 4;
	public static final int NUM_OF_CENSUS_GENERATORTYPES = 1;
	
	public static final float BREAK_TWO_SQUARES_PROB_NO_NUKE = 0.005f;
	public static final float BREAK_TWO_SQUARES_PROB_NUKE = 0.3f;
	
	public static final int NUM_EXTRA_ENCAMPMENTS_BEFORE_FUSION = 1;
	public static final int NUM_SUPPLIER_OR_GENERATOR_BEFORE_MEDBAY = 4;
	
	//SOLDIER WAY POINT RALLY CHAN ORGANIZATION:
		//12 bits - round waypoints last updated
		//14 bits - start waypoint transmission channel
		//6 bits - number of waypoints in rally

	public static final int WAYPOINT_ROUND_BITS = 12;
	public static final int WAYPOINT_START_CHAN_BITS = 14;
	public static final int WAYPOINT_NUM_RALLY_BITS = 6;
	
	public static final int ATTACK_HQ_SIGNAL = 100;
	public static final int RETREAT_SIGNAL = 200;
	
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
		public static final int FIRST_BYTE_KEY =-805306368; 

	
}
