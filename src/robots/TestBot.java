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

	private EnemyBot[] enemies;
	private EnemyBot attacker = new EnemyBot(); // Robot which last attacked us
	private EnemyBot target = new EnemyBot();
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
		findDataByName(target.getName()).BulletHit(false, fireMode);
	}

	public void onBulletHit(BulletHitEvent event) {		
		findDataByName(target.getName()).BulletHit(true, fireMode);
		bulletHit = true;
	}
	
	public void onWin(WinEvent event) {
		
		
	}
	
	public void onDeath(DeathEvent event) {
		for (Data data : dataList) {
			data.lost();
		}
	}

	public void onRoundEnded(RoundEndedEvent event) {
		System.out.println("Round ended");
		
		if(getEnergy() > 0) {
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
		if(gameOver) {
			return;
		}	

		// Increment Time Handler
		if (!scanStarted) {
			scanElapsedTime++;
		}		

		// Periodic scan
		if (scanElapsedTime >= scanTimer) {
			if (enemies != null && enemies.length > 1 && periodicScan) {

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
			data = loadData(robotName);
			if(data == null) {
				System.out.println("Loading failed horribly :(");
			}
			dataList.add(data);
			System.out.println("Added " + data + " to DataList.");
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
		        setFire(power);
		    }			
		}
		
		if(fireMode == FireMode.GuessFactor) {
			
			// Guess Targeting
			double absBearing = enemy.getBearingRadians() + getHeadingRadians();
			double power = Math.min(3, Math.max(.1, 400 / target.getInfo().getDistance()));
			
			if (target.getInfo().getVelocity() != 0) {
				if (Math.sin(target.getInfo().getHeadingRadians() - absBearing) * target.getInfo().getVelocity() < 0)
					direction = -1;
				else
					direction = 1;
			}
			Data data = findDataByName(target.getName());			
			int[] currentStats = data.getStats()[(int)(target.getInfo().getDistance() / 100)];
			
			WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, power, direction, getTime(), currentStats);				
			
			int bestindex = 15; // initialize it to be in the middle, guessfactor 0.
			for (int i = 0; i < 31; i++) {
				if (currentStats[bestindex] < currentStats[i]) {
					bestindex = i;				
				}			
			}
			
			// this should do the opposite of the math in the WaveBullet:
			double guessfactor = (double) (bestindex - (currentStats.length - 1) / 2) / ((currentStats.length - 1) / 2);
			double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
			double gunAdjust = Utils.normalRelativeAngle(absBearing - getGunHeadingRadians() + angleOffset);
			
			setTurnGunRightRadians(gunAdjust);

			if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, target.getInfo().getDistance()) && setFireBullet(power) != null) {				
				waves.add(newWave);							
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
		double avoidDistance = 30 + 10 * Rules.MAX_VELOCITY;

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
		// TODO if he hits the wall a 2. time in a short time frame it wont be detected
		double wallDmg = target.getInfo().getVelocity() * 0.5 - 1;
		if(deltaEnergy == wallDmg){
			System.out.println("bot hit wall");
			System.out.println("------");
			return;
		}
		
		//check if enemy hits another bot
		//TODO verify with coordinates
		//TODO never triggers (maybe collision dmg isn't 0.6?)
		if(deltaEnergy == 0.6){
			System.out.println("bot hit bot");
			System.out.println("------");
			return;
		}
		
		//TODO doesn't detect bullet shots while crashing
		double bulletVelocity = 20 - 3 * deltaEnergy; 
		System.out.println("AVOID: " + deltaEnergy + " BulletVelocity: " + bulletVelocity);

		//TODO when robot turns the radar turns and he looses the target
		//if not avoiding the wall, make a random movement
		//if(avoidWall == AvoidWall.None){
			randomMovement();
		//}
	}
	 
	/**
	 * turn the robot in a random direction but not straight to or away from the enemy
	 * ("straight" is defined in an angle which gets bigger as closer we are to the enemy)
	 *  
	 */
	private void randomMovement() {
		ScannedRobotEvent bot = target.getInfo();
		double botDistance = bot.getDistance();
		
		//TODO change deltaAngle according to the distance to the enemy (between 30-80°)  
		double deltaAngle = 30;		
		deltaAngle = deltaAngle + (botDistance/50)*5;
		if(deltaAngle > 80) deltaAngle = 80;
		
		double angleDeg = (this.getHeading() + bot.getBearing()) % 360;
		if(angleDeg < 0) angleDeg+=360;
		Random rand = new Random();
		double randAngle;
		double deltaMin;
		double deltaMax;
		//gives a random angle which is not to the enemy or the opposite direction
		do{
			randAngle = rand.nextDouble()*360;
			deltaMin = angleDeg - deltaAngle;
			if(deltaMin<0) deltaMin+=180;
			deltaMax = angleDeg + deltaAngle;
		}while (randAngle < deltaMin && randAngle > deltaMax 
				|| randAngle < (deltaMin+180) % 360 && randAngle > (deltaMax+180) % 360);
		
		//turns to rand direction to the bearing to robot has to change
		double randBearing = randAngle - this.getHeading();
		
		System.out.println("own heading: " + this.getHeading() + " enemy bearring: " + bot.getBearing());
		System.out.println("angle to enemy: " + angleDeg + " Choosen angle: " +randAngle);	
		System.out.println("my bearing: " +randBearing +"enemy dist: " + bot.getDistance());
		
		//change the movement direction if the bearing is >90°/<-90° and turn accordingly
		if(moveDirection > 0){
			if(randBearing < 90 && randBearing > -90){
				turnRight(randBearing);
			}else{
				moveDirection*=-1;
				if(randBearing <0) randBearing +=180;
				else randBearing-=180;
				turnRight(randBearing);
				System.out.print("change direction and ");
			}
			System.out.println("turn " + randBearing + " md: " + moveDirection);
		}else{
			if(randBearing < 90 && randBearing > -90){
				moveDirection*=-1;
				turnRight(randBearing);
				System.out.print("change direction and ");
			}else{
				if(randBearing <0) randBearing +=180;
				else randBearing-=180;
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
		
		// TODO: 
		
		fireMode = FireMode.LinearTargeting;
		
	}
	
	/**
	 * Tries to load the data file with the specified name, return the object if found or null if not.
	 * @param robotName Name of the corresponding robot.
	 * @return The loaded data object or null.
	 */
	private Data loadData(String robotName) {		
		
		System.out.println("Trying to load " + robotName + ".json");		
		
		File file = getDataFile(robotName + ".json");
		if(file.exists()) {
			
			Gson gson = new Gson();			
			
			try {
				return gson.fromJson(new FileReader(file), Data.class);
			} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {				
				e.printStackTrace();
			}			
		}		
		return null;
	}
	
	private void saveData() {
		System.out.println("Saving Data " + dataList.size());	
		
		Gson gson = new Gson();		
		
		for (Data data : dataList) {		
			
			try {
				String dataString = gson.toJson(data);	
//				File dir = new File(dataDirectory);
//				dir.mkdirs();
				File file = getDataFile(data.getRobotName() + ".json"); //new File(dataDirectory + "/" + data.getRobotName() + ".json");				
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
				
		File file = getDataFile(robotName + ".json");			
		
		return file.exists();
	}

}