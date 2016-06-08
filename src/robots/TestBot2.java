package robots;

import static robocode.util.Utils.normalRelativeAngleDegrees;
import java.awt.Color;
import helper.Enums.*;
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
	private double moveDirection = -1;
	private AvoidWall avoidWall = AvoidWall.None;

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

	/**
	 * calc needed fire power from distance and life of the enemy
	 * 
	 * @param e
	 *            ScannedRobotEvent
	 */
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
		detectCloseWall();
		avoidWall();
		setAhead(moveDirection * 20);
	}

	/**
	 * calc the minimal fire power needed to kill the enemy (max
	 * MAX_BULLET_POWER)
	 * 
	 * @param e
	 *            ScannedRobotEvent
	 * @return fire power
	 */

	private double calcFirePowerLife(ScannedRobotEvent e) {
		double firePower = Rules.MIN_BULLET_POWER;
		double life = e.getEnergy();

		// raise bullet power till max or the damage is higher than life
		while (Rules.getBulletDamage(firePower) < life
				&& firePower <= Rules.MAX_BULLET_POWER) {
			firePower += 0.1;
		}

		return firePower;
	}

	private boolean avoidWall() {
		double turnDegree = 20;
		double heading = this.getHeading();

		if (moveDirection != 1) {
			setBack(4000);
			heading = (heading + 180) % 360;
		} else {
			setAhead(4000);
		}
		if (avoidWall == AvoidWall.East) {
			if (heading > 0 && heading <= 90)
				turnLeft(turnDegree);
			else if (heading > 90 && heading <= 180)
				turnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (avoidWall == AvoidWall.West) {
			if (heading > 270 && heading <= 360)
				turnRight(turnDegree);
			else if (heading > 180 && heading <= 270)
				turnLeft(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (avoidWall == AvoidWall.North) {
			if (heading > 270 && heading <= 360)
				turnLeft(turnDegree);
			else if (heading > 0 && heading <= 90)
				turnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (avoidWall == AvoidWall.South) {
			if (heading > 90 && heading <= 180)
				turnLeft(turnDegree);
			else if (heading > 180 && heading <= 270)
				turnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		return false;
	}

	/**
	 * To separate avoid and detection (problems with movement otherwise)
	 * 
	 * @return true if you will hit the wall soon
	 */

	private boolean detectCloseWall() {
		double fieldWith = getBattleFieldWidth();
		double fieldHeight = getBattleFieldHeight();
		double avoidDistance = Math.abs(60 + 10 * Rules.MAX_VELOCITY);

		System.out.println("velocity: " + this.getVelocity());
		double heading = this.getHeading();

		if (moveDirection != 1) {
			heading = (heading + 180) % 360;
		}

		double xDist = getX() + Math.sin(Math.toRadians(heading))
				* avoidDistance;
		double yDist = getY() + Math.cos(Math.toRadians(heading))
				* avoidDistance;

		if (xDist >= fieldWith - 36 || avoidWall == AvoidWall.East) {
			// going to hit east wall
			avoidWall = AvoidWall.East;
			return true;
		}
		if (xDist <= 18 || avoidWall == AvoidWall.West) {
			// going to hit west wall
			avoidWall = AvoidWall.West;
			return true;
		}
		if (yDist >= fieldHeight - 36 || avoidWall == AvoidWall.North) {
			// going to hit north wall
			avoidWall = AvoidWall.North;
			return true;
		}
		if (yDist <= 18 || avoidWall == AvoidWall.South) {
			// going to hit south wall
			avoidWall = AvoidWall.South;
			return true;
		}
		return false;
	}
}
