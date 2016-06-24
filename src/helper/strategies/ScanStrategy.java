package helper.strategies;

import robots.BaseBot;

public abstract class ScanStrategy {
	
	/**
	 * Override this function with your specific scan code
	 * @param robot
	 */
	public abstract void execute(BaseBot robot);

}
