package helper.strategies.target;

import helper.Bot;

import java.util.ArrayList;

import robots.BaseBot;

public class ChooseWeakestStrategy extends TargetStrategy {

	@Override
	public Bot execute(BaseBot robot) {
		ArrayList<Bot> enemies = robot.getEnemies();
		if (enemies == null) {
			return null;
		}
		
		Bot target = null;
		// Init or assign any enemy as current target
		if(robot.getTarget() == null) {
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
		
		for (Bot bot : enemies) {
			if(!bot.isDead() && bot.getInfo().getEnergy() < target.getInfo().getEnergy()) {
				target = bot;
			}
		}		
		
		return target;
	}
	
	@Override
	public String toString() {		
		return super.toString() + "Choose Weakest Strategy";
	}

}