package robots;

import robocode.*;
import helper.*;
import helper.Enums.*;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import sun.font.EAttribute;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class TestBot extends TeamRobot {

	public static boolean periodicScan = false;

	// Variables
	private boolean gameOver = false;
	private int moveDirection = 1;// >0 : turn right, <0 : tun left
	private int turnDirection = 1;
	private int count = 0; // Count for movement patterns
	private double EnergyThreshold = 15;
	private boolean scanStarted = false;

	// States
	private State state = State.Evading;
	private MovementPattern movePattern = MovementPattern.Stop;
	private RadarState radarState;
	private FireMode fireMode;

	
	// Time Handles in rounds	
	private double scanElapsedTime;	
	private double scanTimer = 10; // time elapses between scans	
	private int sweepScanCount = 0;
	//private Bot[] enemies;
	private ArrayList<Bot> enemies = new ArrayList<>();
	private  ArrayList<Bot> team = new ArrayList<>();
	private Bot attacker = new Bot(); // Robot which last attacked us
	private Bot target = new Bot();
	private AvoidWall avoidWall;

	// Statistics
	List<Data> dataList = new ArrayList<>();
	private List<WaveBullet> waves = new ArrayList<WaveBullet>();

	int direction = 1;

	private boolean bulletHit;

	private boolean hitRobot;
	private boolean isEnemyLocked = false;

	public void run() {

		// Color
		setBodyColor(Color.gray);
		setGunColor(Color.blue);
		setRadarColor(Color.blue);
		setBulletColor(Color.green);

		setAdjustGunForRobotTurn(true);
		// setAdjustRadarForRobotTurn(true);
		// setAdjustRadarForGunTurn(true);

		state = State.Scanning;

		while (true) {
			scan();
			if (target.getInfo() != null)
				antiGravMove();
		}
	}
/**
 * gets the absolute bearing between to x,y coordinates
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

	int midpointcount = 0; // Number of turns since that strength was changed.
	double midpointstrength = 0; // The strength of the gravity point in the
									// middle of the field
/**
 * the anti Gravity move
 */
	void antiGravMove() {
		double xforce = 0;
		double yforce = 0;
		double force;
		double ang;
		GravPoint p;
		// cycle through all the enemies. If they are alive, they are repulsive.
		// Calculate the force on us
		for (Bot enemyBot : enemies) {
			if (!enemyBot.isDead()) {

				double angleToEnemy = enemyBot.getInfo().getBearing();

				// Calculate the angle to the scanned robot
				double angle = Math
						.toRadians((getHeading() + angleToEnemy % 360));

				// Calculate the coordinates of the robot
				double enemyX = (getX() + Math.sin(angle)
						* enemyBot.getInfo().getDistance());
				double enemyY = (getY() + Math.cos(angle)
						* enemyBot.getInfo().getDistance());

				p = new GravPoint(enemyX, enemyY, -1000);

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
	double normaliseBearing(double ang) {
		if (ang > Math.PI)
			ang -= 2 * Math.PI;
		if (ang < -Math.PI)
			ang += 2 * Math.PI;
		return ang;
	}

	/** Move in the direction of an x and y coordinate **/
	void goTo(double x, double y) {
		double dist = 20;
		double angle = Math.toDegrees(absBearing(getX(), getY(), x, y));
		double r = turnTo(angle);
		setAhead(dist * r);
	}

	/**
	 * Turns the shortest angle possible to come to a heading, then returns the
	 * direction the bot needs to move in.
	 **/
	int turnTo(double angle) {
		double ang;
		int dir;
		ang = normaliseBearing(getHeading() - angle);
		if (ang > 90) {
			ang -= 180;
			dir = -1;
		} else if (ang < -90) {
			ang += 180;
			dir = -1;
		} else {
			dir = 1;
		}
		setTurnLeft(ang);
		return dir;
	}

	/** Returns the distance between two points **/
	double getRange(double x1, double y1, double x2, double y2) {
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
		// Mark robot as dead
		for (Bot bot : enemies) {
			if(bot.getName().equals(event.getName())) {
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
	}

	public void onBulletHit(BulletHitEvent event) {
		findDataByName(target.getName()).BulletHit(true, fireMode);
		bulletHit = true;
	}	

	public void onDeath(DeathEvent event) {
		for (Data data : dataList) {
			data.lost();
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
		for (Data data : dataList) {
			data.printData(true);
		}

		gameOver = true;
		saveData();

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

				state = State.Scanning;
			}
			scanElapsedTime = 0;
		}

		// Avoid walls
		avoidWall = detectCloseWall(Math.toRadians(this.getHeading()));
		avoidWall();

		// Execute behavior for corresponding state
		if (state == State.Attacking) {

			// Find Target
			findTarget();			
			
			// Radar Scanning
				// FullScan finished, start sweep scan
			if(!scanStarted && radarState == RadarState.FullScan) { 
				System.out.println("Full scan finished.");
				// Sweep search for our target at last known position
				runScan(RadarState.Sweep);				
			}		
			
			
			if(isEnemyLocked) {
				System.out.println("Locked on " + target.getName());
				chooseFireMode();
				fireGun();
			} else {
				System.out.println("Enemy no longer locked."); 
				// Use sweep to find target again
				// do a full scan if target cannot be found after five rounds
				if(sweepScanCount < 10 && (radarState == RadarState.Lock || radarState == RadarState.Sweep)) {
					runScan(RadarState.Sweep);
				} else {					
					runScan(RadarState.FullScan);
					sweepScanCount = 0;
				}
			}
			
			// TODO:
			runMovementPattern(MovementPattern.Stop); // Needs to be adjusted, should try to get closer to enemy etc
		}
		
		if(state == State.Scanning) {
			
			runMovementPattern(MovementPattern.Stop);
			
			runScan(RadarState.FullScan);			
		}

		if (state == State.Evading) {
			// TODO:
			// Implement anti gravity stuff here
			// Or short evasive maneuver
			runMovementPattern(MovementPattern.Stop);

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

		// Pattern Up and Down
		if (pattern == MovementPattern.UpAndDown) {

			setAhead(moveDirection * 10);

		}
		
		if(pattern == MovementPattern.Random) {
			count++;
			if(count >= 40) {
				randomMovement();
				count = 0;
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
				avoidBullet(enemyDeltaEnergy);
			}
		}
		
		
		if(isTeammate(robot.getName())) {
			// Team mate
			// Update a team mate
			for (Bot bot : team) {
				if(bot.getName().equals(robot.getName())) {
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
					// A data object for this kind of robot already exists, abort
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
					System.out.println("File was not found, create new data file");
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
		ScannedRobotEvent enemy = target.getInfo();		
		// Linear Targeting		
		if(fireMode == FireMode.LinearTargeting) {			
			
			double power = Math.min(3, Math.max(.1, 400 / target.getInfo().getDistance()));
		    final double ROBOT_WIDTH = 16,ROBOT_HEIGHT = 16;
		    // Variables prefixed with e- refer to enemy, b- refer to bullet and r- refer to robot
		    final double eAbsBearing = getHeadingRadians() + target.getInfo().getBearingRadians();
		    final double rX = getX(), rY = getY(),
		        bV = Rules.getBulletSpeed(power);
		    final double eX = rX + target.getInfo().getDistance()*Math.sin(eAbsBearing),
		        eY = rY + target.getInfo().getDistance()*Math.cos(eAbsBearing),
		        eV = target.getInfo().getVelocity(),
		        eHd = target.getInfo().getHeadingRadians();
		    // These constants make calculating the quadratic coefficients below easier
		    final double A = (eX - rX)/bV;
		    final double B = eV/bV*Math.sin(eHd);
		    final double C = (eY - rY)/bV;
		    final double D = eV/bV*Math.cos(eHd);
		    // Quadratic coefficients: a*(1/t)^2 + b*(1/t) + c = 0
		    final double a = A*A + C*C;
		    final double b = 2*(A*B + C*D);
		    final double c = (B*B + D*D - 1);
		    final double discrim = b*b - 4*a*c;
		    if (discrim >= 0) {
		        // Reciprocal of quadratic formula
		        final double t1 = 2*a/(-b - Math.sqrt(discrim));
		        final double t2 = 2*a/(-b + Math.sqrt(discrim));
		        final double t = Math.min(t1, t2) >= 0 ? Math.min(t1, t2) : Math.max(t1, t2);
		        // Assume enemy stops at walls
		        final double endX = limit(
		            eX + eV*t*Math.sin(eHd),
		            ROBOT_WIDTH/2, getBattleFieldWidth() - ROBOT_WIDTH/2);
		        final double endY = limit(
		            eY + eV*t*Math.cos(eHd),
		            ROBOT_HEIGHT/2, getBattleFieldHeight() - ROBOT_HEIGHT/2);
		        setTurnGunRightRadians(robocode.util.Utils.normalRelativeAngle(
		            Math.atan2(endX - rX, endY - rY)
		            - getGunHeadingRadians()));
		        if(getGunTurnRemaining() < 5) {
		        	setFire(power);
		        	System.out.println("FIRE, LinTarget");		        	
		        }
		    }			
		}
		
		if(fireMode == FireMode.GuessFactor) {
			// Guess Targeting
			double absBearing = enemy.getBearingRadians() + getHeadingRadians();
			double power = Math.min(3,
					Math.max(.1, 400 / target.getInfo().getDistance()));

			if (target.getInfo().getVelocity() != 0) {
				if (Math.sin(target.getInfo().getHeadingRadians() - absBearing)
						* target.getInfo().getVelocity() < 0)
					direction = -1;
				else
					direction = 1;
			}
			Data data = findDataByName(target.getName());
			int[] currentStats = data.getStats()[(int) (target.getInfo()
					.getDistance() / 100)];

			WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing,
					power, direction, getTime(), currentStats);

			int bestindex = 15; // initialize it to be in the middle,
								// guessfactor 0.
			for (int i = 0; i < 31; i++) {
				if (currentStats[bestindex] < currentStats[i]) {
					bestindex = i;
				}
			}

			// this should do the opposite of the math in the WaveBullet:
			double guessfactor = (double) (bestindex - (currentStats.length - 1) / 2)
					/ ((currentStats.length - 1) / 2);
			double angleOffset = direction * guessfactor
					* newWave.maxEscapeAngle();
			double gunAdjust = Utils.normalRelativeAngle(absBearing
					- getGunHeadingRadians() + angleOffset);

			setTurnGunRightRadians(gunAdjust);


			if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, target.getInfo().getDistance()) && setFireBullet(power) != null) {				
				waves.add(newWave);
				System.out.println("Fire,Guess Shooting");
			}			
			 // End of guess shoting 			
		}

	}

	private double limit(double value, double min, double max) {
		return Math.min(max, Math.max(min, value));
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
		double avoidDistance = 60 + 10 * Rules.MAX_VELOCITY;
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
	private void avoidBullet(double deltaEnergy) {

		if (deltaEnergy > 3 || bulletHit || hitRobot) {
			bulletHit = false;
			hitRobot = false;
			return;
		}

		// Check if enemy hit a wall
		// TODO if he hits the wall a 2. time in a short time frame it wont be
		// detected
		double wallDmg = target.getInfo().getVelocity() * 0.5 - 1;
		if (deltaEnergy == wallDmg) {
			System.out.println("bot hit wall");
			System.out.println("------");
			return;
		}

		// check if enemy hits another bot
		// TODO verify with coordinates
		// TODO never triggers (maybe collision dmg isn't 0.6?)
		if (deltaEnergy == 0.6) {
			System.out.println("bot hit bot");
			System.out.println("------");
			return;
		}

		// TODO doesn't detect bullet shots while crashing
		double bulletVelocity = 20 - 3 * deltaEnergy;
		System.out.println("AVOID: " + deltaEnergy + " BulletVelocity: "
				+ bulletVelocity);

		// if not avoiding the wall, make a random movement
		// if(avoidWall == AvoidWall.None){
		randomMovement();
		// }
	}

	/**
	 * turn the robot in a random direction but not straight to or away from the
	 * enemy ("straight" is defined in an angle which gets bigger as closer we
	 * are to the enemy) or to a wall
	 * 
	 */
	private void randomMovement() {
		ScannedRobotEvent bot = target.getInfo();
		double botDistance = bot.getDistance();

		//TODO consider close bots
		// change deltaAngle according to the distance to the enemy
		double deltaAngle = 30;
		deltaAngle = deltaAngle + (botDistance / 50) * 5;
		if (deltaAngle > 80)
			deltaAngle = 80;

		double angleDeg = (this.getHeading() + bot.getBearing()) % 360;
		if (angleDeg < 0)
			angleDeg += 360;
		Random rand = new Random();
		double randAngle;
		double deltaMin;
		double deltaMax;
		
		deltaMin = angleDeg - deltaAngle;
		if (deltaMin < 0)
			deltaMin += 180;
		deltaMax = angleDeg + deltaAngle;
		// gives a random angle which is not to the enemy or the opposite
		// direction
		do {
			randAngle = rand.nextDouble() * 360;
		} while (randAngle < deltaMin && randAngle > deltaMax
				|| randAngle < (deltaMin + 180) % 360
				&& randAngle > (deltaMax + 180) % 360
				|| detectCloseWall(randAngle) != AvoidWall.None);

		// turns to rand direction to the bearing to robot has to change
		double randBearing = randAngle - this.getHeading();

		// change the movement direction if the bearing is >90°/<-90° and turn
		// accordingly
		if (moveDirection > 0) {
			if (randBearing < 90 && randBearing > -90) {
				turnRight(randBearing);
			} else {
				moveDirection *= -1;
				if (randBearing < 0)
					randBearing += 180;
				else
					randBearing -= 180;
				turnRight(randBearing);
				System.out.print("change direction and ");
			}
			System.out.println("turn " + randBearing + " md: " + moveDirection);
		} else {
			if (randBearing < 90 && randBearing > -90) {
				moveDirection *= -1;
				turnRight(randBearing);
				System.out.print("change direction and ");
			} else {
				if (randBearing < 0)
					randBearing += 180;
				else
					randBearing -= 180;
				turnRight(randBearing);
			}
			System.out.println("turn " + randBearing + " md: " + moveDirection);
		}
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
		    double angleToEnemy = getHeadingRadians() + target.getInfo().getBearingRadians();
		 
		    // Subtract current radar heading to get the turn required to face the enemy, be sure it is normalized
		    double radarTurn = Utils.normalRelativeAngle( angleToEnemy - getRadarHeadingRadians() );
		 
		    // Distance we want to scan from middle of enemy to either side
		    // The 36.0 is how many units from the center of the enemy robot it scans.
		    double extraTurn = Math.min( Math.atan( 36.0 / target.getInfo().getDistance() ), Rules.RADAR_TURN_RATE_RADIANS );
		 
		    // Adjust the radar turn so it goes that much further in the direction it is going to turn
		    // Basically if we were going to turn it left, turn it even more left, if right, turn more right.
		    // This allows us to overshoot our enemy so that we get a good sweep that will not slip.
		    radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);
		 
		    //Turn the radar
		    setTurnRadarRightRadians(radarTurn);
			
		}
		
		if(scan == RadarState.Lock) {				
			
			radarState = RadarState.Lock;

			if (target.getName().equals("None")) {
				System.out.println("No Target assigned.");
				return;
			}

			double angleToEnemy = getHeading() + target.getInfo().getBearing();

			double radarTurn = Utils.normalRelativeAngleDegrees(angleToEnemy
					- getRadarHeading());

			setTurnRadarRight(radarTurn);
		}

	}

	private void collectData(ScannedRobotEvent e) {

		// Collect data
		double absBearing = getHeadingRadians() + e.getBearingRadians();

		// find our enemy's location:
		double ex = getX() + Math.sin(absBearing) * e.getDistance();
		double ey = getY() + Math.cos(absBearing) * e.getDistance();

		// Let's process the waves now:
		for (int i = 0; i < waves.size(); i++) {
			WaveBullet currentWave = (WaveBullet) waves.get(i);
			if (currentWave.checkHit(ex, ey, getTime())) {
				waves.remove(currentWave);
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

	private void chooseFireMode() {
		
		// TODO: 
		String robotName = target.getName();
		
		// Clean up name
		if(target.getName().contains(" ")) {
			int i = target.getName().indexOf(" ");
			robotName = target.getName().substring(0, i);			
		}
		
		Data data = findDataByName(robotName);
		
		System.out.println("GuessAcc: " + data.getGuessAccuracy() + " LinAcc: " + data.getLinAccuracy());
		
		if(data.getGuessAccuracy() > data.getLinAccuracy()) {
			fireMode = FireMode.GuessFactor;
			return;
		}
		fireMode = FireMode.LinearTargeting;

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

	private Data findDataByName(String name) {

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

}