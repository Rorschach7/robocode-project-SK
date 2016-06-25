package helper.strategies.radar;

import helper.Enums.State;
import robocode.ScannedRobotEvent;
import robots.BaseBot;

public abstract class RadarStrategy {
	
	/**
	 * This function is called in the onScannedRobotEvent method.
	 * Can be used to keep track of robots.
	 * Most scanning strategies only need that much.
	 * @param robot
	 */
	public abstract boolean execute(BaseBot robot, ScannedRobotEvent e);
	
	/**
	 * 
	 * @param robot
	 * @return
	 */
	public boolean execute(BaseBot robot) {
		return false;
	}
	
	/**
	 * Override this function with your specific scan code. 
	 * The BaseBot calls this function every round, if it is in its attacking state.
	 * If you intend to use different scanning methods, this is where you manage them.
	 * @param robot
	 * @return returns true if scan is completed, otherwise false.
	 */
	public void attackingScan(BaseBot robot) {
		
	}
	
	/**
	 * The BaseBot calls this function at the start of each match. 
	 * IMPORTANT: This function will be called until the bot's state is set to attacking.
	 * Use super call to change BaseBot's state to attacking.
	 * @param robot
	 */
	public void scanningScan(BaseBot robot) {
		robot.setState(State.Attacking);
	}
	
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Scan Strategy: ";
	}

}