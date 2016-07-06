package robots;

import java.awt.Color;

import helper.strategies.gun.DynamicChange;
import helper.strategies.movement.RandomMovement;
import helper.strategies.movement.SpinAroundMovement;

public class Meteor extends BaseBot {
	
	@Override
	public void initialize() {		
		super.initialize();
		
		// Color
		setBodyColor(Color.black);
		setGunColor(Color.red);
		setRadarColor(Color.black);
		setBulletColor(Color.red);
		
		// Strategies
		gunStrategy = new DynamicChange();		
		dodgeBullet = new RandomMovement(); 
		victoryDance = new SpinAroundMovement();
		
		printStatus = false;
		printData =false;
		
	}

}
