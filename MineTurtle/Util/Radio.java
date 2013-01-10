package MineTurtle.Util;

import battlecode.common.*;


public class Radio {
	private RobotController mRC;
	private int mBroadcastOffset; 
	public Radio(RobotController rc, int broadcastOffset) {
		mRC = rc;
		mBroadcastOffset = broadcastOffset;		
	}
	public int readChannel(int channel) throws GameActionException{
		return mRC.readBroadcast(mBroadcastOffset + channel);
	}
	public void writeChannel(int channel, int message) throws GameActionException {
		mRC.broadcast(mBroadcastOffset + channel, message);		
	}
}
