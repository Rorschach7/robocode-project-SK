package helper.strategies.gun;

import helper.Enums.State;
import helper.strategies.movement.MovementStrategy;
import robocode.ScannedRobotEvent;
import robots.BaseBot;

public abstract class GunStrategy {
	
	private int friendlyFireThreshold = 5;
	protected int friendlyFireCount;
	protected MovementStrategy repositionStrategy;
	
	/**
	 * Override this method to with the code you want to execute when the robot is meant to shoot.
	 * @param robot the robot on which this method will be executed
	 * @return true if the gun was fired, false if not
	 */
	public abstract boolean execute(BaseBot robot);
	
	public double getAccuracy(BaseBot robot) {
		double acc = robot.getHits() / (robot.getHits() + robot.getMisses()) * 100.0;
		return acc;
	}
	
	/**
	 * This function will be called in the robot's onScannedRobotEvent method. 
	 * @param robot
	 * @param e
	 */
	public void collectData(BaseBot robot, ScannedRobotEvent e) {
		
	}
	
	public void addToFriendlyFire(BaseBot robot) {
		friendlyFireCount++;
		if(friendlyFireCount >= friendlyFireThreshold) {
			friendlyFireCount = 0;
			if(repositionStrategy != null) {
				robot.setState(State.Evading);
			}
		}
	}

	public int getFriendlyFireThreshold() {
		return friendlyFireThreshold;
	}

	public void setFriendlyFireThreshold(int friendlyFireThreshold) {
		this.friendlyFireThreshold = friendlyFireThreshold;
	}
	
	

}