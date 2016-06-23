package helper.strategies;

import robots.TestBot;

public class StopMovement extends MovementStrategy {

	@Override
	public void execute(TestBot robot) {		
		robot.setMaxVelocity(0);
	}

}
