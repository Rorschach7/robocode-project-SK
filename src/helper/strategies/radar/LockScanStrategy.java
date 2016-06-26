package helper.strategies.radar;

import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import robots.BaseBot;

public class LockScanStrategy extends RadarStrategy {
	
	protected double lockAngle = 20.0;
	
	@Override
	public boolean execute(BaseBot robot, ScannedRobotEvent e) {
		return execute(robot);	
	}
	
	@Override
	public boolean execute(BaseBot robot) {
		ScannedRobotEvent target = robot.getTarget().getInfo();
		if(target == null) {
			return false;
		}
		
		// double angleToEnemy = getHeading() +
		// target.getInfo().getBearing();
		//
		// double radarTurn = Utils.normalRelativeAngleDegrees(angleToEnemy
		// - getRadarHeading());
		//
		// setTurnRadarRight(radarTurn);

		double angleToEnemy = robot.getHeadingRadians() + target.getBearingRadians();

		// Subtract current radar heading to get the turn required to face
		// the enemy, be sure it is normalized
		double radarTurn = Utils.normalRelativeAngle(angleToEnemy - robot.getRadarHeadingRadians());

		// Distance we want to scan from middle of enemy to either side
		// The 36.0 is how many units from the center of the enemy robot it
		// scans.
		double extraTurn = Math.min(Math.atan(lockAngle / target.getDistance()), Rules.RADAR_TURN_RATE_RADIANS);

		// Adjust the radar turn so it goes that much further in the
		// direction it is going to turn
		// Basically if we were going to turn it left, turn it even more
		// left, if right, turn more right.
		// This allows us to overshoot our enemy so that we get a good sweep
		// that will not slip.
		radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);

		// Turn the radar
		robot.setTurnRadarRightRadians(radarTurn);
		return true;
	}
	
	/**
	 * This ScanLock locks not directly on the enemy,
	 * but sweeps back and forth a little bit.
	 * Helps to make up for sudden turns etc.
	 * lockAngle <= 20.
	 * @param lockAngle
	 */
	public void setLockAngle(double lockAngle) {
		if(lockAngle > 20) {
			this.lockAngle = 20.0;
		}
		this.lockAngle = lockAngle;
	}

}
