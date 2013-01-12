package AttackingTest.Util;

import battlecode.common.*;

import static AttackingTest.Util.Constants.*;


public class Radio {
	private RobotController mRC;
	private int mBroadcastOffset; 
	public Radio(RobotController rc, int broadcastOffset) {
		mRC = rc;
		mBroadcastOffset = broadcastOffset;		
	}
	public int readChannel(int channel) throws GameActionException{
		return mRC.readBroadcast((mBroadcastOffset + channel)%NUM_CHANNELS);
	}
	public void writeChannel(int channel, int message) throws GameActionException {
		mRC.broadcast((mBroadcastOffset + channel)%NUM_CHANNELS, message);		
	}
}
