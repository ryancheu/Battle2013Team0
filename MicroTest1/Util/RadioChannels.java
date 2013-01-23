package MicroTest1.Util;

public class RadioChannels {

// Channels that use rotation (must be resent every round)
// Use this for values that change frequently
// 0 to NUM_ROTATED_CHANNELS

// Unused
// public static final int COUNT_MINERS = 100; // 1 channel
// public static final int RALLY = 101; // 1 channel
// public static final int ARMY_MESSAGE_SIGNAL = 102; //1 channel

// Used by HQ to tell new soldiers what type to become
public static final int NEXT_SOLDIER_TYPE = 103; // 1 channel

// Medbay location, broadcasted by Medbay
public static final int MEDBAY_LOCATION = 104; // 1 channel


// Average of enemy locations, broadcasted by HQ
public static final int ENEMY_AVG_POS = 105; // 1 channel

// Whether or not the army should lay mines, broadcast by HQ
public static final int SHOULD_LAY_MINES = 106; // 1 channel

// Initialized by HQ, incremented by robots
public static final int CENSUS_START = 107; // SoldierType.length channels

// Used by scouts to send to the HQ the waypoints they compute
public static final int SCOUT_WAYPOINTS_START = 300; // 30 channels

// Location of last seen enemy, sent by HQ
public static final int ENEMY_LOCATION = 400; // 1 channel

// Encampment with nearby enemies
public static final int ENCAMPMENT_IN_DANGER = 401; // 1 channel

// The id and type of a newly spawned unit
public static final int NEW_UNIT_ID = 402; // 1 channel

//HQ with nearby enemies
public static final int HQ_IN_DANGER = 405; //1channel


// Channels that use duplication
// Use this for values that change rarely or are very important
// NUM_ROTATED_CHANNELS + 1 to NUM_ROTATED_CHANNELS + NUM_DUPLICATED_CHANNELS

// Round num started building medbay if we've building/built a medbay, 0 if not
public static final int MEDBAY_CLAIMED = 1001; // 1 channel

// Encampment claiming
public static final int ENC_CLAIM_START = 1002;// NUM_ENC_TO_CLAIM channels

// Length of SCOUT_WAYPOINTS_START
public static final int NUM_SCOUT_WAYPOINTS = 1100; // 1 channel

// Information about army soldier waypoints, sent by HQ
public static final int SOLDIER_WAYPOINT_RALLY = 1101; // 1 channel

// Unused
// public static final int BACKUP_RALLY_POINT = 1102;

// Number of turns spent capturing an encampment
public static final int ENCAMPMENT_BUILDING_START = 1103; // NUM_ENC_TO_CLAIM at max

// List of waypoints to the enemy, broadcast by HQ
public static final int HQ_ATTACK_RALLY_START = 1200; // 30 channels

// Whether the army should enter battle state
public static final int ENTER_BATTLE_STATE = 1300; // 1 channel

//Shield Battery location, broadcasted by Shield Battery
public static final int SHIELD_LOCATION = 1301; // 1 channel

// amount of Suppliers, generators,broadcast by HQ for use by EncampmentSoldiers
public static final int NUM_GENERATORS = 1302;
public static final int NUM_SUPPLIERS = 1303;

// The type of the HQ
public static final int HQ_TYPE = 1304; // 1 channel

// The state of the HQ
public static final int HQ_STATE = 1305; // 1 channel

//Should a soldier at a point ecome an encampment?
public static final int BECOME_ENCAMPMENT = 1306; // 1 channel

//0 if no nuke, 1 if nuke
public static final int ENEMY_FASTER_NUKE = 1307;

public static final int SECOND_MEDBAY = 1308;

public static final int MAX_ENC_CHANNEL_TO_CHECK = 1309;

//What type of point scout did we make?
public static final int POINT_SCOUT_TYPE = 1310;

public static final int SHIELDS_CLAIMED = 1311; // 1 channel

public static final int SECOND_MEDBAY_CLAIMED = 1312; // 1 channel
}
