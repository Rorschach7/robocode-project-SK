package robots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

public class TestBot2 extends AdvancedRobot {
	private double MIN_BULLET_POWER = Rules.MIN_BULLET_POWER;
	private double MAX_BULLET_POWER = Rules.MAX_BULLET_POWER;
	private double lastRobotDistance;
	private String lastRobotName;
	private boolean lastRobotDied = false;

	public void run() {

		setBodyColor(Color.black);
		setGunColor(Color.red);
		setRadarColor(Color.black);
		setBulletColor(Color.green);
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true); // Keep turret still while moving
		turnRadarRight(Double.POSITIVE_INFINITY);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		double absBearing = e.getBearing() + getHeading();
		double gunTurnAmt;
		
		//e.getHeading();

		// Radar Stuff
		// setTurnRadarLeft(getRadarTurnRemaining());// Lock on the radar
		if (lastRobotName == null || lastRobotDied || e.getDistance() < lastRobotDistance
				|| e.getName().equals(lastRobotName)) {
			// Gun Stuff
			gunTurnAmt = normalRelativeAngleDegrees(absBearing
					- getGunHeading());
			setTurnGunRight(gunTurnAmt); // turn gun
			fireBullet(e);
			lastRobotName = e.getName();
			lastRobotDistance = e.getDistance();
		}
	}

	private void fireBullet(ScannedRobotEvent e) {
		double firePower = 400 / e.getDistance();
		if (firePower > calcFirePowerLife(e)) {
			firePower = calcFirePowerLife(e);
		}

		fire(firePower);
		lastRobotDistance = e.getDistance();
		lastRobotName = e.getName();
	}

	@Override
	public void onRobotDeath(RobotDeathEvent event) {
		// TODO Auto-generated method stub
		super.onRobotDeath(event);
		
		if(lastRobotName == null || event.getName().equals(lastRobotName)){
			lastRobotDied = true;
		}
	}

	public void onStatus(StatusEvent event) {
		// TODO
	}

	private double calcFirePowerLife(ScannedRobotEvent e) {
		double firePower = 0.1;
		@SuppressWarnings("deprecation")
		double life = e.getLife();

		// raise bullet power till max or the damage is higher than life
		while (Rules.getBulletDamage(firePower) < life
				&& firePower <= MAX_BULLET_POWER) {
			firePower += 0.1;
		}

		return firePower;
	}
}
