package helper.strategies;

import helper.Data;
import helper.WaveBullet;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import robots.TestBot;

public class GuessTargeting extends GunStrategy {

	@Override
	public boolean execute(TestBot robot) {
		ScannedRobotEvent enemy = robot.getTarget().getInfo();

		// Guess Targeting
		double absBearing = enemy.getBearingRadians() + robot.getHeadingRadians();
		double power = Math.min(3, Math.max(.1, 400 / enemy.getDistance()));

		if (enemy.getVelocity() != 0) {
			if (Math.sin(enemy.getHeadingRadians() - absBearing) * enemy.getVelocity() < 0){
				robot.setDirection(-1);
				
			} else {
				robot.setDirection(1);				
			}
		}
		
		Data data = robot.findDataByName(enemy.getName());
		int[] currentStats = data.getStats()[(int) (enemy.getDistance() / 100)];

		WaveBullet newWave = new WaveBullet(robot.getX(), robot.getY(), absBearing, power, robot.getDirection(), robot.getTime(), currentStats);

		int bestindex = 15; // initialize it to be in the middle,
							// guessfactor 0.
		for (int i = 0; i < 31; i++) {
			if (currentStats[bestindex] < currentStats[i]) {
				bestindex = i;
			}
		}

		// this should do the opposite of the math in the WaveBullet:
		double guessfactor = (double) (bestindex - (currentStats.length - 1) / 2) / ((currentStats.length - 1) / 2);
		double angleOffset = robot.getDirection() * guessfactor * newWave.maxEscapeAngle();
		double gunAdjust = Utils.normalRelativeAngle(absBearing - robot.getGunHeadingRadians() + angleOffset);

		robot.setTurnGunRightRadians(gunAdjust);

		if (robot.getGunHeat() == 0 && gunAdjust < Math.atan2(9, enemy.getDistance()) && !robot.checkFriendlyFire() && robot.setFireBullet(power) != null) {
			robot.getWaves().add(newWave);
			System.out.println("Fire, Guess Shooting ");
			return true;			
		}
		return false;
	}
	
	public String toString(){
		return "Guess Factor Targeting";		
	}
	
}
