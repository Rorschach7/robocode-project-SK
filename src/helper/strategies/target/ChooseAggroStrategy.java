package helper.strategies.target;

import helper.Bot;
import helper.FuncLib;
import java.util.ArrayList;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robots.BaseBot;

public class ChooseAggroStrategy extends TargetStrategy {
	
	private Bot target;

	@Override
	public void execute(BaseBot robot) {
		
		if(isSkipTargeting()) {
			// Reset
			setSkipTargeting(false);			
		}
		
		// Find new target
		// Find a target among all enemies
		ArrayList<Bot> enemies = robot.getEnemies();
		if (enemies == null) {
			return;
		}	
		
		target = null;
		// Init or assign current target
		for (Bot bot : enemies) {
			if(!bot.isDead()) {
				target = bot;
				break;
			}
		}
		
		// Find bot with the most aggro
		getAggroBot(robot);		
		
		// Fallback case, find closest enemy
		if(target.getAggro() < 1.0 || target.isDead()) {	// TODO: nullptr
			// Aggro is not convincing
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
		}
		
		System.out.println("INFO: new target: " + target);
		robot.setTarget(target);		
	}	
	
	
	@Override
	public void onCollision(BaseBot robot, HitRobotEvent event) {
		// Make the robot that just rammed into as, our new target
		//System.out.println(robot.getTarget().getName() + " " + event.getName());
		if(robot.getTarget() == null) {
			return;
		}
		if (!robot.getTarget().getName().equals(event.getName())) {
			System.out.println("Who rammed us?");
			//robot.setState(State.Scanning);
			robot.setTarget(FuncLib.findBotByName(event.getName(), robot.getEnemies()));
		}
	}
	
	@Override
	public void onHitByBullet(BaseBot robot, HitByBulletEvent event) {
		// Find the enemy with the most aggro
		getAggroBot(robot);	
		
		if(!robot.getTarget().getName().equals(target.getName())) {			
			robot.setTarget(target);
			setSkipTargeting(true);
			System.out.println("INFO: New Target: " + target.getName() + "Aggro: " + target.getAggro());
		}		
	}
	
	private void getAggroBot(BaseBot robot) {
		for (Bot enemy : robot.getEnemies()) {
			if(!enemy.isDead() && enemy.getAggro() > target.getAggro()) {
				target = enemy;
			}
		}
	}
	
	@Override
	public String toString() {		
		return super.toString() + "Choose Closest";
	}

}
