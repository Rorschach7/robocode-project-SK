package helper.strategies;

import robocode.ScannedRobotEvent;
import robots.BaseBot;

public abstract class MovementStrategy {
	
	public abstract void execute(BaseBot robot);
	
	/**
	 * This function will be called in the robot's onScannedRobotEvent method. 
	 * @param robot
	 * @param e
	 */
	public void collectData(BaseBot robot, ScannedRobotEvent e) {
		
	}

}
