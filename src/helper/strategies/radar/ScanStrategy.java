package helper.strategies.radar;

import helper.Enums.State;
import robocode.ScannedRobotEvent;
import robots.BaseBot;

public abstract class ScanStrategy {
	
	/**
	 * Override this function with your specific scan code. 
	 * The BaseBot calls this function every round, if it is in its attacking state.
	 * If you intend to use different scanning methods, this is where you manage them.
	 * @param robot
	 * @return returns true if scan is completed, otherwise false.
	 */
	public abstract boolean attackingScan(BaseBot robot);
	
	/**
	 * The BaseBot calls this function at the start of each match. 
	 * IMPORTANT: This function will be called until the bot's state is set to attacking.
	 * Use super call to change BaseBot's state to attacking.
	 * @param robot
	 */
	public void scanningScan(BaseBot robot) {
		robot.setState(State.Attacking);
	}
	
	/**
	 * This function is called in the onScannedRobotEvent method.
	 * Can be used to keep track of robots.
	 * @param robot
	 */
	public void updateScan(BaseBot robot, ScannedRobotEvent e) {
		
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Scan Strategy: ";
	}

}