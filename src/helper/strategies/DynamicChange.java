package helper.strategies;

import helper.Data;
import helper.FuncLib;
import robots.BaseBot;

public class DynamicChange extends GunStrategy {
	
	// Strtegies
	private LinTargeting linTargeting = new LinTargeting();
	private GuessTargeting guessTargeting = new GuessTargeting();
	private GunStrategy strategy = linTargeting;
	
	// Statistics	
	private double acc = 100;
	private double formerAcc = 100;
	
	// Timer
	private int count = 0;
	private int interval = 10;
	
	@Override
	public boolean execute(BaseBot robot) {
		boolean fired;
		// Check if we have reliable data about our target
		Data data = FuncLib.findDataByName(robot.getTarget().getName(), robot.getDataList());
		if(data.isReliable()) {			
			if(data.getGuessAccuracy() > data.getLinAccuracy()) {
				fired = guessTargeting.execute(robot);
			} else {
				fired = linTargeting.execute(robot);
			}
		} else {			
			// Change targeting periodically
			if(fired = strategy.execute(robot)) {
				count++;
			}
			
			// Check if we need to change fire mode
			if(count >= interval) {
				formerAcc = acc;
				acc = robot.getHits() / (robot.getHits() + robot.getMisses()) * 100.0;

				System.out.println("Current Accuracy " + acc);

				robot.setHits(0);
				robot.setMisses(0);
				count = 0;
				
				if(acc > formerAcc) {
					// Don't change; current fire mode is better
					return fired;
				}
				
				if(strategy instanceof LinTargeting) {					
					strategy = guessTargeting;
				} else {
					System.out.println("Switch to lin");
					strategy = linTargeting;
				}				
			}
		}		
		return fired;
	}
	
	@Override
	public double getAccuracy(BaseBot robot) {		
		return acc;
	}
	
	public GunStrategy getCurrentFireStrategy() {
		return strategy;
	}
	
	public GuessTargeting getGuessTargeting() {
		return guessTargeting;
	}
	
	public String toString(){
		return "DynamicChange Targeting with " + guessTargeting + " and " + linTargeting;		
	}
}