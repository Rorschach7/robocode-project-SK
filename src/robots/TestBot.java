package robots;

import robocode.*;
import helper.*;
import helper.Enums.*;
import helper.strategies.*;
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import robocode.util.Utils;
import javafx.geometry.Point2D;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class TestBot extends TeamRobot {
	public static int BINS = 47;

	public static boolean periodicScan = false;
	public static Rectangle2D.Double _fieldRect = new java.awt.geom.Rectangle2D.Double(
			18, 18, 764, 564);
	public static double WALL_STICK = 120;
	public ArrayList<Integer> _surfDirections;
	public ArrayList<Double> _surfAbsBearings;
	public Point _myLocation; // our bot's location
	public static double _surfStats[] = new double[BINS]; // we'll use 47 bins

	// Variables
	private int nr;
	private boolean gameOver = false;
	private int moveDirection = 1;// >0 : turn right, <0 : tun left	
	private double EnergyThreshold = 15;
	private boolean scanStarted = false;
	private boolean bulletHit;
	private boolean hitRobot;
	private boolean isEnemyLocked = false;
	private double bulletVelocity;	
	private int fireDirection; 

	// States
	private State state = State.Scanning;	
	private RadarState radarState;	

	// Time Handles in rounds
	private double scanElapsedTime;
	private double scanTimer = 10; // time elapses between scans
	private int sweepScanCount = 0;
	private ArrayList<Bot> enemies = new ArrayList<>();
	private ArrayList<Bot> team = new ArrayList<>();
	private Bot attacker = new Bot(); // Robot which last attacked us
	private Bot target = new Bot();	

	// Statistics
	List<Data> dataList = new ArrayList<>();
	private List<WaveBullet> waves = new ArrayList<WaveBullet>();
	private boolean bestScore = true;
	private double hits;
	private double misses;

	// Strategies
	// Targeting
	private GunStrategy aimStrategy = new LinTargeting();
	// Movement
	private MovementStrategy attackingMovement = new StopMovement(); // Used most of the time
	private MovementStrategy scanningMovement = new StopMovement(); // Used when we're performing 360 scan
	private MovementStrategy dodgeBullet = new RandomMovement(); // Used to dodge incoming bullet
	private MovementStrategy victoryDance = new SpinAroundMovement(); // Use for victory dance

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
		_surfDirections = new ArrayList<Integer>();
		_surfAbsBearings = new ArrayList<Double>();
		setState(State.Scanning);


		while (true) {
			scan();
		}
	}	

	/** Move in the direction of an x and y coordinate **/
	public void goTo(double x, double y) {
		setMaxVelocity(Rules.MAX_VELOCITY);
		double dist = 20;
		double angle = Math.toDegrees(FuncLib.absBearing(getX(), getY(), x, y));
		turnTo(angle);
		setAhead(dist * moveDirection);
	}

	/**
	 * Turns the shortest angle possible to come to a heading, then returns the
	 * direction the bot needs to move in.
	 **/
	private void turnTo(double angle) {
		double ang = FuncLib.normaliseBearing(getHeading() - angle);
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
	
	public void onScannedRobot(ScannedRobotEvent e) {
		update(e);
//		collectWaveSurfData(e);
		
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
			setState(State.Evading);
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
		// Should never happen
		System.out.println("We crashed into a wall. How could that happen? :(");
	}

	public void onBulletMissed(BulletMissedEvent event) {
		findDataByName(target.getName()).BulletHit(false, aimStrategy);
		misses++;		
	}

	public void onWin(WinEvent event) {
		System.out.println("WINEVENT");
	}

	public void onBulletHit(BulletHitEvent event) {
		findDataByName(target.getName()).BulletHit(true, aimStrategy);
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
			victoryDance.execute(this);
		}

		// Debug
		// for (Data data : dataList) {
		// data.printData(true);
		// }
		
		System.out.println(aimStrategy + " acc: " + aimStrategy.getAccuracy(this));

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
		
		// Increment Time Handler
		if (!scanStarted) {
			scanElapsedTime++;
		}
		
		// Periodic scan
		if (scanElapsedTime >= scanTimer) {
			if (enemies != null && enemies.size() > 1 && periodicScan) {
				setState(State.Scanning);
			}
			scanElapsedTime = 0;
		}
		
		System.out.println();

		// Execute behavior for corresponding state
		if (getState() == State.Attacking) {
			
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
				aimStrategy.execute(this);
			} else {
				System.out.println("Enemy no longer locked.");
				// Use sweep to find target again
				// do a full scan if target cannot be found after given rounds
				if (sweepScanCount < 10
						&& (radarState == RadarState.Lock || radarState == RadarState.Sweep)) {
					runScan(RadarState.Sweep);
				} else {
					runScan(RadarState.FullScan);
					sweepScanCount = 0;
				}
			}
			
			attackingMovement.execute(this);
		}

		if (getState() == State.Scanning) {
			scanningMovement.execute(this);
			runScan(RadarState.FullScan);
		}

		if (getState() == State.Evading) {
			// Execute avoiding movement strategy	
			aimStrategy.execute(this);
			dodgeBullet.execute(this);			
		}

		// printStatus();
		isEnemyLocked = false;

	}	

	/**
	 * Prints current status information
	 */
	public void printStatus() {
		System.out
				.println("---------------------------------------------------------");				
		System.out.println("Target: " + target.getName());
		System.out.println("Attacker: " + attacker.getName());
		System.out.println("State: " + getState());
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
			// Update an enemie
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
	
	private void collectWaveSurfData(ScannedRobotEvent robot){
		Double energyDrop = 0.0;
		if (!(target.getName().equals("None"))
				&& target.getName().equals(robot.getName())) {
			double enemyDeltaEnergy = target.getInfo().getEnergy()
					- robot.getEnergy();
			if (enemyDeltaEnergy > 0) {
				energyDrop = enemyDeltaEnergy;
			}
		}
		
		System.out.println("start waveSurfing");
		// wave surfing stuff
		ScannedRobotEvent targetBot = target.getInfo();
		double absBearing = getHeadingRadians()
				+ targetBot.getBearingRadians();
		double lateralVelocity = getVelocity()
				* Math.sin(targetBot.getBearingRadians());
		_surfDirections.add(0, new Integer((lateralVelocity >= 0) ? 1
				: -1));
		_surfAbsBearings.add(0, new Double(absBearing + Math.PI));

		double bulletPower = energyDrop;

		if (bulletPower < 3.01 && bulletPower > 0.09
				&& _surfDirections.size() > 2) {
			EnemyWave ew = new EnemyWave();
			ew.setFireTime(getTime() - 1);
			ew.setBulletVelocity(20D - (3D * bulletPower));
			ew.setDistanceTraveled(20D - (3D * bulletPower));
			ew.setDirection(((Integer) _surfDirections.get(2))
					.intValue());
			ew.setDirectAngle(((Double) _surfAbsBearings.get(2))
					.doubleValue());
			ew.setFireLocation((Point) _myLocation.clone()); // last
																// tick

			target.addBulletWave(ew);
			// TODO move
			// doSurfing();

		}

		_myLocation = new Point((int) (getX() + absBearing
				* targetBot.getDistance()), (int) (getY() + absBearing
				* targetBot.getDistance()));

		updateWaves();
	}

	public void updateWaves() {
		System.out.println("updateWaves");
		for (Bot enemy : enemies) {
			for (int x = 0; x < enemy.getBulletWave().size(); x++) {
				EnemyWave ew = (EnemyWave) enemy.getBulletWave().get(x);

				ew.setDistanceTraveled((getTime() - ew.getFireTime())
						* ew.getBulletVelocity());
				if (ew.getDistanceTraveled() > new Point((int) getX(),
						(int) getY()).distance(ew.getFireLocation()) + 50) {
					enemy.getBulletWave().remove(x);
					x--;
				}
			}
		}
	}

	public void doSurfing() {
		EnemyWave surfWave = getClosestSurfableWave();
		System.out.println("doSurfing");
		//TODO surfwave = null
		if (surfWave == null) {
			return;
		}

		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);

		double goAngle = FuncLib.absoluteBearing(surfWave.getFireLocation(),
				_myLocation);
		if (dangerLeft < dangerRight) {
			goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI / 2), -1);
		} else {
			goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI / 2), 1);
		}

		setBackAsFront(this, goAngle);
	}	

	public static void setBackAsFront(AdvancedRobot robot, double goAngle) {

		double angle = Utils.normalRelativeAngle(goAngle
				- robot.getHeadingRadians());
		if (Math.abs(angle) > (Math.PI / 2)) {
			if (angle < 0) {
				robot.setTurnRightRadians(Math.PI + angle);
			} else {
				robot.setTurnLeftRadians(Math.PI - angle);
			}
			robot.setBack(100);
		} else {
			if (angle < 0) {
				robot.setTurnLeftRadians(-1 * angle);
			} else {
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}

	// CREDIT: Iterative WallSmoothing by Kawigi
	// - return absolute angle to move at after account for WallSmoothing
	// robowiki.net?WallSmoothing
	public double wallSmoothing(Point botLocation, double angle, int orientation) {
		while (!_fieldRect.contains(project(botLocation, angle, 160))) {
			angle += orientation * 0.05;
		}
		return angle;
	}

	// CREDIT: from CassiusClay, by PEZ
	// - returns point length away from sourceLocation, at angle
	// robowiki.net?CassiusClay
	public static Point project(Point sourceLocation, double angle,
			double length) {
		return new Point((int) (sourceLocation.x + Math.sin(angle) * length),
				(int) (sourceLocation.y + Math.cos(angle) * length));
	}

	public EnemyWave getClosestSurfableWave() {
		double closestDistance = 50000; // I juse use some very big number here
		double closestDist2 = 50000;

		EnemyWave surfWave = null;
		EnemyWave surfWave2 = null;
		for (Bot enemy : enemies) {
			for (int x = 0; x < enemy.getBulletWave().size(); x++) {
				EnemyWave ew = (EnemyWave) enemy.getBulletWave().get(x);
				double distance = _myLocation.distance(ew.getFireLocation())
						- ew.getDistanceTraveled();

				if (distance > ew.getBulletVelocity()
						&& distance < closestDistance) {
					surfWave = ew;
					closestDistance = distance;
				}
			}
			if (closestDistance < closestDist2) {
				closestDist2 = closestDistance;
				surfWave2 = surfWave;
			}
		}
		return surfWave2;
	}

	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = getFactorIndex(surfWave,
				predictPosition(surfWave, direction));

		return _surfStats[index];
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, calculate the index into our stat array for that factor.
	public static int getFactorIndex(EnemyWave ew, Point targetLocation) {
		double offsetAngle = (FuncLib.absoluteBearing(ew.getFireLocation(),
				targetLocation) - ew.getDirectAngle());
		double factor = Utils.normalRelativeAngle(offsetAngle)
				/ maxEscapeAngle(ew.getBulletVelocity()) * ew.getDirection();

		return (int) FuncLib.limit(0, (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
				BINS - 1);
	}

	public static double maxEscapeAngle(double velocity) {
		return Math.asin(8.0 / velocity);
	}

	

	public Point predictPosition(EnemyWave surfWave, int direction) {
		Point predictedPosition = (Point) _myLocation.clone();
		double predictedVelocity = getVelocity();
		double predictedHeading = getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {
			moveAngle = wallSmoothing(
					predictedPosition,
					FuncLib.absoluteBearing(surfWave.getFireLocation(),
							predictedPosition) + (direction * (Math.PI / 2)),
					direction)
					- predictedHeading;
			moveDir = 1;

			if (Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this
			// in one tick
			maxTurning = Math.PI / 720d
					* (40d - 3d * Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading
					+ FuncLib.limit(-maxTurning, moveAngle, maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to breack down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir
					: moveDir);
			predictedVelocity = FuncLib.limit(-8, predictedVelocity, 8);

			// calculate the new predicted position
			predictedPosition = project(predictedPosition, predictedHeading,
					predictedVelocity);

			counter++;

			if (predictedPosition.distance(surfWave.getFireLocation()) < surfWave
					.getDistanceTraveled()
					+ (counter * surfWave.getBulletVelocity())
					+ surfWave.getBulletVelocity()) {
				intercepted = true;
			}
		} while (!intercepted && counter < 500);

		return predictedPosition;
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

		setBulletVelocity(20 - 3 * deltaEnergy);		
		setState(State.Evading);		
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
				setState(State.Attacking);
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
		return fireDirection;
	}

	public void setDirection(int direction) {
		this.fireDirection = direction;
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
	
	public ArrayList<Bot> getEnemies() {
		return enemies;
	}
	
	public ArrayList<Bot> getTeam() {
		return team;
	}

	public double getBulletVelocity() {
		return bulletVelocity;
	}

	public void setBulletVelocity(double bulletVelocity) {
		this.bulletVelocity = bulletVelocity;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
}