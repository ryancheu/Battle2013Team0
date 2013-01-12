package MineTurtle.Util;

import battlecode.common.*;

import static MineTurtle.Util.Constants.*;


public class Radio {
	
	private RobotController mRC;
	private int mBroadcastOffset; 
	public Radio(RobotController rc, int broadcastOffset) {
		mRC = rc;
		mBroadcastOffset = broadcastOffset;		
	}
	
	public int readChannel(int channel) throws GameActionException {
		return mRC.readBroadcast(getChannelNumber(channel, Clock.getRoundNum(), channel < RAD_NO_ROTATION));
	}
	
	public void writeChannel(int channel, int message) throws GameActionException {
		int round = Clock.getRoundNum();
		boolean rotate = (channel < RAD_NO_ROTATION);
		
		if(readChannel(channel) != message)
			mRC.broadcast(getChannelNumber(channel, round, rotate), message);
		if(rotate && (round + 1) % RAD_ROTATION_INTERVAL == 0)
			mRC.broadcast(getChannelNumber(channel, round + 1, rotate), message);
	}
	
	public void updateChannel(int channel) throws GameActionException {
		
	}
	
	private int getChannelNumber(int channel, int round, boolean rotate){
		if(!rotate)
			return (mBroadcastOffset + channel)%NUM_CHANNELS;
		else{
			return (mBroadcastOffset + channel + (round / RAD_ROTATION_INTERVAL) * NUM_CHANNELS)
					% (GameConstants.BROADCAST_MAX_CHANNELS - NUM_CHANNELS) + NUM_CHANNELS;
		}
	}
	
}
