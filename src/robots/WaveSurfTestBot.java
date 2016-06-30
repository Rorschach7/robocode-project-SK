package robots;

import helper.strategies.gun.DynamicChange;
import helper.strategies.movement.RandomMovement;
import helper.strategies.movement.SpinAroundMovement;
import helper.strategies.movement.WaveSurfing;

import java.awt.Color;

public class WaveSurfTestBot extends BaseBot {
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
		dodgeBullet = new WaveSurfing();
		victoryDance = new SpinAroundMovement();

		printStatus = false;
	}
}
