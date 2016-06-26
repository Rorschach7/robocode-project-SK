package helper.strategies.movement;

import robocode.ScannedRobotEvent;
import robots.BaseBot;

public abstract class MovementStrategy {
	
	/**
	 * Override this function with your specific movement code
	 * @param robot
	 */
	public abstract void execute(BaseBot robot);
	
	/**
	 * This function will be called in the robot's onScannedRobotEvent method. 
	 * @param robot
	 * @param e
	 */
	public void collectData(BaseBot robot, ScannedRobotEvent e) {
		
	}
	
	@Override
	public String toString() {		
		return "Movement Strategy: ";
	}
}