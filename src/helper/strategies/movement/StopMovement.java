package helper.strategies.movement;

import robots.BaseBot;

public class StopMovement extends MovementStrategy {

	@Override
	public void execute(BaseBot robot) {		
		robot.setMaxVelocity(0);
	}

}
