package robots;

import robocode.*;
import static robocode.util.Utils.*;

import java.awt.Color;
import java.util.Random;
import robocode.ScannedRobotEvent;

enum State {
	Spotting, Attacking, Evading, Finishing
}

enum MovementPattern {
	Circle, Eight, Scanning, Approach
}

public class TestBot extends AdvancedRobot {	
	
	final int N = 5;
	
	// Variables	
	private int moveDirection = 1;// which way to move
	private State state = State.Spotting;
	private int count = 0; // Count for movement patterns
	private MovementPattern movePattern;
	private boolean lockTarget = true;
	private EnemyBot[] enemies = new EnemyBot[N];
	private EnemyBot attacker = new EnemyBot(); // Robot which last attacked us
	private EnemyBot target;
	
	public void run() {	
		
		for(int i= 0; i < N; i++) {
			enemies[i] = new EnemyBot();			
		}
		
		// Color
		setBodyColor(Color.black);
		setGunColor(Color.yellow);
		setRadarColor(Color.black);
		setBulletColor(Color.green);
		

		setAdjustGunForRobotTurn(true); // Keep turret still while moving
		turnRadarRight(Double.POSITIVE_INFINITY);
		RunMovementPattern(MovementPattern.Scanning);
		
		setAdjustRadarForRobotTurn(true);		
		
		
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		Update(e);
		
		state = State.Attacking;		
		RunMovementPattern(MovementPattern.Eight);
				
		// Radar Stuff
		setTurnRadarLeft(getRadarTurnRemaining());// Lock on the radar
		
						
	
	}
	
	public void onHitByBullet(HitByBulletEvent event) {
		attacker.Init(event);
	}
	
	public void onHitWall(HitWallEvent event) {
		
		System.out.println( "Wall Hit Bearing" + event.getBearing());
		//turnRight(event.getBearing());
		
	}
	
	public void onStatus(StatusEvent event) {
		
		if(state == State.Attacking) {
			target = enemies[0];
			double absBearing = target.getBearing() + getHeading();
			double gunTurnAmt;
						
			// Gun Stuff
			gunTurnAmt = normalRelativeAngleDegrees(absBearing - getGunHeading());
			setTurnGunRight(gunTurnAmt); // turn gun
			
			if(target.getDistance() < 350) {
				setFire(400 / target.getDistance());
			}
			
				
		}
		
		printStatus();
		
	}
	
	private void RunMovementPattern(MovementPattern pattern) {
		movePattern = pattern;
		
		if(pattern == MovementPattern.Eight) {
			
			if(count < 40) {
				setTurnRight(45);
				setAhead(10);
			} else {
				setTurnRight(-45);
				setAhead(10);
			}
					
			count++;
			if(count == 100) {
				count = 0;
			}
			return;
		}	
		
		if(pattern == MovementPattern.Circle) {
			
			setTurnRight(10);
			setAhead(10);			
					
			count++;
			if(count == 100) {
				count = 0;
			}
			return;
		}
		
		if(pattern == MovementPattern.Scanning) {
			
			setTurnRight(10);
			setAhead(35);			
					
			count++;
			if(count == 100) {
				count = 0;
			}
			return;
		}
		
		if(pattern == MovementPattern.Approach) {
			
			
			
		}
		
	}
	
	public void printStatus() {
		System.out.println("Current MovementPattern: " + movePattern.name() + "\n" + 
							"Count: " + count + "\n" +
							"Attacker: " + attacker.getName() + "\n" +
							"Target: " + target.getName());
	}
		
	public void Update(ScannedRobotEvent robot) {		
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

}
