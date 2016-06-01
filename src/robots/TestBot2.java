package robots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import robocode.AdvancedRobot;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

enum AvoidWall {
	West, North, East, South, None;
}

public class TestBot2 extends AdvancedRobot {
	private double lastRobotDistance;
	private String lastRobotName;
	private boolean lastRobotDied = false;
	private double moveDirection = 1;
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
 * 	calc needed fire power from distance and life of the enemy
 * 
 * @param e ScannedRobotEvent
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
		avoidWalls();
		setAhead(moveDirection * 20);
	}
/**
 * calc the minimal fire power needed to kill the enemy (max MAX_BULLET_POWER)
 * 
 * @param e ScannedRobotEvent
 * @return fire power
 */
	
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
		double xDist = getX() + Math.sin(Math.toRadians(this.getHeading()))
				* avoidDistance;
		double yDist = getY() + Math.cos(Math.toRadians(this.getHeading()))
				* avoidDistance;

		if (xDist >= fieldWith - 36 || avoidWall == AvoidWall.East) {
			// going to hit east wall
			avoidWall = AvoidWall.East;
			if (this.getHeading() > 0 && this.getHeading() <= 90)
				turnLeft(turnDegree);
			else if (this.getHeading() > 90 && this.getHeading() <= 180)
				turnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (xDist <= 18 || avoidWall == AvoidWall.West) {
			// going to hit west wall
			avoidWall = AvoidWall.West;
			if (this.getHeading() > 270 && this.getHeading() <= 360)
				turnRight(turnDegree);
			else if (this.getHeading() > 180 && this.getHeading() <= 270)
				turnLeft(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (yDist >= fieldHeight - 36 || avoidWall == AvoidWall.North) {
			// going to hit north wall
			avoidWall = AvoidWall.North;
			if (this.getHeading() > 270 && this.getHeading() <= 360)
				turnLeft(turnDegree);
			else if (this.getHeading() > 0 && this.getHeading() <= 90)
				turnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (yDist <= 18 || avoidWall == AvoidWall.South) {
			// going to hit south wall
			avoidWall = AvoidWall.South;
			if (this.getHeading() > 90 && this.getHeading() <= 180)
				turnLeft(turnDegree);
			else if (this.getHeading() > 180 && this.getHeading() <= 270)
				turnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		return false;
	}
	
	/**
	 * To separate avoid and detection (problems with movement otherwise)
	 * (TODO)
	 * @return true if you will hit the wall soon
	 */
	
	private boolean detectCloseWall(){
		//TODO
		
		return false;
	}
}
