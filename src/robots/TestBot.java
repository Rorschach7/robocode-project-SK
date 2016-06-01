package robots;

import robocode.*;
import static robocode.util.Utils.*;

import java.awt.Color;
import java.util.Random;

import robocode.ScannedRobotEvent;
import robocode.util.Utils;

enum State {
	Scanning, Attacking, Evading, Finishing
}

enum MovementPattern {
	Circle, Eight, Scanning, Approach, Stop, UpAndDown
}

public class TestBot extends TeamRobot {

	// Variables
	private int moveDirection = 1;// >0 : turn right, <0 : tun left
	private int turnDirection = 1;
	private int count = 0; // Count for movement patterns
	private double EnergyThreshold = 15;
	private boolean scanStarted = false;
	private State state = State.Evading;
	private MovementPattern movePattern = MovementPattern.Stop;

	// Time Handles in rounds
	private double scanElapsedTime;
	private double scanTimer = 10; // time elapses between scans

	private EnemyBot[] enemies;
	private EnemyBot attacker = new EnemyBot(); // Robot which last attacked us
	private EnemyBot target = new EnemyBot();
	private AvoidWall avoidWall;

	// Statistics
	private int shotsHit = 0;
	private int shotsMissed = 0;

	private boolean bulletHit;

	private boolean hitRobot;

	public void run() {

		// Color
		setBodyColor(Color.black);
		setGunColor(Color.black);
		setRadarColor(Color.yellow);
		setBulletColor(Color.green);

		setAdjustGunForRobotTurn(true);
		// setAdjustRadarForRobotTurn(true);
		// setAdjustRadarForGunTurn(true);

		state = State.Scanning;

	}

	public void onScannedRobot(ScannedRobotEvent e) {
		update(e);

		// System.out.println("---Begin---");
		// for(int i = 0;i < enemies.length; i++) {
		// if(enemies[i] != null) {
		// System.out.println(enemies[i].getName());
		// }
		// }
		// System.out.println("---End---");

	}

	public void onHitByBullet(HitByBulletEvent event) {
		attacker.init(event);

		if (getEnergy() <= EnergyThreshold) {
			state = State.Evading;
		}
	}

	public void onHitRobot(HitRobotEvent event) {
		hitRobot = true;
	}

	public void onRobotDeath(RobotDeathEvent event) {
		// Remove robot from enemies array
		for (int i = 0; i < enemies.length; i++) {
			if (enemies[i].getName().equals(event.getName())) {
				enemies[i] = null;
				return;
			}
		}
		if (target.getName().equals(event.getName())) {
			// TODO: Assign new target
		}
	}

	public void onHitWall(HitWallEvent event) {

		if (movePattern == MovementPattern.UpAndDown) {
			moveDirection *= -1;
			setAhead(moveDirection * 5);
		}

		// TODO:
		// Implement a DELAYED change of the current movePattern, so we wont
		// crash into the wall again
	}

	public void onBulletMissed(BulletMissedEvent event) {
		shotsMissed++;
	}

	public void onBulletHit(BulletHitEvent event) {
		shotsHit++;
		bulletHit = true;
	}

	public void onRoundEnded(RoundEndedEvent event) {
		// TODO: Victory Dance
	}

