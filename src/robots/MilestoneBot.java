package robots;

import java.awt.Color;
import helper.strategies.gun.DynamicChange;
import helper.strategies.movement.RandomMovement;
import helper.strategies.movement.SpinAroundMovement;

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
		gunStrategy = new DynamicChange();		
		dodgeBullet = new RandomMovement(); 
		victoryDance = new SpinAroundMovement();
		
		printStatus = false;
	}

}
