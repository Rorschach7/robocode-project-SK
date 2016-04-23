package robots;

import robocode.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;
import robocode.ScannedRobotEvent;

public class TestBot extends AdvancedRobot {	
	
	// Variables	
	private int moveDirection = 1;// which way to move
	
	public void run() {				
		setAdjustRadarForRobotTurn(true);		
		setAdjustGunForRobotTurn(true); // Keep turret still while moving
		turnRadarRight(Double.POSITIVE_INFINITY);
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {	
		
		double absBearing = e.getBearing() + getHeading();
		double gunTurnAmt;
				
		setTurnRadarLeft(getRadarTurnRemainingRadians());// Lock on the radar
		
		gunTurnAmt = normalRelativeAngleDegrees(absBearing - getGunHeading());
		setTurnGunRight(gunTurnAmt); // turn gun
		setTurnRight(normalRelativeAngleDegrees(absBearing - getHeading()));
		setAhead((e.getDistance() - 140) * moveDirection);
		setFire(400 / e.getDistance());			
	
	}

}
