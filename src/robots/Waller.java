package robots;

import java.awt.Color;

import robocode.AdvancedRobot;
import robocode.HitWallEvent;

public class Waller extends AdvancedRobot{
	
	public void run() {	
	
		// Color
		setBodyColor(Color.red);
		setGunColor(Color.red);
		setRadarColor(Color.black);
		setBulletColor(Color.red);		
		
		setMaxVelocity(4);
		
		setAhead(5000);		
		
	}

	public void onHitWall(HitWallEvent event) {		
		
		turnRight(180);
		setAhead(5000);		
		
	}

}
