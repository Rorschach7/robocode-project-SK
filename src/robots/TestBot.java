package robots;

import robocode.*;
import helper.*;
import helper.Enums.*;
import helper.strategies.DynamicChange;
import helper.strategies.GunStrategy;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import robocode.util.Utils;
import javafx.geometry.Point2D;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class TestBot extends TeamRobot {

	public static boolean periodicScan = false;

	// Variables
	private int nr;
	private boolean gameOver = false;
	private int moveDirection = 1;// >0 : turn right, <0 : tun left
	private int turnDirection = 1;
	private int count = 0; // Count for movement patterns
	private double EnergyThreshold = 15;
	private boolean scanStarted = false;
	private boolean bulletHit;
	private boolean hitRobot;
	private boolean isEnemyLocked = false;
	private double bulletVelocity;
	private boolean isEvading;
	private Point randPoint;
	private int direction;

	// States
	private State state = State.Scanning;
	private MovementPattern movePattern = MovementPattern.Stop;
	private RadarState radarState;
	private FireMode fireMode = FireMode.GuessFactor;

	// Time Handles in rounds
	private double scanElapsedTime;
	private double scanTimer = 10; // time elapses between scans
	private int sweepScanCount = 0;
	private ArrayList<Bot> enemies = new ArrayList<>();
	private ArrayList<Bot> team = new ArrayList<>();
	private Bot attacker = new Bot(); // Robot which last attacked us
	private Bot target = new Bot();
	private AvoidWall avoidWall;

	// Statistics
	List<Data> dataList = new ArrayList<>();
	private List<WaveBullet> waves = new ArrayList<WaveBullet>();
	private boolean bestScore = true;
	private double hits;
	private double misses;

	// Strategies
	private GunStrategy gunStrategy = new DynamicChange();

	public void run() {

		// Assign number
		if (getTeammates() != null) {
			int start = getName().indexOf("(");
			int end = getName().indexOf(")");
			nr = Integer.parseInt(getName().substring(start + 1, end));
		}

		// Color
		setBodyColor(Color.gray);
		setGunColor(Color.blue);
		setRadarColor(Color.blue);
		setBulletColor(Color.green);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		state = State.Scanning;

		while (true) {
			scan();
		}
	}

	/**
	 * gets the absolute bearing between to x,y coordinates
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public double absBearing(double x1, double y1, double x2, double y2) {
		double xo = x2 - x1;
		double yo = y2 - y1;
		double h = getRange(x1, y1, x2, y2);
		if (xo > 0 && yo > 0) {
			return Math.asin(xo / h);
		}
		if (xo > 0 && yo < 0) {
			return Math.PI - Math.asin(xo / h);
		}
		if (xo < 0 && yo < 0) {
			return Math.PI + Math.asin(-xo / h);
		}
		if (xo < 0 && yo > 0) {
			return 2.0 * Math.PI - Math.asin(-xo / h);
		}
		return 0;
	}

	private int midpointcount = 0; // Number of turns since that strength was
									// changed.
	private double midpointstrength = 0; // The strength of the gravity point in
											// the

	private double evadeRounds;

	// middle of the field
	/**
	 * the anti Gravity move
	 */
	private void antiGravMove() {
		double xforce = 0;
		double yforce = 0;
		double force;
		double ang;
		GravPoint p;
		ArrayList<Bot> allBots = enemies;
		for (Bot bot : team) {
			if (!bot.equals(this)) {
				allBots.add(bot);
			}
		}
		// cycle through all bots. If they are alive, they are repulsive.
		// Calculate the force on us

		for (Bot bot : allBots) {
			if (!bot.isDead()) {

				double angleToBot = bot.getInfo().getBearing();

				// Calculate the angle to the scanned robot
				double angle = Math
						.toRadians((getHeading() + angleToBot % 360));

				// Calculate the coordinates of the robot
				double botX = (getX() + Math.sin(angle)
						* bot.getInfo().getDistance());
				double botY = (getY() + Math.cos(angle)
						* bot.getInfo().getDistance());

				p = new GravPoint(botX, botY, -1000);

				force = p.power
						/ Math.pow(getRange(getX(), getY(), p.x, p.y), 2);

				// Find the bearing from the point to us
				ang = normaliseBearing(Math.PI / 2
						- Math.atan2(getY() - p.y, getX() - p.x));
				// Add the components of this force to the total force in their
				// respective directions
				xforce += Math.sin(ang) * force;
				yforce += Math.cos(ang) * force;
			}
		}

		/**
		 * The next section adds a middle point with a random (positive or
		 * negative) strength. The strength changes every 5 turns, and goes
		 * between -1000 and 1000. This gives a better overall movement.
		 **/

		midpointcount++;
		if (midpointcount > 5) {
			midpointcount = 0;
			midpointstrength = (Math.random() * 2000) - 1000;
		}
		p = new GravPoint(getBattleFieldWidth() / 2,
				getBattleFieldHeight() / 2, midpointstrength);
		force = p.power / Math.pow(getRange(getX(), getY(), p.x, p.y), 1.5);
		ang = normaliseBearing(Math.PI / 2
				- Math.atan2(getY() - p.y, getX() - p.x));
		xforce += Math.sin(ang) * force;
		yforce += Math.cos(ang) * force;

		/**
		 * The following four lines add wall avoidance. They will only affect us
		 * if the bot is close to the walls due to the force from the walls
		 * decreasing at a power 3.
		 **/
		xforce += 5000 / Math.pow(
				getRange(getX(), getY(), getBattleFieldWidth(), getY()), 3);
		xforce -= 5000 / Math.pow(getRange(getX(), getY(), 0, getY()), 3);
		yforce += 5000 / Math.pow(
				getRange(getX(), getY(), getX(), getBattleFieldHeight()), 3);
		yforce -= 5000 / Math.pow(getRange(getX(), getY(), getX(), 0), 3);

		// Move in the direction of our resolved force.
		goTo(getX() - xforce, getY() - yforce);
	}

	// if a bearing is not within the -pi to pi range, alters it to provide the
	// shortest angle
	private double normaliseBearing(double ang) {
		if (ang > Math.PI)
			ang -= 2 * Math.PI;
		if (ang < -Math.PI)
			ang += 2 * Math.PI;
		return ang;
	}

	/** Move in the direction of an x and y coordinate **/
	private void goTo(double x, double y) {
		setMaxVelocity(Rules.MAX_VELOCITY);
		double dist = 20;
		double angle = Math.toDegrees(absBearing(getX(), getY(), x, y));
		turnTo(angle);
		setAhead(dist * moveDirection);
	}

	/**
	 * Turns the shortest angle possible to come to a heading, then returns the
	 * direction the bot needs to move in.
	 **/
	private void turnTo(double angle) {
		double ang = normaliseBearing(getHeading() - angle);
		if (ang > 90) {
			ang -= 180;
			moveDirection = -1;
		} else if (ang < -90) {
			ang += 180;
			moveDirection = -1;
		} else {
			moveDirection = 1;
		}
		setTurnLeft(ang);
	}

	/** Returns the distance between two points **/
	private double getRange(double x1, double y1, double x2, double y2) {
		double x = x2 - x1;
		double y = y2 - y1;
		double range = Math.sqrt(x * x + y * y);
		return range;
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		update(e);

		// System.out.println("Scanned Robot: " + e.getName());

		// Sweep scan found target, lock on
		if (radarState == RadarState.Sweep
				&& target.getName().equals(e.getName())) {
			// System.out.println("Sweep found target, lock on");
			radarState = RadarState.Lock;
			isEnemyLocked = true;
		}

		if (radarState == RadarState.Lock) {

			if (target.getName().equals(e.getName())) {
				isEnemyLocked = true;
				runScan(RadarState.Lock);
			}
		}

		// Collect data for guess targeting
		collectData(e);

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

		for (Bot bot : team) {
			if (event.getName().equals(bot.getName())) {
				bot.died();
				team.remove(bot);
				System.out.println("Team mate died");
				return;
			}
		}

		// Mark robot as dead
		for (Bot bot : enemies) {
			if (bot.getName().equals(event.getName())) {
				bot.died();
				return;
			}
		}

		// Our target just died, we need a new one
		if (target.getName().equals(event.getName())) {
			findTarget();
			runScan(RadarState.FullScan);
		}
	}

	public void onHitWall(HitWallEvent event) {

		if (movePattern == MovementPattern.UpAndDown) {
			moveDirection *= -1;
			setAhead(moveDirection * 5);
		}
	}

	public void onBulletMissed(BulletMissedEvent event) {
		findDataByName(target.getName()).BulletHit(false, fireMode);
		misses++;		
	}

	public void onWin(WinEvent event) {
		System.out.println("WINEVENT");
	}

	public void onBulletHit(BulletHitEvent event) {
		findDataByName(target.getName()).BulletHit(true, fireMode);
		bulletHit = true;
		hits++;		
	}

	public void onDeath(DeathEvent event) {
		for (Data data : dataList) {
			data.lost();
		}
		System.out.println("DEFEAT");

		if (getTeammates() == null) {
			saveData();
			return;
		}
	}

	public void onRoundEnded(RoundEndedEvent event) {
		System.out.println("Round ended");

		if (getEnergy() > 0) {
			for (Data data : dataList) {
				data.win();
			}
			System.out.println("VICTORY");
			// TODO: Victory Dance
		}

		// Debug
		// for (Data data : dataList) {
		// data.printData(true);
		// }

		gameOver = true;

		if (getTeammates() == null) {
			saveData();
			return;
		}

		// Broadcast our score to the team
		double score = (dataList.get(0).getGuessTargetingHits() + dataList.get(
				0).getGuessTargetingMissed())
				/ dataList.get(0).getGuessTargetingHits();
		try {
			broadcastMessage("SCORE " + score);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void onMessageReceived(MessageEvent event) {

		String msg = event.getMessage().toString();
		int ind = msg.indexOf(" ");
		String start = msg.substring(0, ind);
		String info = msg.substring(ind + 1);

		if (start.equals("ALIVE")) {
			System.out.println(info + " is alive.");
			for (Bot bot : team) {
				if (bot.getName().equals(info)) {
					bot.died();
				}
			}
		}

		if (start.equals("SCORE")) {
			double score = (dataList.get(0).getGuessTargetingHits() + dataList
					.get(0).getGuessTargetingMissed())
					/ dataList.get(0).getGuessTargetingHits();
			double msgScore = Double.parseDouble(info);
			if (msgScore > score) {
				bestScore = false;
			}
			for (Bot bot : team) {
				if (bot.getName().equals(event.getSender())) {
					team.remove(bot);
					break;
				}
			}
			if (team.isEmpty() && bestScore) {
				System.out.println("I have the best score " + score + " "
						+ getName());
				saveData();
			}
		}
	}

	public void onStatus(StatusEvent event) {
		if (gameOver) {
			return;
		}

		// TODO: when should this run
		if (target.getInfo() != null) {
			// TODO: hat bei rand dazwischen gepfuscht ;D
			// antiGravMove();
		}

		// Increment Time Handler
		if (!scanStarted) {
			scanElapsedTime++;
		}

		// Periodic scan
		if (scanElapsedTime >= scanTimer) {
			if (enemies != null && enemies.size() > 1 && periodicScan) {

				state = State.Scanning;
			}
			scanElapsedTime = 0;
		}

		// Execute behavior for corresponding state
		if (state == State.Attacking) {

			// Find Target
			findTarget();

			// Radar Scanning
			// FullScan finished, start sweep scan
			if (!scanStarted && radarState == RadarState.FullScan) {
				System.out.println("Full scan finished.");
				// Sweep search for our target at last known position
				runScan(RadarState.Sweep);
			}

			if (isEnemyLocked) {
				fireGun();
			} else {
				System.out.println("Enemy no longer locked.");
				// Use sweep to find target again
				// do a full scan if target cannot be found after five rounds
				if (sweepScanCount < 10
						&& (radarState == RadarState.Lock || radarState == RadarState.Sweep)) {
					runScan(RadarState.Sweep);
				} else {
					runScan(RadarState.FullScan);
					sweepScanCount = 0;
				}
			}

			// TODO:
			runMovementPattern(MovementPattern.AntiGravity); // Needs to be
																// adjusted,
			// should try to get
			// closer to enemy etc
		}

		if (state == State.Scanning) {

			runMovementPattern(MovementPattern.Stop);

			runScan(RadarState.FullScan);
		}

		if (state == State.Evading) {
			// TODO maybe add more than evading
			runMovementPattern(MovementPattern.Random);
		}

		// printStatus();
		isEnemyLocked = false;
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

		// Pattern Stop
		if (pattern == MovementPattern.Stop) {

			// Do nothing
			setMaxVelocity(0);
		}

		if (pattern == MovementPattern.AntiGravity) {
			antiGravMove();
		}

		// Pattern Up and Down
		if (pattern == MovementPattern.UpAndDown) {
			setMaxVelocity(Rules.MAX_VELOCITY * 0.75);
			setAhead(moveDirection * 10);

		}

		if (pattern == MovementPattern.Random) {
			if (isEvading) {
				System.out.println("evading");
				goTo(randPoint.getX(), randPoint.getY());
				evadeRounds--;
				// check if we reached desired position
				if (new Point((int) getX(), (int) getY()).distance(randPoint) < 10
						|| evadeRounds < 0) {
					isEvading = false;
					state = State.Attacking;
					System.out.println("reached point");
				}
			} else {
				System.out.println("Start randomMovement");
				randPoint = randomMovement();
				isEvading = true;
			}
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
		System.out.println("RadarState: " + radarState);
		System.out
				.println("---------------------------------------------------------");
	}

	/**
	 * Updates the enemies array and the team array
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
				detectBullet(enemyDeltaEnergy);
			}
		}

		if (isTeammate(robot.getName())) {
			// Team mate
			// Update a team mate
			for (Bot bot : team) {
				if (bot.getName().equals(robot.getName())) {
					bot.init(robot);
					return;
				}
			}

			// Add new team mate
			Bot bot = new Bot();
			bot.init(robot);
			team.add(bot);

		} else {
			// Enemy
			// Updtae an enemie
			if (enemies != null) {

				for (Bot bot : enemies) {
					if (bot.getName().equals(robot.getName())) {
						bot.init(robot);
						return;
					}
				}
			}

			// Add not existing enemy robot
			Bot bot = new Bot();
			bot.init(robot);
			enemies.add(bot);

			// Assign scanned Robot to a Data structure
			String robotName = robot.getName();

			// Clean up name
			if (robot.getName().contains(" ")) {
				int i = robot.getName().indexOf(" ");
				robotName = robot.getName().substring(0, i);
				// System.out.println("Multiple Instances: " + robotName);
			}

			// Check if we already added this kind of robot
			for (Data data : dataList) {
				if (data.getRobotName().equals(robotName)) {
					// A data object for this kind of robot already exists,
					// abort
					System.out
							.println("A data object for this kind of robot already exists, abort");
					return;
				}
			}

			// Create data object, that we cann add to our list
			Data data;

			if (checkForData(robotName)) {
				// A data file already exists, so load it
				System.out.println("Load File.");
				data = loadData(robotName);

				if (data == null) {
					System.out
							.println("File was not found, create new data file");
					dataList.add(new Data(robotName));
					return;
				}
				dataList.add(data);
				System.out.println("Added " + data + " to DataList.");
			} else {
				System.out.println("No File found.");
				data = new Data(robotName);
				dataList.add(data);
			}

		}
	}

	/**
	 * Fires the tank's gun. Uses linear targeting and gun power dependent on
	 * distance between tank and enemy
	 * 
	 * @param target
	 *            the robot we want to shoot
	 */
	private void fireGun() {
		gunStrategy.execute(this);
	}

	/**
	 * Calculates future position and checks whether the tank will collide with
	 * a wall or not.
	 * 
	 * @return true, if wall will be hit. Otherwise false.
	 */
	private boolean avoidWall() {
		double turnDegree = 10;
		double heading = this.getHeading();

		if (moveDirection != 1) {
			heading = (heading + 180) % 360;
		}
		if (avoidWall == AvoidWall.East) {
			if (heading > 0 && heading <= 90)
				setTurnLeft(turnDegree);
			else if (heading > 90 && heading <= 180)
				setTurnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (avoidWall == AvoidWall.West) {
			if (heading > 270 && heading <= 360)
				setTurnRight(turnDegree);
			else if (heading > 180 && heading <= 270)
				setTurnLeft(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (avoidWall == AvoidWall.North) {
			if (heading > 270 && heading <= 360)
				setTurnLeft(turnDegree);
			else if (heading > 0 && heading <= 90)
				setTurnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (avoidWall == AvoidWall.South) {
			if (heading > 90 && heading <= 180)
				setTurnLeft(turnDegree);
			else if (heading > 180 && heading <= 270)
				setTurnRight(turnDegree);
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
	private AvoidWall detectCloseWall(double heading) {
		double fieldWith = getBattleFieldWidth();
		double fieldHeight = getBattleFieldHeight();
		double avoidDistance = 120;
		AvoidWall aW = AvoidWall.None;

		if (moveDirection != 1) {
			heading = (heading + 180) % 360;
		}

		double xDist = getX() + Math.sin(Math.toRadians(heading))
				* avoidDistance;
		double yDist = getY() + Math.cos(Math.toRadians(heading))
				* avoidDistance;

		if (xDist >= fieldWith - 36 || aW == AvoidWall.East) {
			// going to hit east wall
			aW = AvoidWall.East;
		}
		if (xDist <= 18 || aW == AvoidWall.West) {
			// going to hit west wall
			aW = AvoidWall.West;
		}
		if (yDist >= fieldHeight - 36 || aW == AvoidWall.North) {
			// going to hit north wall
			aW = AvoidWall.North;
		}
		if (yDist <= 18 || aW == AvoidWall.South) {
			// going to hit south wall
			aW = AvoidWall.South;
		}
		return aW;
	}

	/**
	 * Tries to figure out if enemy tank shoots at us and starts evasive
	 * maneuver if
	 * 
	 * @param deltaEnergy
	 *            the energy the targeted tank lost between last and current
	 *            turn
	 */
	private void detectBullet(double deltaEnergy) {

		if (deltaEnergy > 3 || bulletHit || hitRobot) {
			bulletHit = false;
			hitRobot = false;
			return;
		}

		// Check if enemy hit a wall
		double wallDmg = target.getInfo().getVelocity() * 0.5 - 1;
		if (deltaEnergy == wallDmg) {
			return;
		}

		// TODO bot hits bot

		bulletVelocity = 20 - 3 * deltaEnergy;

		// TODO
		state = State.Evading;
		// runMovementPattern(MovementPattern.Random);
	}

	/**
	 * returns a random Point to evade to, which is not inside a wall or
	 * straight to/away from the enemy
	 * 
	 * @return Point point to evade to
	 */

	private Point randomMovement() {
		ScannedRobotEvent bot = target.getInfo();
		double botBearing = bot.getBearing();
		double heading = getHeading();
		Rectangle field = new Rectangle(new Point(16, 16), new Dimension(
				(int) getBattleFieldWidth() - 32,
				(int) getBattleFieldHeight() - 32));

		// TODO: change deltaAngle according to the distance to the enemy
		double deltaAngle = 100;

		Random rand = new Random();
		Point target = new Point();
		double randAngle;

		// Random velocity between 5 and 8
		double velo = 5 + new Random().nextDouble() * 3;
		setMaxVelocity(velo);

		double turnsTillBulletHits = bot.getDistance() / bulletVelocity;

		// to stop rand movement when the bullet passed or
		evadeRounds = turnsTillBulletHits * 2 / 3;

		// distance to move to dodge bullet
		double dist = turnsTillBulletHits * 2 / 3 * velo;

		// gives a random angle which is not to the enemy or the opposite
		// direction
		do {
			randAngle = rand.nextDouble() * 360;
			double randAngTotal = (randAngle + heading + botBearing - deltaAngle / 2) % 360;
			if (randAngTotal < 0) {
				randAngTotal += 360;
			}
			double tx = getX() + dist * Math.sin(Math.toRadians(randAngTotal));
			double ty = getY() + dist * Math.cos(Math.toRadians(randAngTotal));
			target.setLocation(tx, ty);
		} while (randAngle > 0 && randAngle < deltaAngle || randAngle > 180
				&& randAngle < 180 + deltaAngle || !field.contains(target));

		// System.out.println("pos: " + getX() + " " + getY());
		// System.out.println("ang: " + randAngTotal);
		// System.out.println("dist: " + dist);
		// System.out.println("target: " + target);

		return target;
	}

	/**
	 * Finds a target among all spotted enemies
	 */
	private void findTarget() {

		if (enemies == null) {
			return;
		}

		for (int i = 0; i < enemies.size(); i++) {
			if (!enemies.get(i).isDead()) {
				target = enemies.get(i);
				break;
			}
		}

		// Find closest enemy
		for (int i = 0; i < enemies.size(); i++) {
			if (enemies.get(i).isDead()) {
				continue;
			}
			if (target.getInfo().getDistance() > enemies.get(i).getInfo()
					.getDistance()) {
				target = enemies.get(i);
			}
		}
	}

	private void runScan(RadarState scan) {

		// System.out.println("RunScan: " + scan);

		if (scan == RadarState.FullScan) {
			if (!scanStarted) {
				// make a short scan of the whole battlefield
				radarState = RadarState.FullScan;
				// System.out.println("Executing Scan");
				setTurnRadarRight(360);
				scanStarted = true;
			} else if (getRadarTurnRemaining() < 10) {
				// Scan finished
				scanStarted = false;
				state = State.Attacking;
			}
		}

		if (scan == RadarState.Sweep) {

			radarState = RadarState.Sweep;
			sweepScanCount++;
			// Absolute angle towards target
			double angleToEnemy = getHeadingRadians()
					+ target.getInfo().getBearingRadians();

			// Subtract current radar heading to get the turn required to face
			// the enemy, be sure it is normalized
			double radarTurn = Utils.normalRelativeAngle(angleToEnemy
					- getRadarHeadingRadians());

			// Distance we want to scan from middle of enemy to either side
			// The 36.0 is how many units from the center of the enemy robot it
			// scans.
			double extraTurn = Math.min(
					Math.atan(36.0 / target.getInfo().getDistance()),
					Rules.RADAR_TURN_RATE_RADIANS);

			// Adjust the radar turn so it goes that much further in the
			// direction it is going to turn
			// Basically if we were going to turn it left, turn it even more
			// left, if right, turn more right.
			// This allows us to overshoot our enemy so that we get a good sweep
			// that will not slip.
			radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);

			// Turn the radar
			setTurnRadarRightRadians(radarTurn);

		}

		if (scan == RadarState.Lock) {

			radarState = RadarState.Lock;

			if (target.getName().equals("None")) {
				System.out.println("No Target assigned.");
				return;
			}

			// double angleToEnemy = getHeading() +
			// target.getInfo().getBearing();
			//
			// double radarTurn = Utils.normalRelativeAngleDegrees(angleToEnemy
			// - getRadarHeading());
			//
			// setTurnRadarRight(radarTurn);

			double angleToEnemy = getHeadingRadians()
					+ target.getInfo().getBearingRadians();

			// Subtract current radar heading to get the turn required to face
			// the enemy, be sure it is normalized
			double radarTurn = Utils.normalRelativeAngle(angleToEnemy
					- getRadarHeadingRadians());

			// Distance we want to scan from middle of enemy to either side
			// The 36.0 is how many units from the center of the enemy robot it
			// scans.
			double extraTurn = Math.min(
					Math.atan(20.0 / target.getInfo().getDistance()),
					Rules.RADAR_TURN_RATE_RADIANS);

			// Adjust the radar turn so it goes that much further in the
			// direction it is going to turn
			// Basically if we were going to turn it left, turn it even more
			// left, if right, turn more right.
			// This allows us to overshoot our enemy so that we get a good sweep
			// that will not slip.
			radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);

			// Turn the radar
			setTurnRadarRightRadians(radarTurn);
		}
	}

	private void collectData(ScannedRobotEvent e) {

		// Collect data
		double absBearing = getHeadingRadians() + e.getBearingRadians();

		// find our enemy's location:
		double ex = getX() + Math.sin(absBearing) * e.getDistance();
		double ey = getY() + Math.cos(absBearing) * e.getDistance();

		// Let's process the waves now:
		for (int i = 0; i < getWaves().size(); i++) {
			WaveBullet currentWave = (WaveBullet) getWaves().get(i);
			if (currentWave.checkHit(ex, ey, getTime())) {
				getWaves().remove(currentWave);
				i--;
			}
		}
	}

	/**
	 * Finds the specefied robot among all spotted enemies.
	 * 
	 * @param name
	 *            The name of the robot that you want.
	 * @return the Robot if already spotted and existing, null Otherwise.
	 */
	private Bot findBotByName(String name) {
		for (int i = 0; i < enemies.size(); i++) {
			if (enemies.get(i).getName().equals(name)) {
				return enemies.get(i);
			}
		}
		return null;
	}

	/**
	 * Tries to load the data file with the specified name, return the object if
	 * found or null if not.
	 * 
	 * @param robotName
	 *            Name of the corresponding robot.
	 * @return The loaded data object or null.
	 */
	private Data loadData(String robotName) {

		System.out.println("Trying to load " + robotName + ".json");

		File file = getDataFile(robotName + ".json");

		Gson gson = new Gson();

		try {
			System.out.println("IN JSON");
			return gson.fromJson(new FileReader(file), Data.class);
		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
			System.out.println("Error while converting from Json");
			e.printStackTrace();
		}

		return null;
	}

	private void saveData() {

		System.out.println("Saving Data " + dataList.size());

		Gson gson = new Gson();

		for (Data data : dataList) {

			try {
				String dataString = gson.toJson(data);
				// File dir = new File(dataDirectory);
				// dir.mkdirs();
				File file = getDataFile(data.getRobotName() + ".json"); // new
																		// File(dataDirectory
																		// + "/"
																		// +
																		// data.getRobotName()
																		// +
																		// ".json");
				RobocodeFileWriter writer = new RobocodeFileWriter(file);
				writer.write(dataString);
				writer.close();
				System.out.println("Data saved to " + file.getAbsolutePath());
			} catch (JsonIOException | IOException e) {
				e.printStackTrace();
			}

		}
	}

	public Data findDataByName(String name) {

		String robotName = name;

		// Check
		if (name.contains(" ")) {
			int j = name.indexOf(" ");
			robotName = name.substring(0, j);
			// System.out.println("Multiple Instances: " + robotName);
		}

		for (Data data : dataList) {
			if (data.getRobotName().equals(robotName)) {
				return data;
			}
		}
		return null;
	}

	private boolean checkForData(String name) {

		String robotName = name;

		// Check
		if (name.contains(" ")) {
			int i = name.indexOf(" ");
			robotName = name.substring(0, i);
			System.out.println("Multiple Instances: " + robotName);
		}

		File file = getDataFile(robotName + ".json");

		return file.exists();
	}

	public boolean checkFriendlyFire() {
		double absBearing = getHeadingRadians()
				+ target.getInfo().getBearingRadians();
		// find our enemy's location:
		double ex = getX() + Math.sin(absBearing)
				* target.getInfo().getDistance();
		double ey = getY() + Math.cos(absBearing)
				* target.getInfo().getDistance();

		Point2D enemy = new Point2D(ex, ey);
		Point2D self = new Point2D(getX(), getY());

		Point2D fwd = new Point2D(Math.sin(getGunHeadingRadians()),
				Math.cos(getGunHeadingRadians()));
		Point2D right = new Point2D(fwd.getY(), -fwd.getX());

		// TODO: use these vectors to create a rectangle in front of our tank
		// and then check if another tank overlaps
		Point2D a = right.multiply(20).add(self);
		Point2D d = right.multiply(-20).add(self);
		Point2D b = a.add(fwd.multiply(self.distance(enemy)));
		Point2D c = d.add(fwd.multiply(self.distance(enemy)));

		double xLo = a.getX() < b.getX() ? a.getX() : b.getX();
		double yLo = a.getY() < b.getY() ? a.getY() : b.getY();

		double xHi = c.getX() < d.getX() ? d.getX() : c.getX();
		double yHi = c.getY() < d.getY() ? d.getY() : c.getY();

		// System.out.println("A " + a);
		// System.out.println("B " + b);
		// System.out.println("C " + c);
		// System.out.println("D " + d);

		// System.out.println("Gun FWD: " + fwd.toString());
		// System.out.println("Tank pos: " + getX() + " " + getY());
		// System.out.println("Lower right corner " + xLo + " " + yLo);
		// System.out.println("Upper left corner " + xHi + " " + yHi);

		for (Bot bot : team) {
			double absBe = getHeadingRadians()
					+ bot.getInfo().getBearingRadians();
			double tx = getX() + Math.sin(absBe) * bot.getInfo().getDistance();
			double ty = getY() + Math.cos(absBe) * bot.getInfo().getDistance();
			if (xLo <= tx && tx <= xHi && yLo <= ty && ty <= yHi) {
				System.out.println("Friendly Fire! " + bot.getName());
				System.out.println(tx + " " + ty);
				return true;
			}
		}
		return false;
	}

	public Bot getTarget() {
		return target;
	}

	public List<WaveBullet> getWaves() {
		return waves;
	}

	public void setWaves(List<WaveBullet> waves) {
		this.waves = waves;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	public double getHits() {
		return hits;
	}
	
	public void setHits(double hits) {
		this.hits = hits;
	}
	
	public double getMisses() {
		return misses;
	}
	
	public void setMisses(double misses) {
		this.misses = misses;
	}
}