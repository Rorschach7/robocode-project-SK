package helper.strategies.gun;

import java.util.ArrayList;
import java.util.List;

import helper.Data;
import helper.FuncLib;
import helper.WaveBullet;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import robots.BaseBot;

public class GuessTargeting extends GunStrategy {
	
	private List<WaveBullet> waves = new ArrayList<WaveBullet>();

	@Override
	public boolean execute(BaseBot robot) {
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
		
		Data data = FuncLib.findDataByName(enemy.getName(), robot.getDataList());
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

		if (robot.getGunHeat() == 0 && Math.abs(robot.getGunTurnRemaining()) < 1 && !robot.checkFriendlyFire() && robot.setFireBullet(power) != null) {
			waves.add(newWave);
//			System.out.println("Fire, Guess Shooting ");
			return true;			
		}
		return false;
	}
	
	@Override
	public void collectData(BaseBot robot, ScannedRobotEvent e) {
		// Collect data
		double absBearing = robot.getHeadingRadians() + e.getBearingRadians();

		// find our enemy's location:
		double ex = robot.getX() + Math.sin(absBearing) * e.getDistance();
		double ey = robot.getY() + Math.cos(absBearing) * e.getDistance();

		// Let's process the waves now:
		for (int i = 0; i < waves.size(); i++) {
			WaveBullet currentWave = (WaveBullet) waves.get(i);
			if (currentWave.checkHit(ex, ey, robot.getTime())) {
				waves.remove(currentWave);
				i--;
			}
		}
	}
	
	
	public String toString(){
		return "Guess Factor Targeting";		
	}
	
}
