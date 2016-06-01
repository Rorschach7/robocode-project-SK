package robots;

import robocode.*;
import static robocode.util.Utils.*;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Random;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

enum State {
	Spotting, Attacking, Evading, Finishing
}

enum MovementPattern {
	Circle, Eight, Scanning, Approach, Stop, UpAndDown
}

public class TestBot extends TeamRobot {	
	
	final int N = 5;
	
	// Variables	
	private int moveDirection = 1;// >0 : turn right, <0 : tun left
	private int turnDirection = 1;
	private int count = 0; // Count for movement patterns
	private double EnergyThreshold = 35;
	private boolean resetMovement = true;
	
	private State state = State.Spotting;
	private MovementPattern movePattern = MovementPattern.Stop;
	
	private double deltaTime;
	private double startTime;
	
	private boolean lockTarget = true;
	private EnemyBot[] enemies = new EnemyBot[N];
	private EnemyBot attacker = new EnemyBot(); // Robot which last attacked us
	private EnemyBot target;
	private AvoidWall avoidWall;
	
	// Statistics
	private int shotsHit = 0;
	private int shotsMissed = 0;

	private boolean bulletHit;
	
	public void run() {	
		
		for(int i= 0; i < N; i++) {
			enemies[i] = new EnemyBot();			
		}
		
		// Color
		setBodyColor(Color.black);
		setGunColor(Color.black);
		setRadarColor(Color.yellow);
		setBulletColor(Color.green);
		
		startTime = System.currentTimeMillis();

		setAdjustGunForRobotTurn(true); // Keep turret still while moving
				
		while(true) {
			
			if(getRadarTurnRemainingRadians() == 0.0) {
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}
			execute();
		}		
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		update(e);
		
		// Absolute angle towards target
	    double angleToEnemy = getHeadingRadians() + e.getBearingRadians();
	 
	    // Subtract current radar heading to get the turn required to face the enemy, be sure it is normalized
	    double radarTurn = Utils.normalRelativeAngle( angleToEnemy - getRadarHeadingRadians() );
	 
	    // Distance we want to scan from middle of enemy to either side
	    // The 36.0 is how many units from the center of the enemy robot it scans.
	    double extraTurn = Math.min( Math.atan( 36.0 / e.getDistance() ), Rules.RADAR_TURN_RATE_RADIANS );
	 
	    // Adjust the radar turn so it goes that much further in the direction it is going to turn
	    // Basically if we were going to turn it left, turn it even more left, if right, turn more right.
	    // This allows us to overshoot our enemy so that we get a good sweep that will not slip.
	    radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);
	 
	    //Turn the radar
	    setTurnRadarRightRadians(radarTurn);		
		
		state = State.Attacking;		
	
	}
	
	public void onHitByBullet(HitByBulletEvent event) {
		attacker.init(event);
		
		if(getEnergy() <= EnergyThreshold) {
			state = State.Evading;
		}
	}
	
	public void onHitWall(HitWallEvent event) {		
		
		if(movePattern == MovementPattern.UpAndDown) {
			moveDirection *= -1;
			setAhead(moveDirection * 5);
		}
		
		// TODO:
		// Implement a DELAYED change of the current movePattern, so we wont crash into the wall again
	}
	
	
	public void onBulletMissed(BulletMissedEvent event) {
	    shotsMissed++;
	}
	
	public void onBulletHit(BulletHitEvent event) {	      
		shotsHit++;
		bulletHit = true;
   }
	
	public void onRoundEnded(RoundEndedEvent event) {	      
	   }
	
	public void onStatus(StatusEvent event) {		
		
		deltaTime = (System.currentTimeMillis() - startTime) / 1000.0;
		startTime = System.currentTimeMillis();		
				
		// Avoid walls
		avoidWalls();
		
		
		
		// Execute behavior for corresponding state		
		if(state == State.Attacking) {
			target = enemies[0];		
						
			fireGun(target);		
			
			
			// Run Attacking movement pattern/strategy
			RunMovementPattern(MovementPattern.UpAndDown); // Needs to be adjusted, should try to get closer to enemy etc
		}
		
		if(state == State.Spotting) {
			// not really necessary, I think 
			
			
		}
		
		if(state == State.Evading) {
			// TODO:
			// Implement anti gravity stuff here
			// Or short evasive maneuver
			RunMovementPattern(MovementPattern.Stop);
			
		}
		
		if(state == State.Finishing) {
			// TODO:
			// Implement aggressive close combat behavior here, don't let enemy escape
			
			
		}
		
		//printStatus();
		
	}
	
	/**
	 * Executes the specified movement pattern
	 * @param pattern the movement pattern that should be executed
	 */
	private void RunMovementPattern(MovementPattern pattern) {
		movePattern = pattern;
		
		
		
		// Pattern Eight
		if(pattern == MovementPattern.Eight) {
			
			if(count < 40) {
				setTurnRight(turnDirection * 45);
				setAhead(10);
			} else {
				setTurnRight(turnDirection * -45);
				setAhead(10);
			}
					
			count++;
			if(count == 100) {
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
		System.out.println("Current MovementPattern: " + movePattern.name() + "\n" + 
							"Count: " + count + "\n" +
							"Attacker: " + attacker.getName() + "\n" +
							"Target: " + target.getName() + "\n" + 
							"State: " + state);
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
		if (target != null) {
			double enemyDeltaEnergy = target.getInfo().getEnergy() - robot.getEnergy();
			if(enemyDeltaEnergy > 0) {
				avoidBullet(enemyDeltaEnergy);				
			}
		}
				
		for(int i = 0; i < N; i++) {
			// Update robot			
			if( enemies[i].getName().equals(robot.getName())) {
				enemies[i].init(robot);
				if(target == null || !lockTarget) {
					target = enemies[i];
				}				
				return;
			}
		}
		
		// Add not existing robot
		for(int i = 0; i < N; i++) {
			if(enemies[i].getName().equals("None")) {
				enemies[i].init(robot);
				if(target == null || !lockTarget) {
					target = enemies[i];
				}				
				return;
			}
		}
		
	}
	
	/**
	 * Fires the tank's gun. Uses linear targeting and gun power dependent on distance between tank and enemy
	 * @param target
	 */
	private void fireGun(EnemyBot target) {
		ScannedRobotEvent enemy = target.getInfo();
		double absBearing = enemy.getBearing() + getHeading();
		double gunTurnAmt;
		
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
	 * Tries to figure out if enemy tank shots at us and starts evasive maneuver if  
	 * @param deltaEnergy the energy the enemy tank lost between last and current turn
	 */
	private void avoidBullet(double deltaEnergy) {
		
		if(deltaEnergy <= 3 || bulletHit) {
			System.out.println("AVOID: " + deltaEnergy);
			bulletHit = false;
		}
		
	}

}