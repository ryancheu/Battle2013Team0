package MineTurtle.Robots;

import static MineTurtle.Util.Constants.TEAM_A_BROADCAST_OFFSET;
import static MineTurtle.Util.Constants.TEAM_B_BROADCAST_OFFSET;
import MineTurtle.Util.Radio;
import battlecode.common.*;

public class ARobot 
{
	public static RobotController mRC;
	public static Radio mRadio;
	public static Team mTeam;
	public static Team mEnemy;
		
	public ARobot(RobotController rc)
	{
		mRC = rc;
		mTeam = rc.getTeam();
		mEnemy = mTeam.opponent();		
		mRadio = new Radio(mRC, 
				mTeam == Team.A ? TEAM_A_BROADCAST_OFFSET : TEAM_B_BROADCAST_OFFSET);
	}
	public void takeTurn() throws GameActionException{
		
	}
}
