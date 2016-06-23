package helper.strategies;

import robots.TestBot;

public class SpinAroundMovement extends MovementStrategy {

	@Override
	public void execute(TestBot robot) {
		robot.setMaxVelocity(0);
		robot.setTurnRight(3600);
	}

}
