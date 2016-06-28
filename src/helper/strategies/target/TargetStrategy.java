package helper.strategies.target;

import helper.Bot;
import robots.BaseBot;

public abstract class TargetStrategy {
	
	/**
	 * Use robot.setTarget() to communicate your target choice to the robot.
	 * @param bot
	 */
	public abstract Bot execute(BaseBot robot);	
	
	@Override
	public String toString() {		
		return "Target Strategy: ";
	}
	

}
