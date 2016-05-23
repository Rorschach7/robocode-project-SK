package robots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import robocode.AdvancedRobot;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

public class TestBot2 extends AdvancedRobot {
	private double lastRobotDistance;
	private String lastRobotName;
	private boolean lastRobotDied = false;
	private double moveDirection = 1;

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

		// check if there is an enemy closer than the last and hit him instead
		if (lastRobotName == null || lastRobotDied
				|| e.getDistance() < lastRobotDistance
				|| e.getName().equals(lastRobotName)) {
			// Gun Stuff
			gunTurnAmt = normalRelativeAngleDegrees(absBearing
					- getGunHeading());
			// Adjust turn amount to compensate own movement
			gunTurnAmt += (moveDirection * -1) * 5;
			setTurnGunRight(gunTurnAmt); // turn gun
			fireBullet(e);
			lastRobotName = e.getName();
			lastRobotDistance = e.getDistance();
			absBearing = e.getBearing() + getHeading();
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
		super.onRobotDeath(event);

		// check if the last Robot you fired on Died
		if (lastRobotName == null || event.getName().equals(lastRobotName)) {
			lastRobotDied = true;
		}
	}

	@Override
	public void onHitWall(HitWallEvent event) {
		super.onHitWall(event);

		System.out.println(" X: " + getX() + " Y: " + getY());
	}

	public void onStatus(StatusEvent event) {
		// TODO
		avoidWalls();
		setAhead(moveDirection * 20);
	}

	private double calcFirePowerLife(ScannedRobotEvent e) {
		double firePower = Rules.MIN_BULLET_POWER;
		@SuppressWarnings("deprecation")
		double life = e.getLife();

		// raise bullet power till max or the damage is higher than life
		while (Rules.getBulletDamage(firePower) < life
				&& firePower <= Rules.MAX_BULLET_POWER) {
			firePower += 0.1;
		}

		return firePower;
	}

	private boolean avoidWalls() {
		double fieldWith = getBattleFieldWidth();
		double fieldHeight = getBattleFieldHeight();
		double avoidDistance = 120;
		double turnDegree = 15;

		// TODO still bugs sometimes when heading to an edge

		// System.out.println("stickX: "
		// + (getX() + Math.sin(Math.toRadians(this.getHeading())) *
		// avoidDistance) + " stickY: "
		// + (getY() + Math.cos(Math.toRadians(this.getHeading())) *
		// avoidDistance) + " heading: " + this.getHeading()
		// + " X: " + getX() + " sinH: " + Math.sin(this.getHeading())
		// + " Y: " + getY() + " cosH: " + Math.cos(this.getHeading()));

		if (getX() + Math.sin(Math.toRadians(this.getHeading()))
				* avoidDistance >= fieldWith - 36) {
			// going to hit east wall
			System.out.println("going to hit east wall-------------------");
			if (this.getHeading() > 0 && this.getHeading() < 90)
				turnLeft(turnDegree);
			if (this.getHeading() > 90 && this.getHeading() < 180)
				turnRight(turnDegree);
			return true;
		}
		if (getX() + Math.sin(Math.toRadians(this.getHeading()))
				* avoidDistance <= 18) {
			// going to hit west wall
			System.out.println("going to hit west wall-------------------");
			if (this.getHeading() > 270 && this.getHeading() < 360)
				turnRight(turnDegree);
			if (this.getHeading() > 180 && this.getHeading() < 270)
				turnLeft(turnDegree);
			return true;
		}
		if (getY() + Math.cos(Math.toRadians(this.getHeading()))
				* avoidDistance >= fieldHeight - 36) {
			// going to hit north wall
			System.out.println("going to hit north wall-------------------");
			if (this.getHeading() > 270 && this.getHeading() < 360)
				turnLeft(turnDegree);
			if (this.getHeading() > 0 && this.getHeading() < 90)
				turnRight(turnDegree);
			return true;
		}
		if (getY() + Math.cos(Math.toRadians(this.getHeading()))
				* avoidDistance <= 18) {
			// going to hit south wall
			System.out.println("going to hit south wall-------------------");
			if (this.getHeading() > 90 && this.getHeading() < 180)
				turnLeft(turnDegree);
			if (this.getHeading() > 180 && this.getHeading() < 270)
				turnRight(turnDegree);
			return true;
		}
		return false;
	}
}
