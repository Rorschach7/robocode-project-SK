package helper.strategies.target;

import helper.Bot;
import helper.Enums.State;

import java.util.ArrayList;

import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robots.BaseBot;

public class ChooseAggroStrategy extends TargetStrategy {
	
	

	@Override
	public void execute(BaseBot robot) {
		
		// Find new target
		if(robot.getTarget() == null || !isSkipTargeting() || robot.getTarget().isDead()) {
			// Assuming we just finished a full scan.
			// Find a target among all enemies
			ArrayList<Bot> enemies = robot.getEnemies();
			if (enemies == null) {
				return;
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
			
			
			
			// Find bot with the most aggro
			target = mostAggro(robot);
			
			if(target.getAggro() < 1.0) {	
				// Aggro is not convinving
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
		// Reset
		setSkipTargeting(false);
	}	
	
	
	@Override
	public void onCollision(BaseBot robot, HitRobotEvent event) {
		// Make the robot that just rammed into as, our new target
		//System.out.println(robot.getTarget().getName() + " " + event.getName());
		if (!robot.getTarget().getName().equals(event.getName())) {
			System.out.println("Who rammed us?");
			robot.setState(State.Scanning);
		}
	}
	
	@Override
	public void onHitByBullet(BaseBot robot, HitByBulletEvent event) {
		// Find the enemy with the most aggro		
		Bot newTarget = mostAggro(robot);		
		
		if(!robot.getTarget().getName().equals(newTarget.getName())) {			
			robot.setTarget(newTarget);
			setSkipTargeting(true);
			System.out.println("New Target: " + newTarget.getName() + "Aggro: " + newTarget.getAggro());
		}		
	}
	
	private Bot mostAggro(BaseBot robot) {
		Bot newTarget = robot.getTarget();
		for(Bot bot : robot.getEnemies()) {
			if(!bot.isDead()) {
				if(bot.getAggro() > newTarget.getAggro()) {
					newTarget = bot;
				}
			}
		}
		return newTarget;
	}
	
	@Override
	public String toString() {		
		return super.toString() + "Choose Closest";
	}

}
