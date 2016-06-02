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

enum RadarState {
	Lock, Sweep, FullScan
}

public class TestBot extends TeamRobot {
	
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
			System.out.println("Sweep found target, lock on");
			radarState = RadarState.Lock;
			isEnemyLocked = true;					
		}		
		
		if (radarState == RadarState.Lock) {
			if (target.getName().equals(e.getName())) {				
				isEnemyLocked = true;
				
				runScan(RadarState.Lock);

//				// Lock on
//				double angleToEnemy = getHeading() + target.getInfo().getBearing();
//
//				double radarTurn = Utils.normalRelativeAngleDegrees(angleToEnemy - getRadarHeading());
//
//				setTurnRadarRight(radarTurn);					
			}
		}
		
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
		
		if(getEnergy() <= EnergyThreshold) {
			state = State.Evading;
		}
	}
	
	public void onHitRobot(HitRobotEvent event) {
		hitRobot = true;
	}
	
	public void onRobotDeath(RobotDeathEvent event) {
		// Remove robot from enemies array
		for(int i = 0; i < enemies.length; i++) {
			if(enemies[i].getName().equals(event.getName())) {
				enemies[i] = null;
				return;
			}
		}	
		
		if(target.getName().equals(event.getName())) {
			findTarget();
		}
	}
	
	public void onHitWall(HitWallEvent event) {		
		
		if(movePattern == MovementPattern.UpAndDown) {
			moveDirection *= -1;
			setAhead(moveDirection * 5);
		}
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
		gameOver = true;
   }
	
	public void onStatus(StatusEvent event) {
		if(gameOver) {
			return;
		}
		
		// Increment Time Handler
		if(!scanStarted) {
			scanElapsedTime++;
		}		
		
		// Periodic scan	
		if(scanElapsedTime >= scanTimer) {
			if(enemies != null && enemies.length > 1) {
				state = State.Scanning;
			}			
			scanElapsedTime = 0;
		}		
		
				
		// Avoid walls
		avoidWalls();		
		
		// Execute behavior for corresponding state		
		if(state == State.Attacking) {
			
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
				fireGun(target);
			} else {
				System.out.println("Enemy no longer locked.");
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
		
		if(state == State.Evading) {
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
	 * @param pattern the movement pattern that should be executed
	 */
	private void runMovementPattern(MovementPattern pattern) {
		movePattern = pattern;
		
		
		
		// Pattern Eight
		if(pattern == MovementPattern.Eight) {
			
			if(count < 80) {
				setTurnRight(turnDirection * 45);
				setAhead(10);
			} else {
				setTurnRight(turnDirection * -45);
				setAhead(10);
			}
					
			count++;
			if(count == 160) {
				count = 0;
			}
			return;
		}	
		
		// Pattern Circle 
		if(pattern == MovementPattern.Circle) {
			
			setTurnRight(turnDirection * 10);
			setAhead(10);			
					
			count++;
			if(count == 100) {
				count = 0;
			}
			return;
		}
		
		// Pattern Scanning
		if(pattern == MovementPattern.Scanning) {
			
			setTurnRight(10);
			setAhead(35);			
					
			count++;
			if(count == 100) {
				count = 0;
			}
			return;
		}
		
		// Pattern Approaching
		if(pattern == MovementPattern.Approach) {
			
			
			
		}
		
		// Pattern Stop
		if(pattern == MovementPattern.Stop) {
			
			// Do nothing
			
		}
		
		// Pattern Up and Down
		if(pattern == MovementPattern.UpAndDown) {
			
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
		System.out.println("Shots Fired: " + (shotsHit + shotsMissed));
		System.out.println("Shots Hits: " + shotsHit);
	    System.out.println("Shots Missed: " + shotsMissed);
	    double acc = 0;
	    if((shotsHit + shotsMissed) != 0) {
	    	acc = shotsHit / (shotsHit + shotsMissed) * 100;
	    }	    
	    System.out.println("Accuracy: " + acc);
		System.out.println("---------------------------------------------------------");
	}
	
	/**
	 * Updates the enemies array
	 * @param robot the robot that should be updated
	 */
	public void update(ScannedRobotEvent robot) {		
		// Scan energy
		if (!(target.getName().equals("None")) && target.getName().equals(robot.getName()))  {
			double enemyDeltaEnergy = target.getInfo().getEnergy() - robot.getEnergy();
			if(enemyDeltaEnergy > 0) {
				avoidBullet(enemyDeltaEnergy);				
			}
		}
				
		// Update robot
		if(enemies != null) {			
			for(int i = 0; i < enemies.length; i++) {			
				if(enemies[i] == null) {
					continue;
				}
				if( enemies[i].getName().equals(robot.getName())) {
					enemies[i].init(robot);							
					return;
				}
			}
		}
		
		
		// Add not existing robot		
		int n = 0;
		if(enemies == null) {
			n = 1;
		} else {
			n = enemies.length + 1;
		}
		EnemyBot[] newEnemies = new EnemyBot[n];
		
		for(int i = 0; i < n - 1; i++) {
			if(enemies[i] != null) {
				newEnemies[i] = enemies[i];
			}
		}
		newEnemies[n - 1] = new EnemyBot();
		newEnemies[n - 1].init(robot);
		enemies = newEnemies;
	}
	
	/**
	 * Fires the tank's gun. Uses linear targeting and gun power dependent on distance between tank and enemy
	 * @param target the robot we want to shoot
	 */
	private void fireGun(EnemyBot target) {
		ScannedRobotEvent enemy = target.getInfo();
		double absBearing = enemy.getBearing() + getHeading();
		double gunTurnAmt;
		
		// TODO: Skip linear targeting if enemy is too close
		// Calculate enemie's lateral velocity
		double latVel = enemy.getVelocity() * Math.sin(enemy.getHeadingRadians() - Math.toRadians(absBearing));
				
		double bulletSpeed = 20 - 3 * (400 / target.getInfo().getDistance());
		
		gunTurnAmt = normalRelativeAngleDegrees(absBearing - getGunHeading() + ((latVel / bulletSpeed) * 57.3) );
		setTurnGunRight(gunTurnAmt); // Turn gun
		
		if(target.getInfo().getDistance() < 600 && getEnergy() > EnergyThreshold) {				
			if(Math.abs(getGunTurnRemaining()) <= 5) { // Don't shoot before gun is adjusted
				setFire(400 / target.getInfo().getDistance());
				//System.out.println("FIRE");
			}				
		}	
		return;
	}
	
	/**
	 * Calculates future position and checks whether the tank will collide with a wall or not.
	 * @return true, if wall will be hit. Otherwise false.
	 */
	private boolean avoidWalls() {
		// TODO: consider moveDirection!
		// TODO: consider velocity for detection
		// TODO: block other movement
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
				setTurnLeft(turnDegree);
			else if (this.getHeading() > 90 && this.getHeading() <= 180)
				setTurnRight(turnDegree);
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
				setTurnLeft(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (yDist >= fieldHeight - 36 || avoidWall == AvoidWall.North) {
			// going to hit north wall
			avoidWall = AvoidWall.North;
			if (this.getHeading() > 270 && this.getHeading() <= 360)
				setTurnLeft(turnDegree);
			else if (this.getHeading() > 0 && this.getHeading() <= 90)
				setTurnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		if (yDist <= 18 || avoidWall == AvoidWall.South) {
			// going to hit south wall
			avoidWall = AvoidWall.South;
			if (this.getHeading() > 90 && this.getHeading() <= 180)
				setTurnLeft(turnDegree);
			else if (this.getHeading() > 180 && this.getHeading() <= 270)
				setTurnRight(turnDegree);
			else
				avoidWall = AvoidWall.None;
			return true;
		}
		return false;
	}
	
	/**
	 * Tries to figure out if enemy tank shoots at us and starts evasive maneuver if  
	 * @param deltaEnergy the energy the enemy tank lost between last and current turn
	 */
	private void avoidBullet(double deltaEnergy) {
		
		if(deltaEnergy > 3 || bulletHit || hitRobot) {
			bulletHit = false;
			hitRobot = false;
			return;
		}
		
		// Check if enemy hit a wall
		// TODO:	
		
		System.out.println("AVOID: " + deltaEnergy);			
		moveDirection *= -1;
		Random rand = new Random();
		int rnd = rand.nextInt(6) + 3;
		setMaxVelocity(rnd);		
	}
		
	/**
	 * Finds a target among all spotted enemies
	 */
	private void findTarget() {
		
		if(enemies == null) {
			return;
		}
		
		target = enemies[0];
		
		// Find closest enemy
		for(int i = 0; i < enemies.length; i++ ) {
			if(enemies[i] == null) {
				continue;
			}
			if(target.getInfo().getDistance() > enemies[i].getInfo().getDistance()) {
				target = enemies[i];
			}
		}
		
	}	
	
	private void runScan(RadarState scan) {				
				
		System.out.println("RunScan: " + scan);
		
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

}