package SimpleAttacking.Robots;

import SimpleAttacking.Robots.Types.ArtilleryNormalType;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ArtilleryRobot extends EncampmentRobot{
	//Maybe remove? kept in for organization
		public enum ArtilleryType  {
			NORMAL,
		}
		public enum ArtilleryState {	
			FIRE
		}
		
		public static int lastRoundShot = 0;
		
		protected static ArtilleryState mState;
		protected static ArtilleryType mType;
		public static MapLocation enemyHQLoc;
		
		public ArtilleryRobot(RobotController rc) {
			super(rc);
			enemyHQLoc = rc.senseEnemyHQLocation();
		}
		@Override
		public void takeTurn() throws GameActionException {
			super.takeTurn();
			mainArtilleryLogic();
		}
		
		private void mainArtilleryLogic() throws GameActionException {
			if (mType == null) {
				mType = ArtilleryType.NORMAL;
				mState = ArtilleryState.FIRE;
			}
			switch(mType)
			{
				case NORMAL: 
				{
					ArtilleryNormalType.run(mRC);
				}
			}
			
		}

		
		public static ArtilleryState getState() {
			return mState;
		}
		
		public static void switchState(ArtilleryState state) {
			mState = state;
			mRC.setIndicatorString(state.ordinal(), "State");
		}
		public static void switchType(ArtilleryType type) {
			mType = type; 
			mRC.setIndicatorString(type.ordinal(), "Type");
		}

}
