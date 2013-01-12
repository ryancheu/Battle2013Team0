package MineTurtle.Util;

import java.util.HashMap;

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
		if(channel < NUM_ROTATED_CHANNELS)
			return readRotatedChannel(channel);
		else
			return readDuplicatedChannel(channel);
	}

	public void writeChannel(int channel, int message) throws GameActionException {
		if(channel < NUM_ROTATED_CHANNELS)
			writeRotatedChannel(channel, message);
		else
			writeDuplicatedChannel(channel, message);
	}
	
	/*
	 * 	Rotated channels are channels between 0 and NUM_ROTATED_CHANNELS
	 *  Rotated channels are actually broadcast on a channel between RAD_ROTATION_START
	 *  and BROADCAST_MAX_CHANNELS, rotated by NUM_ROTATED_CHANNELS every
	 *  RAD_ROTATION_INTERVAL rounds.
	 *  At the end of the rotation interval, writeRotatedChannel will write to
	 *  the channel for the next interval in addition to the channel for the current
	 *  interval.
	 *  Rotation channels cannot be read unless they were written to in the previous
	 *  round.
	 */
	
	private int readRotatedChannel(int channel) throws GameActionException {
		return mRC.readBroadcast(getRotatedChannelNumber(channel, Clock.getRoundNum()));
	}
	
	private void writeRotatedChannel(int channel, int message) throws GameActionException {
		int round = Clock.getRoundNum();
		
		if(readRotatedChannel(channel) != message)
			mRC.broadcast(getRotatedChannelNumber(channel, round), message);
		if((round + 1) % RAD_ROTATION_INTERVAL == 0)
			mRC.broadcast(getRotatedChannelNumber(channel, round + 1), message);
	}
	
	private int getRotatedChannelNumber(int channel, int round){
		return (mBroadcastOffset + channel + (round / RAD_ROTATION_INTERVAL) * NUM_ROTATED_CHANNELS)
				% (GameConstants.BROADCAST_MAX_CHANNELS - RAD_ROTATION_START) + RAD_ROTATION_START;
	}
	
	/*
	 *  Duplicated channels are channels between NUM_ROTATED_CHANNELS and
	 *  NUM_ROTATED_CHANNELS + NUM_DUPLICATED_CHANNELS.
	 *  Duplicated channels are broadcasted RAD_NUM_DUPLICATIONS times between
	 *  0 and RAD_ROTATION_START.
	 *  Reading will read from all RAD_NUM_DUPLICATIONS copies and return the
	 *  most popular value, and change the channels that disagree to this value.
	 *  Duplicated channels cost more to write to than rotated channels, but
	 *  don't need to be written to every turn and is more resistant to collisions.
	 */
	
	private int readDuplicatedChannel(int channel) throws GameActionException {
		HashMap<Integer, Integer> counts = new HashMap<Integer, Integer>();
		int[] values = new int[RAD_NUM_DUPLICATIONS];
		int bestValue = 0, bestValueCount = 0;
		for(int n=0; n<RAD_NUM_DUPLICATIONS; ++n){
			int value = mRC.readBroadcast(getDuplicatedChannelNumber(channel, n));
			Integer oldCount = counts.get(value);
			if(oldCount == null)
				oldCount = 0;
			counts.put(value, oldCount + 1);
			values[n] = value;
			if(bestValueCount < oldCount + 1){
				bestValueCount = oldCount + 1;
				bestValue = value;
			}
		}
		if(bestValueCount < RAD_NUM_DUPLICATIONS){
			Util.print("Radio collision!");
			for(int n=0; n<RAD_NUM_DUPLICATIONS; n++){
				if(values[n] != bestValue)
					mRC.broadcast(getDuplicatedChannelNumber(channel, n), bestValue);
			}
		}
		return bestValue;
	}
	
	private void writeDuplicatedChannel(int channel, int message) throws GameActionException{
		for(int n=0; n<RAD_NUM_DUPLICATIONS; ++n){
			int value = mRC.readBroadcast(getDuplicatedChannelNumber(channel, n));
			if(value != message)
				mRC.broadcast(getDuplicatedChannelNumber(channel, n), message);
		}
	}
	
	private int getDuplicatedChannelNumber(int channel, int n){
		return (mBroadcastOffset + channel + n*NUM_DUPLICATED_CHANNELS) % RAD_ROTATION_START;
	}
	
}
