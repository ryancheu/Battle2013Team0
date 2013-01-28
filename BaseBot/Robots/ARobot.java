package BaseBot.Robots;

import static BaseBot.Util.Constants.TEAM_A_BROADCAST_OFFSET;
import static BaseBot.Util.Constants.TEAM_B_BROADCAST_OFFSET;

import java.util.Random;








import BaseBot.Util.Radio;
import BaseBot.Util.Util;
import battlecode.common.*;
public class ARobot 
{
	
	public static RobotController mRC;
	public static Radio mRadio;
	public static Team mTeam;
	public static Team mEnemy;
	public static Random rand;
	public ARobot(RobotController rc)
	{
		rand = new Random((int)((rc.getRobot().getID() + rc.getTeamPower())*100*(Clock.getRoundNum() + Clock.getBytecodesLeft())));
		mRC = rc;
		mTeam = rc.getTeam();
		mEnemy = mTeam.opponent();	
		//mRadio.read/writeChannel is called to use the radio
		mRadio = new Radio(mRC, 
				mTeam == Team.A ? TEAM_A_BROADCAST_OFFSET : TEAM_B_BROADCAST_OFFSET);
		Util.setMapWidthAndHeight();
	}
	public void takeTurn() throws GameActionException{
		
	}
}
