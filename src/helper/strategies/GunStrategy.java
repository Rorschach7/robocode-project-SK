package helper.strategies;

import robocode.ScannedRobotEvent;
import robots.BaseBot;

public abstract class GunStrategy {
	
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

}
