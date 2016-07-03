package helper.strategies.target;

import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robots.BaseBot;

public abstract class TargetStrategy {
	
	/**
	 * If set to true, the robot is supposed to find and attack a specific robot.
	 */
	private boolean skipTargeting;
	
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
	
	/**
	 * @return the skipTargeting
	 */
	public boolean isSkipTargeting() {
		return skipTargeting;
	}

	/**
	 * @param skipTargeting the skipTargeting to set
	 */
	public void setSkipTargeting(boolean skipTargeting) {
		this.skipTargeting = skipTargeting;
	}

	@Override
	public String toString() {		
		return "Target Strategy: ";
	}
	

}
