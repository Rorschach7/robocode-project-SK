package helper.strategies.target;

import helper.Bot;
import helper.FuncLib;
import java.util.ArrayList;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robots.BaseBot;

public class ChooseAggroStrategy extends TargetStrategy {
	
	

	@Override
	public void execute(BaseBot robot) {
		
		if(isSkipTargeting()) {
			// Reset
			setSkipTargeting(false);			
		}
		System.out.println("FIND NEW TARGET");
		// Find new target
		// Find a target among all enemies
		ArrayList<Bot> enemies = robot.getEnemies();
		if (enemies == null) {
			return;
		}	
		
		Bot target = null;
		// Init or assign current target
		for (Bot bot : enemies) {
			if(!bot.isDead()) {
				target = bot;
				break;
			}
		}
		
		System.out.println("TARGET " + target + " " + target.isDead());
		
		// Find bot with the most aggro
		for(Bot bot : robot.getEnemies()) {
			if(bot.isDead()) {
				continue;
			}
			if(!bot.isDead()) {
				if(bot.getAggro() > target.getAggro()) {
					target = bot;
				}
			}
		}
		
		System.out.println("AGGRO TARGET: " + target);
		
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
		
		robot.setTarget(target);			
			
	}	
	
	
	@Override
	public void onCollision(BaseBot robot, HitRobotEvent event) {
		// Make the robot that just rammed into as, our new target
		//System.out.println(robot.getTarget().getName() + " " + event.getName());
		if (!robot.getTarget().getName().equals(event.getName())) {
			System.out.println("Who rammed us?");
			//robot.setState(State.Scanning);
			robot.setTarget(FuncLib.findBotByName(event.getName(), robot.getEnemies()));
		}
	}
	
	@Override
	public void onHitByBullet(BaseBot robot, HitByBulletEvent event) {
		// Find the enemy with the most aggro
		Bot target = robot.getTarget();
		for(Bot bot : robot.getEnemies()) {
			if(bot.isDead()) {
				continue;
			}
			if(!bot.isDead()) {
				if(bot.getAggro() > target.getAggro()) {
					target = bot;
				}
			}
		}		
		
		if(!robot.getTarget().getName().equals(target.getName())) {			
			robot.setTarget(target);
			setSkipTargeting(true);
			System.out.println("New Target: " + target.getName() + "Aggro: " + target.getAggro());
		}		
	}
	
	
	@Override
	public String toString() {		
		return super.toString() + "Choose Closest";
	}

}
