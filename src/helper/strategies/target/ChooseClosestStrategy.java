package helper.strategies.target;

import helper.Bot;
import helper.Enums.State;

import java.util.ArrayList;

import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robots.BaseBot;

public class ChooseClosestStrategy extends TargetStrategy {
	
	

	@Override
	public void execute(BaseBot robot) {
		
		// Find new target
		if(robot.getTarget() == null || !skipTargeting || robot.getTarget().isDead()) {
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
			robot.setTarget(target);
			return;
		}
		
		
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
		// TODO Auto-generated method stub
		super.onHitByBullet(robot, event);
	}
	
	@Override
	public String toString() {		
		return super.toString() + "Choose Closest";
	}

}
