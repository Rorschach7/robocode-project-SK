package robots;

import java.awt.Color;

import helper.strategies.*;

public class MilestoneBot extends BaseBot {
	
	@Override
	public void initialize() {		
		super.initialize();
		
		// Color
		setBodyColor(Color.black);
		setGunColor(Color.red);
		setRadarColor(Color.red);
		setBulletColor(Color.green);
		
		// Strategies
		aimStrategy = new DynamicChange();
		dodgeBullet = new RandomMovement(); 
		victoryDance = new SpinAroundMovement();
		
		printStatus = false;
	}

}
