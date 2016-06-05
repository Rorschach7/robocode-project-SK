package robots;

import robocode.*;
import static robocode.util.Utils.*;
import helper.*;
import helper.Enums.*;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;



public class TestBot extends TeamRobot {	
	
	public static boolean PeriodicScan = false;
	
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

	private EnemyBot[] enemies;
	private EnemyBot attacker = new EnemyBot(); // Robot which last attacked us
	private EnemyBot target = new EnemyBot();
	private AvoidWall avoidWall;

	// Statistics
	List<Data> dataList = new ArrayList<>();
	// TODO:
	int[][] stats = new int[13][31]; // onScannedRobot can scan up to 1200px, so there are only 13. // 31 is the number of unique GuessFactors we're using
  									  // Note: this must be odd number so we can get
									  // GuessFactor 0 at middle.
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
		//setAdjustRadarForRobotTurn(true);
		//setAdjustRadarForGunTurn(true);
						
		state = State.Scanning;	
		
		while(true) {
			scan();	
		}
	}	
		
	public void onScannedRobot(ScannedRobotEvent e) {
		update(e);
		
		//System.out.println("Scanned Robot: " + e.getName());

		
		// Sweep scan found target, lock on
		if(radarState == RadarState.Sweep && target.getName().equals(e.getName())) {
			//System.out.println("Sweep found target, lock on");
			radarState = RadarState.Lock;
			isEnemyLocked = true;					
		}		
		
		if (radarState == RadarState.Lock) {
			if (target.getName().equals(e.getName())) {				
				isEnemyLocked = true;
				
				runScan(RadarState.Lock);
								
			}
		}
		
		// Collect data fpr guess targeting
		collectData(e);         
		
//		System.out.println("---Begin---");		
//		for(int i = 0;i < enemies.length; i++) {
//			if(enemies[i] != null) {
//				System.out.println(enemies[i].getName());
//			}
//		}		
//		System.out.println("---End---");		
	

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
				enemies[i].died();
				return;
			}
		}	
		
		// Our target just died, we need a new one
		if(target.getName().equals(event.getName())) {
			findTarget();
		}
	}

	public void onHitWall(HitWallEvent event) {

		if (movePattern == MovementPattern.UpAndDown) {
			moveDirection *= -1;
			setAhead(moveDirection * 5);
		}
	}

	public void onBulletMissed(BulletMissedEvent event) {
		FindDataByName(target.getName()).BulletHit(false, fireMode);
	}

	public void onBulletHit(BulletHitEvent event) {
		FindDataByName(target.getName()).BulletHit(true, fireMode);
		bulletHit = true;
	}

	public void onRoundEnded(RoundEndedEvent event) {
		// TODO: Victory Dance
		for (Data data : dataList) {
			data.printData(false);
		}
		gameOver = true;
		saveData();
   }	

	public void onStatus(StatusEvent event) {
		if(gameOver) {
			return;
		}	

		// Increment Time Handler
		if (!scanStarted) {
			scanElapsedTime++;
		}		

		// Periodic scan
		if (scanElapsedTime >= scanTimer) {
			if (enemies != null && enemies.length > 1 && PeriodicScan) {

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
			
			
			// Radar Scanning
				// FullScan finished, start sweep scan
			if(!scanStarted && radarState == RadarState.FullScan) { 
				//System.out.println("Full scan finished.");
				// Sweep search for our target at last known position
				runScan(RadarState.Sweep);				
			}		
			
			
			if(isEnemyLocked) {
				// TODO:
				chooseFireMode();
				fireGun();
			} else {
				//System.out.println("Enemy no longer locked.");
				runScan(RadarState.Sweep);
			}
			
			
			// Run Attacking movement pattern/strategy
			// TODO:
			//runMovementPattern(MovementPattern.UpAndDown); // Needs to be adjusted, should try to get closer to enemy etc
		}
		
		if(state == State.Scanning) {
			
			//runMovementPattern(MovementPattern.UpAndDown);
			
			runScan(RadarState.FullScan);			
		}

		if (state == State.Evading) {
			// TODO:
			// Implement anti gravity stuff here
			// Or short evasive maneuver
			runMovementPattern(MovementPattern.Stop);

		}
		
		//printStatus();
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
		System.out.println("---------------------------------------------------------");
		System.out.println("Current MovementPattern: " + movePattern.name());
		System.out.println("Count: " + count);
		System.out.println("Target: " + target.getName());
		System.out.println("Attacker: " + attacker.getName());
		System.out.println("State: " + state);		
		System.out.println("RadarState: " + radarState);			
		System.out.println("---------------------------------------------------------");
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
		
		// Assign scanned Robot to a Data structure		
		String robotName = robot.getName();
		
		// Clean up name 
		if(robot.getName().contains(" ")) {
			int i = robot.getName().indexOf(" ");
			robotName = robot.getName().substring(0, i);
			//System.out.println("Multiple Instances: " + robotName);
		}	
		
		// Check if we already added this kind of robot
		for (Data data : dataList) {
			if(data.getRobotName().equals(robotName)) {
				// A data object for this kind of robot already exists, abort
				System.out.println("A data object for this kind of robot already exists, abort");
				return;
			}
		}
		
		// Create data object, that we cann add to our list
		Data data;
		
		if(checkForData(robotName)) {
			// A data file already exists, so load it
			System.out.println("Load File.");
			// Add loaded data file to dataList
			// TODO:
			data = loadData(robotName);
			if(data == null) {
				System.out.println("Loading failed horribly :(");
			}
		} else {			
			System.out.println("No File found.");
			data = new Data(robotName);					
			dataList.add(data);
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
		// TODO: consider walls
		if(fireMode == FireMode.LinearTargeting) {			
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
		}
		
		if(fireMode == FireMode.GuessFactor) {
			double absBearing = enemy.getBearing() + getHeading();
			double power = Math.min(3, Math.max(.1, 400 / target.getInfo().getDistance()));

			// don't try to figure out the direction they're moving
			// they're not moving, just use the direction we had before
			if (target.getInfo().getVelocity() != 0) {
				if (Math.sin(target.getInfo().getHeadingRadians() - absBearing) * target.getInfo().getVelocity() < 0)
					direction = -1;
				else
					direction = 1;
			}
			
			int[] currentStats = stats[(int)(target.getInfo().getDistance() / 100)]; // It doesn't look silly now!
			
			WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, power, direction, getTime(), currentStats);

			int bestindex = 15; // initialize it to be in the middle, guessfactor 0.
			for (int i = 0; i < 31; i++)
				if (currentStats[bestindex] < currentStats[i])
					bestindex = i;

			// this should do the opposite of the math in the WaveBullet:
			double guessfactor = (double) (bestindex - (stats.length - 1) / 2) / ((stats.length - 1) / 2);
			double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
			double gunAdjust = Utils.normalRelativeAngle(absBearing - getGunHeadingRadians() + angleOffset);
			//setTurnGunRightRadians(gunAdjust);

			if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, target.getInfo().getDistance())) {
				setFire(power);
				findBotByName(target.getName()).getWaves().add(newWave);
				System.out.println("Guess Shooting");
			}	
			
			 // End of guess shoting 
			
		}
		
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
	 *            the energy the targeted tank lost between last and current turn
	 */
	private void avoidBullet(double deltaEnergy) {

		if (deltaEnergy > 3 || bulletHit || hitRobot) {
			bulletHit = false;
			hitRobot = false;
			return;
		}
		
		// Check if enemy hit a wall
		// TODO:	
		
		System.out.println("AVOID: " + deltaEnergy);

//		moveDirection *= -1;
//		Random rand = new Random();
//		int rnd = rand.nextInt(6) + 3;
//		setMaxVelocity(rnd);
	}
	
	/**
	 * Finds a target among all spotted enemies
	 */
	private void findTarget() {

		if (enemies == null) {
			return;
		}

		for(int i = 0; i < enemies.length; i++) {
			if(!enemies[i].isDead()) {
				target = enemies[i];
				break;
			}
		}
		
		// Find closest enemy
		for(int i = 0; i < enemies.length; i++ ) {
			if(enemies[i].isDead()) {
				continue;
			}
			if(target.getInfo().getDistance() > enemies[i].getInfo().getDistance()) {
				target = enemies[i];
			}
		}
		
	}	
	
	private void runScan(RadarState scan) {				
				
		//System.out.println("RunScan: " + scan);
		
		if(scan == RadarState.FullScan) {
			if(!scanStarted) {
				// make a short scan of the whole battlefield
				radarState = RadarState.FullScan;
				//System.out.println("Executing Scan");
				setTurnRadarRight(360);
				scanStarted = true;
			} else if (getRadarTurnRemaining() < 10){
				// Scan finished				
				scanStarted = false;				
				state = State.Attacking;				
			}
		}
		
		if(scan == RadarState.Sweep) {
			
			radarState = RadarState.Sweep;
			
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
			
			if(target.getName().equals("None")) {
				System.out.println("No Target assigned.");
				return;
			}			
			
			double angleToEnemy = getHeading() + target.getInfo().getBearing();		
			
			double radarTurn = Utils.normalRelativeAngleDegrees(angleToEnemy - getRadarHeading());				
			
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
		 EnemyBot enemyBot = findBotByName(e.getName());
		for (int i = 0; i < enemyBot.getWaves().size(); i++) {
			WaveBullet currentWave = (WaveBullet) enemyBot.getWaves().get(i);
			if (currentWave.checkHit(ex, ey, getTime())) {
				enemyBot.getWaves().remove(currentWave);
				i--;
			}
		}

	}
	
	/**
	 * Finds the specefied robot among all spotted enemies.
	 * @param name The name of the robot that you want.
	 * @return the Robot if already spotted and existing, null Otherwise.
	 */
	private EnemyBot findBotByName(String name) {
		for(int i = 0; i < enemies.length; i++) {
			if(enemies[i].getName().equals(name)) {
				return enemies[i];
			}
		}
		return null;
	}
	
	private void chooseFireMode() {
		
		fireMode = FireMode.LinearTargeting;
		
	}
	
	private Data loadData(String robotName) {
		// TODO:
		return new Data("WOW");
	}
	
	private void saveData() {
		System.out.println("Saving Data " + dataList.size());
		
		
		
		Gson gson = new Gson();		
		
		for (Data data : dataList) {		
			
			try {
				String dataString = gson.toJson(data);
				//File file = getDataFile(path);
				
//				PrintStream w = null;
//				try {
//					w = new PrintStream(new RobocodeFileOutputStream(getDataFile("count.dat")));
//
//					w.println(roundCount);
//					w.println(battleCount);
				File dir = new File("statistics");
				dir.mkdirs();
				File file = new File("statistics/" + data.getRobotName() + ".json");				
				RobocodeFileWriter writer = new RobocodeFileWriter(file);
				writer.write(dataString);
				writer.close();
				System.out.println("Data saved to ." + file.getAbsolutePath());
			} catch (JsonIOException | IOException e) {				
				e.printStackTrace();
			}
			
		}		
	}
	
	private Data FindDataByName(String name) {
		
		String robotName = name;
		
		// Check 
		if(name.contains(" ")) {
			int j = name.indexOf(" ");
			robotName = name.substring(0, j);
			//System.out.println("Multiple Instances: " + robotName);
		}	
		
		for (Data data : dataList) {
			if(data.getRobotName().equals(robotName)) {
				return data;
			}
		}
		return null;
	}
	
	private boolean checkForData(String name) {
		
		String robotName = name;
		
		// Check 
		if(name.contains(" ")) {
			int i = name.indexOf(" ");
			robotName = name.substring(0, i);
			System.out.println("Multiple Instances: " + robotName);
		}
		
		URL path = this.getClass().getResource("/" + robotName + ".dat");
		
		if(path == null) {
			return false;
		}
		
		File file = new File(path.toString());
		
		return file.exists();
	}

}