	public void onStatus(StatusEvent event) {

		// Increment Time Handler
		if (!scanStarted) {
			scanElapsedTime++;
		}

		// Periodic scan
		if (scanElapsedTime >= scanTimer) {
			if (enemies != null && enemies.length > 1) {
				state = State.Scanning;
			}
			scanElapsedTime = 0;
		}

		// Avoid walls
		detectCloseWall();
		avoidWall();

		// Execute behavior for corresponding state
		if (state == State.Attacking) {

			// Find Target
			findTarget();

			// Lock radar on target
			lockRadarOnTarget();

			// fireGun(target);

			// Run Attacking movement pattern/strategy
			// TODO:
			runMovementPattern(MovementPattern.UpAndDown); // Needs to be
															// adjusted, should
															// try to get closer
															// to enemy etc
		}

		if (state == State.Scanning) {

			runMovementPattern(MovementPattern.UpAndDown);

			if (!scanStarted) {
				// make a short scan of the whole battlefield
				System.out.println("Executing Scan");
				setTurnRadarRight(360);
				scanStarted = true;
			} else if (getRadarTurnRemaining() < 10) {

				// make a short scan of the whole battlefield
				System.out.println("Scan finished");
				scanStarted = false;
				state = State.Attacking;
			}
		}

		if (state == State.Evading) {
			// TODO:
			// Implement anti gravity stuff here
			// Or short evasive maneuver
			runMovementPattern(MovementPattern.Stop);

		}

		if (state == State.Finishing) {
			// TODO:
			// Implement aggressive close combat behavior here, don't let enemy
			// escape

		}

		// printStatus();

	}

	/**
	 * Executes the specified movement pattern
	 * 
	 * @param pattern
	 *            the movement pattern that should be executed
	 */
	private void runMovementPattern(MovementPattern pattern) {
		movePattern = pattern;

		// Pattern Eight
		if (pattern == MovementPattern.Eight) {

			if (count < 80) {
				setTurnRight(turnDirection * 45);
				setAhead(10);
			} else {
				setTurnRight(turnDirection * -45);
				setAhead(10);
			}

			count++;
			if (count == 160) {
				count = 0;
			}
			return;
		}

		// Pattern Circle
		if (pattern == MovementPattern.Circle) {

			setTurnRight(turnDirection * 10);
			setAhead(10);

			count++;
			if (count == 100) {
				count = 0;
			}
			return;
		}

		// Pattern Scanning
		if (pattern == MovementPattern.Scanning) {

			setTurnRight(10);
			setAhead(35);

			count++;
			if (count == 100) {
				count = 0;
			}
			return;
		}

		// Pattern Approaching
		if (pattern == MovementPattern.Approach) {

		}

		// Pattern Stop
		if (pattern == MovementPattern.Stop) {

			// Do nothing

		}

		// Pattern Up and Down
		if (pattern == MovementPattern.UpAndDown) {

			setAhead(moveDirection * 10);

		}

	}

	/**
	 * Prints current status information
	 */
	public void printStatus() {
		System.out
				.println("---------------------------------------------------------");
		System.out.println("Current MovementPattern: " + movePattern.name());
		System.out.println("Count: " + count);
		System.out.println("Target: " + target.getName());
		System.out.println("Attacker: " + attacker.getName());
		System.out.println("State: " + state);
		System.out.println("Shots Fired: " + (shotsHit + shotsMissed));
		System.out.println("Shots Hits: " + shotsHit);
		System.out.println("Shots Missed: " + shotsMissed);
		double acc = 0;
		if ((shotsHit + shotsMissed) != 0) {
			acc = shotsHit / (shotsHit + shotsMissed) * 100;
		}
		System.out.println("Accuracy: " + acc);
		System.out
				.println("---------------------------------------------------------");
	}

	/**
	 * Updates the enemies array
	 * 
	 * @param robot
	 *            the robot that should be updated
	 */
	public void update(ScannedRobotEvent robot) {

		// Scan energy
		if (!(target.getName().equals("None"))
				&& target.getName().equals(robot.getName())) {
			double enemyDeltaEnergy = target.getInfo().getEnergy()
					- robot.getEnergy();
			if (enemyDeltaEnergy > 0) {
				avoidBullet(enemyDeltaEnergy);
			}
		}

		// Update robot
		if (enemies != null) {
			for (int i = 0; i < enemies.length; i++) {
				if (enemies[i] == null) {
					continue;
				}
				if (enemies[i].getName().equals(robot.getName())) {
					enemies[i].init(robot);
					return;
				}
			}
		}

		// Add not existing robot
		int n = 0;
		if (enemies == null) {
			n = 1;
		} else {
			n = enemies.length + 1;
		}
		EnemyBot[] newEnemies = new EnemyBot[n];

		for (int i = 0; i < n - 1; i++) {
			if (enemies[i] != null) {
				newEnemies[i] = enemies[i];
			}
		}
		newEnemies[n - 1] = new EnemyBot();
		newEnemies[n - 1].init(robot);
		enemies = newEnemies;
	}

