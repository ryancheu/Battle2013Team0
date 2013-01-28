package OldSchoolBot.Robots;

import OldSchoolBot.Robots.Types.ArtilleryNormalType;
import OldSchoolBot.Util.RadioChannels;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import static OldSchoolBot.Util.Constants.*;

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
		
		public ArtilleryRobot(RobotController rc) throws GameActionException {
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
			performCensus();
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
		
		public static void performCensus() throws GameActionException {
			if ( Clock.getRoundNum() % CENSUS_INTERVAL == 0) {
				int count = SupplierRobot.mRadio.readChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES 
						+ NUM_OF_CENSUS_GENERATORTYPES + NUM_OF_CENSUS_SUPPLIERTYPES );
				SoldierRobot.mRadio.writeChannel(RadioChannels.CENSUS_START + NUM_SOLDIERTYPES 
						+ NUM_OF_CENSUS_GENERATORTYPES + NUM_OF_CENSUS_SUPPLIERTYPES, count + 1);
			}
		}

}
