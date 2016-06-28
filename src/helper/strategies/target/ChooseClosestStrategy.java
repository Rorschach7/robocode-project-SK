package helper.strategies.target;

import helper.Bot;
import helper.Enums.State;

import java.util.ArrayList;

import robots.BaseBot;

public class ChooseClosestStrategy extends TargetStrategy {

	@Override
	public Bot execute(BaseBot robot) {
		ArrayList<Bot> enemies = robot.getEnemies();
		if (enemies == null) {
			return null;
		}	
		
		Bot target = null;
		// Init or assign current target
		if(robot.getTarget() == null || robot.getTarget().isDead()) {
			// Choose next best enemy
			for (Bot bot : enemies) {
				if(!bot.isDead()) {
					target = bot;
					break;
				}
			}			
		} else {
			target = robot.getTarget();			
		}
		
		// Check if another robot is ramming us which is not already our target.
		// This one should then be the closest enemy there is.
		if(robot.isHitRobot() && !robot.getMeleeAttacker().getName().equals(robot.getTarget().getName())) {			
			robot.setState(State.Scanning);
		}
		

		// Find closest enemy
		for (Bot bot : enemies) {
			if(bot.isDead()) {
				continue;
			}
			if (target.getInfo().getDistance() > bot.getInfo()
					.getDistance()) {
				target = bot;
			}
		}		
		return target;		
	}
	
	@Override
	public String toString() {		
		return super.toString() + "Choose Closest";
	}

}
