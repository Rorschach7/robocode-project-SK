package helper.strategies.gun;

import helper.FuncLib;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robots.BaseBot;

public class LinTargeting extends GunStrategy {

	@Override
	public boolean execute(BaseBot robot) {
		ScannedRobotEvent enemy = robot.getTarget().getInfo();		
		
		double power = Math.min(3,
				Math.max(.1, 400 / enemy.getDistance()));
		final double ROBOT_WIDTH = 16, ROBOT_HEIGHT = 16;
		// Variables prefixed with e- refer to enemy, b- refer to bullet and
		// r- refer to robot
		final double eAbsBearing = robot.getHeadingRadians()
				+ enemy.getBearingRadians();
		final double rX = robot.getX(), rY = robot.getY(), bV = Rules
				.getBulletSpeed(power);
		final double eX = rX + enemy.getDistance()
				* Math.sin(eAbsBearing), eY = rY
				+ enemy.getDistance() * Math.cos(eAbsBearing), eV = robot.getTarget()
				.getInfo().getVelocity(), eHd = enemy
				.getHeadingRadians();
		// These constants make calculating the quadratic coefficients below
		// easier
		final double A = (eX - rX) / bV;
		final double B = eV / bV * Math.sin(eHd);
		final double C = (eY - rY) / bV;
		final double D = eV / bV * Math.cos(eHd);
		// Quadratic coefficients: a*(1/t)^2 + b*(1/t) + c = 0
		final double a = A * A + C * C;
		final double b = 2 * (A * B + C * D);
		final double c = (B * B + D * D - 1);
		final double discrim = b * b - 4 * a * c;
		if (discrim >= 0) {
			// Reciprocal of quadratic formula
			final double t1 = 2 * a / (-b - Math.sqrt(discrim));
			final double t2 = 2 * a / (-b + Math.sqrt(discrim));
			final double t = Math.min(t1, t2) >= 0 ? Math.min(t1, t2)
					: Math.max(t1, t2);
			// Assume enemy stops at walls
			final double endX = FuncLib.limit(eX + eV * t * Math.sin(eHd),
					ROBOT_WIDTH / 2, robot.getBattleFieldWidth() - ROBOT_WIDTH
							/ 2);
			final double endY = FuncLib.limit(eY + eV * t * Math.cos(eHd),
					ROBOT_HEIGHT / 2, robot.getBattleFieldHeight() - ROBOT_HEIGHT
							/ 2);
			
			// TODO: Compensate for own movement?		
			
			robot.setTurnGunRightRadians(robocode.util.Utils
					.normalRelativeAngle(Math.atan2(endX - rX, endY - rY)
							- robot.getGunHeadingRadians()));
			if (Math.abs(robot.getGunTurnRemaining()) < 1 && !robot.checkFriendlyFire()
					&& robot.setFireBullet(power) != null) {
//				System.out.println("FIRE, LinTarget ");
				return true;				
			}			
		}
		return false;		
	}
	
	public String toString(){
		return "Linear Targeting";		
	}
}
