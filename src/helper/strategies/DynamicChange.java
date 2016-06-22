package helper.strategies;

import helper.Data;
import robots.TestBot;

public class DynamicChange extends GunStrategy {
	
	// Strtegies
	private LinTargeting linTargeting = new LinTargeting();
	private GuessTargeting guessTargeting = new GuessTargeting();
	private GunStrategy strategy = linTargeting;
	
	// Statistics
	private double hits = 0;
	private double misses = 0;	
	private double acc = 100;
	private double formerAcc = 100;
	
	// Timer
	private int count = 0;
	private int interval = 10;
	
	@Override
	public boolean execute(TestBot robot) {
		boolean fired;
		// Check if we have reliable data about our target
		Data data = robot.findDataByName(robot.getTarget().getName());
		if(data.isReliable()) {
			System.out.println("Data is reliable");
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
				acc = hits / (hits + misses) * 100.0;

				System.out.println("Current Accuracy " + acc);

				hits = 0;
				misses = 0;
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
	
	public void hit() {
		hits++;
	}
	
	public void miss() {
		misses++;
	}

}
