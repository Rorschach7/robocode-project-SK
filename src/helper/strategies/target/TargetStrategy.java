package helper.strategies.target;

import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robots.BaseBot;

public abstract class TargetStrategy {
	
	/**
	 * Is set to true, if the robot is supposed to find and attack a specific robot.
	 */
	protected boolean skipTargeting;
	
	/**
	 * Use robot.setTarget() to communicate your target choice to the robot.
	 * @param bot
	 */
	public abstract void execute(BaseBot robot);	
	
	/**
	 * Is called if a robot collides with us.
	 * @param robot
	 * @param event
	 */
	public void onCollision(BaseBot robot, HitRobotEvent event) {
		
	}
	
	/**
	 * Called if we get hit by a bullet.
	 * @param robot
	 * @param event
	 */
	public void onHitByBullet(BaseBot robot, HitByBulletEvent event) {
		
	}
	
	@Override
	public String toString() {		
		return "Target Strategy: ";
	}
	

}
