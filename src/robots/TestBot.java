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
	Circle, Eight
}

public class TestBot extends AdvancedRobot {	
	
	// Variables	
	private int moveDirection = 1;// which way to move
	private State state = State.Spotting;
	private int count = 0; // Count for movement patterns
	private String targetName; // Name of our target
	private String attackerName; // Robot which last attacked us
	
	
	public void run() {	
		
		setBodyColor(Color.black);
		setGunColor(Color.yellow);
		setRadarColor(Color.black);
		setBulletColor(Color.green);
		
		setAdjustRadarForRobotTurn(true);		
		setAdjustGunForRobotTurn(true); // Keep turret still while moving
		turnRadarRight(Double.POSITIVE_INFINITY);
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {	
				
		double absBearing = e.getBearing() + getHeading();
		double gunTurnAmt;
		
		// Radar Stuff
		setTurnRadarLeft(getRadarTurnRemaining());// Lock on the radar
		
		// Gun Stuff
		gunTurnAmt = normalRelativeAngleDegrees(absBearing - getGunHeading());
		setTurnGunRight(gunTurnAmt); // turn gun
		
		
		// Movement Stuff
		//setTurnRight(normalRelativeAngleDegrees(absBearing - getHeading()));
		//setAhead((e.getDistance() - 140) * moveDirection);
		
		// Drive in Circles
		Random random = new Random();
		int rnd = random.nextInt((2 - 1) + 1) + 1;
		if(rnd == 1) {
			moveDirection = -1;
		} else {
			moveDirection = 1;
		}
		
		//setTurnRight(45);
		//setAhead(10);
		
		Do();
		
		System.out.println(count);
		
		setFire(400 / e.getDistance());			
	
	}
	
	public void onStatus(StatusEvent event) {
		System.out.println("onStatus");
	}
	
	private void Do() {
		
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
	}

}
