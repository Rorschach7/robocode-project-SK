package robots;

import robocode.*;
import helper.*;
import helper.Enums.*;
import helper.strategies.gun.*;
import helper.strategies.movement.*;
import helper.strategies.radar.*;
import helper.strategies.target.ChooseAggroStrategy;
import helper.strategies.target.TargetStrategy;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javafx.geometry.Point2D;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class BaseBot extends TeamRobot {

	// Variables
	protected int nr; // number to identify in team
	protected boolean periodicScan = false;
	protected ArrayList<Bot> enemies = new ArrayList<>();
	protected ArrayList<Bot> team = new ArrayList<>();
	protected Bot attacker; // Robot which last attacked us
	protected Bot meleeAttacker;
	protected Bot target;

	protected boolean bulletHit;
	protected int moveDirection = 1;// >0 : turn right, <0 : tun left
	protected boolean gameOver = false;
	protected boolean robotCollision;
	protected double bulletVelocity;
	protected int fireDirection;
	protected double bulletPower;
	protected boolean printStatus;

	// State
	protected State state = State.Scanning;

	// Time Handles in rounds
	protected double scanElapsedTime;
	protected double scanTimer = 10; // time elapses between scans
	protected int sweepScanCount = 0;

	// Statistics
	protected ArrayList<Data> dataList = new ArrayList<>();
	protected boolean bestScore = true;
	protected double hits;
	protected double hitsTaken;
	protected double enemyBulletsDetected;
	protected double misses;

	// Strategies
	// Gun
	protected GunStrategy gunStrategy = new LinTargeting();
	// Movement
	protected MovementStrategy attackingMovement = new StopMovement(); // Used
																		// most
																		// of
																		// the
																		// time
	protected MovementStrategy scanningMovement = new StopMovement(); // Used
																		// when
																		// we're
																		// performing
																		// 360
																		// scan
	protected MovementStrategy dodgeBullet = new RandomMovement(); // Used to
																	// dodge
																	// incoming
																	// bullet
	protected MovementStrategy victoryDance = new SpinAroundMovement(); // Use
																		// for
																		// victory
																		// dance
	// Radar
	protected RadarStrategy radarStrategy = new FullSweepLockStrategy();
	// Choose target
	protected TargetStrategy targetStrategy = new ChooseAggroStrategy();

	public void run() {

		// Color
		setBodyColor(Color.gray);
		setGunColor(Color.blue);
		setRadarColor(Color.orange);
		setBulletColor(Color.green);

		initialize();

		setState(State.Scanning);

		while (true) {
			scan();
		}
	}

	/**
	 * This method is called in the run method of robocode. Use/Override this
	 * method to initialize robot with certain strategies or set its colors.
	 * Always use super call first.
	 */
	public void initialize() {
		// Assign number
		if (getTeammates() != null) {
			int start = getName().indexOf("(");
			int end = getName().indexOf(")");
			nr = Integer.parseInt(getName().substring(start + 1, end));
		}
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		printStatus = true;
	}

	/**
	 * Moves the robot 20 units towards the position.
	 * 
	 * @param x
	 * @param y
	 */
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
	public void turnTo(double angle) {
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
		// Collect data dependent on strategy
		dodgeBullet.collectData(this, e);
		attackingMovement.collectData(this, e);
		getGunStrategy().collectData(this, e);

		radarStrategy.execute(this, e);
	}

	public void onHitByBullet(HitByBulletEvent event) {
		attacker = FuncLib.findBotByName(event.getName(), enemies);
		if (attacker == null) {
			attacker = new Bot();
			attacker.init(event);
			return;
		}
		attacker.init(event);
		attacker.addAggro(event.getPower() * 4 + 2 * (event.getPower() - 1));
		targetStrategy.onHitByBullet(this, event);
		hitsTaken++;
	}

	public void onHitRobot(HitRobotEvent event) {
		// System.out.println("Robot collision!");

		if (FuncLib.findBotByName(event.getName(), team) != null) {
			System.out
					.println("We collided with a team mate. That should not have happened.");
			return;
		}

		robotCollision = true;
		setMeleeAttacker(new Bot());
		getMeleeAttacker().init(event);

		targetStrategy.onCollision(this, event);
	}

	public void onRobotDeath(RobotDeathEvent event) {
		for (Bot bot : team) {
			if (event.getName().equals(bot.getName())) {
				bot.died();
				team.remove(bot);
				System.out.println("Team mate died :(");
				return;
			}
		}

		// Mark robot as dead
		for (Bot bot : enemies) {
			if (bot.getName().equals(event.getName())) {
				bot.died();
				break;
			}
		}

		// Our target just died, we need a new one
		if (getTarget().getName().equals(event.getName())) {
			System.out.println("Target is dead, acquire new target.");
			setState(State.Scanning);
		}
	}

	public void onHitWall(HitWallEvent event) {
		// Should never happen
		System.out.println("We crashed into a wall. How could that happen? :(");
	}

	public void onBulletMissed(BulletMissedEvent event) {
		FuncLib.findDataByName(getTarget().getName(), dataList).bulletHit(
				false, getGunStrategy());
		misses++;
	}

	public void onWin(WinEvent event) {
		System.out.println("WINEVENT");
	}

	public void onBulletHit(BulletHitEvent event) {
		FuncLib.findDataByName(getTarget().getName(), dataList).bulletHit(true,
				getGunStrategy());
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
				data.printData(true);
			}
			System.out.println("VICTORY");
			victoryDance.execute(this);
		}

		System.out.println(getGunStrategy() + " acc: "
				+ getGunStrategy().getAccuracy(this));

		gameOver = true;

		if (getTeammates() == null) {
			saveData();
			return;
		}

		// Broadcast our score to the team
		double score = (dataList.get(0).getGuessTargetingHits() + dataList.get(
				0).getGuessTargetingMissed())
				/ dataList.get(0).getGuessTargetingHits();

		if (dataList.get(0).getGuessTargetingHits() == 0) {
			score = 0;
		}
		try {
			broadcastMessage("SCORE " + score);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//TODO broadcast movement score

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

			if (dataList.get(0).getGuessTargetingHits() == 0) {
				score = 0;
			}

			double msgScore = Double.parseDouble(info);
			if (msgScore >= score) {
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

		if (start.equals("TEAMPOS")) {
			int n = info.indexOf(":");
			double x = Double.parseDouble(info.substring(0, n));
			double y = Double.parseDouble(info.substring(n + 1));

			for (Bot bot : team) {
				if (bot.getName().equals(event.getSender())) {
					bot.updatePos(x, y);
					return;
				}
			}
		}

	}

	public void onStatus(StatusEvent event) {
		if (gameOver) {
			return;
		}

		// Broadcast position to team
		if (getTeammates() != null) {
			String message = "TEAMPOS " + getX() + ":" + getY();
			try {
				broadcastMessage(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Periodic scan
		if (periodicScan) {
			radarStrategy.periodicScan(this);
		}

		// Find Target
		// targetStrategy.execute(this);

		// Execute behavior for corresponding state
		if (getState() == State.Attacking) {

			// Radar attacking strategy
			radarStrategy.attackingScan(this);

			// Attacking movement strategy
			attackingMovement.execute(this);
		}

		// In this state we want to perform a full scan of the battlefield
		if (getState() == State.Scanning) {
			// Scanning movement strategy
			scanningMovement.execute(this);

			// Scanning scan strategy
			radarStrategy.scanningScan(this);
		}

		if (getState() == State.Evading) {

			// Doge bullet movement strategy
			dodgeBullet.execute(this);

			// Doge bullet gun strategy
			getGunStrategy().execute(this);
		}

		// Reset hitRobot
		if (robotCollision) {
			robotCollision = false;
		}

		// Print status
		if (printStatus) {
			printStatus();
		}
	}

	/**
	 * Prints current status information
	 */
	public void printStatus() {
		System.out
				.println("---------------------------------------------------------");
		if (target != null)
			System.out.println("Target: " + target.getName() + "Aggro: "
					+ target.getAggro());
		if (attacker != null)
			System.out.println("Attacker: " + attacker.getName());
		System.out.println("State: " + getState());
		System.out.println(radarStrategy);
		System.out.println(gunStrategy);
		System.out.println("Dodge Strategy: " + dodgeBullet);
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
		//TODO check if bot already exist in data
		if (getTarget() != null
				&& getTarget().getName().equals(robot.getName())) {
			double enemyDeltaEnergy = getTarget().getInfo().getEnergy()
					- robot.getEnergy();
			setBulletPower(enemyDeltaEnergy);
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
			// Update an enemy
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

			// Create data object, that we can add to our list
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
	 * Tries to figure out if enemy tank shoots at us and starts evasive
	 * maneuver
	 * 
	 * @param deltaEnergy
	 *            the energy the targeted tank lost between last and current
	 *            turn
	 */
	private void detectBullet(double deltaEnergy) {
		if (deltaEnergy > 3 || bulletHit || robotCollision) {
			bulletHit = false;
			return;
		}

		// Check if enemy hit a wall
		double wallDmg = getTarget().getInfo().getVelocity() * 0.5 - 1;
		if (deltaEnergy == wallDmg) {
			return;
		}

		FuncLib.findDataByName(getTarget().getName(), dataList).detectedBullet(getDodgeBullet(), hitsTaken);
		enemyBulletsDetected++;
		setBulletVelocity(20 - 3 * deltaEnergy);
		setState(State.Evading);
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
			// System.out.println("IN JSON");
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
				+ getTarget().getInfo().getBearingRadians();
		// find our enemy's location:
		double ex = getX() + Math.sin(absBearing)
				* getTarget().getInfo().getDistance();
		double ey = getY() + Math.cos(absBearing)
				* getTarget().getInfo().getDistance();

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
				gunStrategy.addToFriendlyFire(this);
				System.out.println(tx + " " + ty);
				return true;
			}
		}
		return false;
	}

	public Bot getTarget() {
		return target;
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
		// Execute find target algorithm
		if (state == State.Attacking) {
			System.out.println("EXECUTE TARGET STARTEGY");
			targetStrategy.execute(this);
		}
		this.state = state;
	}

	public double getBulletPower() {
		return bulletPower;
	}

	public void setBulletPower(double bulletPower) {
		this.bulletPower = bulletPower;
	}

	public ArrayList<Data> getDataList() {
		return dataList;
	}

	public int getMoveDirection() {
		return moveDirection;
	}

	public void setMoveDirection(int moveDirection) {
		this.moveDirection = moveDirection;
	}

	public GunStrategy getGunStrategy() {
		return gunStrategy;
	}

	public void setGunStrategy(GunStrategy aimStrategy) {
		this.gunStrategy = aimStrategy;
	}

	public void setTarget(Bot target) {
		this.target = target;
	}

	public void setPeriodicScan(boolean periodicScan) {
		this.periodicScan = periodicScan;
	}

	public boolean getPeriodicScan() {
		return periodicScan;
	}

	public Bot getMeleeAttacker() {
		return meleeAttacker;
	}

	public void setMeleeAttacker(Bot meleeAttacker) {
		this.meleeAttacker = meleeAttacker;
	}

	public TargetStrategy getTargetStrategy() {
		return targetStrategy;
	}

	public void setTargetStrategy(TargetStrategy targetStrategy) {
		this.targetStrategy = targetStrategy;
	}

	public double getHitsTaken() {
		return hitsTaken;
	}

	public void setHitsTaken(double hitsTaken) {
		this.hitsTaken = hitsTaken;
	}

	public double getEnemyBulletsDetected() {
		return enemyBulletsDetected;
	}

	public void setEnemyBulletsDetected(double enemyBulletsDetected) {
		this.enemyBulletsDetected = enemyBulletsDetected;
	}

	public MovementStrategy getDodgeBullet() {
		return dodgeBullet;
	}

	public void setDodgeBullet(MovementStrategy dodgeBullet) {
		this.dodgeBullet = dodgeBullet;
	}
}