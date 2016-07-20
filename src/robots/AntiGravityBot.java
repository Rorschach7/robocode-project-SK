package robots;

import helper.strategies.gun.DynamicChange;
import helper.strategies.movement.AntiGravity;
import helper.strategies.movement.RandomMovement;
import helper.strategies.movement.SpinAroundMovement;
import helper.strategies.movement.StopMovement;

import java.awt.Color;

public class AntiGravityBot extends BaseBot {
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
		attackingMovement = new AntiGravity();
		scanningMovement = new StopMovement();
		dodgeBullet = new RandomMovement();
		victoryDance = new SpinAroundMovement();

		printStatus = false;
	}
}
