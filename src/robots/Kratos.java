package robots;

import java.awt.Color;

import helper.strategies.gun.DynamicChange;
import helper.strategies.movement.RandomMovement;
import helper.strategies.movement.SpinAroundMovement;
import helper.strategies.movement.StopMovement;

public class Kratos extends BaseBot {
	
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
		scanningMovement = new StopMovement();
		attackingMovement = new StopMovement();
		victoryDance = new SpinAroundMovement();
		
		// Outputs
		printStatus = false;
		printData = false;		
	}

}
