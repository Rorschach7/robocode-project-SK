package helper.strategies;

import robots.BaseBot;

public class SpinAroundMovement extends MovementStrategy {

	@Override
	public void execute(BaseBot robot) {
		robot.setMaxVelocity(0);
		robot.setTurnRight(3600);
	}

}
