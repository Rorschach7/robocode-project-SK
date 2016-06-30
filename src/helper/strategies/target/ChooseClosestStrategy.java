package helper.strategies.target;

import helper.Bot;
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