	/**
	 * Fires the tank's gun. Uses linear targeting and gun power dependent on
	 * distance between tank and enemy
	 * 
	 * @param target
	 *            the robot we want to shoot
	 */
	private void fireGun(EnemyBot target) {
		ScannedRobotEvent enemy = target.getInfo();
		double absBearing = enemy.getBearing() + getHeading();
		double gunTurnAmt;

		// TODO: Skip linear targeting if enemy is too close
		// Calculate enemie's lateral velocity
		double latVel = enemy.getVelocity()
				* Math.sin(enemy.getHeadingRadians()
						- Math.toRadians(absBearing));

		double bulletSpeed = 20 - 3 * (400 / target.getInfo().getDistance());

		gunTurnAmt = normalRelativeAngleDegrees(absBearing - getGunHeading()
				+ ((latVel / bulletSpeed) * 57.3));
		setTurnGunRight(gunTurnAmt); // Turn gun

		if (target.getInfo().getDistance() < 600
				&& getEnergy() > EnergyThreshold) {
			if (Math.abs(getGunTurnRemaining()) <= 5) { // Don't shoot before
														// gun is adjusted
				setFire(400 / target.getInfo().getDistance());
				// System.out.println("FIRE");
			}
		}
		return;
	}

	/**
	 * Calculates future position and checks whether the tank will collide with
	 * a wall or not.
	 * 
	 * @return true, if wall will be hit. Otherwise false.
	 */
	private boolean avoidWall() {
		// TODO: consider velocity for detection
		// TODO: block other movement
		double turnDegree = 10;
		double heading = this.getHeading();

		if (moveDirection != 1) {
			heading = (heading + 180) % 360;
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
		double avoidDistance = 100;

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

	/**
	 * Tries to figure out if enemy tank shoots at us and starts evasive
	 * maneuver if
	 * 
	 * @param deltaEnergy
	 *            the energy the enemy tank lost between last and current turn
	 */
	private void avoidBullet(double deltaEnergy) {

		if (deltaEnergy > 3 || bulletHit || hitRobot) {
			bulletHit = false;
			hitRobot = false;
			return;
		}

		// TODO:
		System.out.println("AVOID: " + deltaEnergy);
		moveDirection *= -1;
		Random rand = new Random();
		int rnd = rand.nextInt(6) + 3;
		setMaxVelocity(rnd);
	}

	/**
	 * Make sure our radar locks on our current target
	 */
	private void lockRadarOnTarget() {

		if (target.getName().equals("None")) {
			return;
		}

		double angleToEnemy = getHeading() + target.getInfo().getBearing();

		System.out.println("--------------");
		System.out.println("Bearing: " + target.getInfo().getBearing());
		System.out.println("Own Heading: " + getHeading());
		System.out.println("Angle to Enemy: " + angleToEnemy);
		double radarTurn = Utils.normalRelativeAngleDegrees(angleToEnemy
				- getRadarHeading());

		System.out.println("Radar Turn: " + radarTurn);
		System.out.println("Current Radar: " + getRadarHeading());
		System.out.println("Remaining: " + getRadarTurnRemaining());
		System.out.println("--------------");
		setTurnRadarRight(radarTurn);

	}

	/**
	 * Finds a target among all spotted enemies
	 */
	private void findTarget() {

		if (enemies == null) {
			return;
		}

		target = enemies[0];

		for (int i = 0; i < enemies.length; i++) {
			if (target.getInfo().getDistance() > enemies[i].getInfo()
					.getDistance()) {
				target = enemies[i];
			}
		}

	}

}