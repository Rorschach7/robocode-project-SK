package helper;

import helper.strategies.*;


public class Data {
	
	private String robotName;
	private int[][] stats = new int[13][31];
	
	private double linearTargetHits;
	private double linearTargetMissed;
	
	private double guessTargetingHits;
	private double guessTargetingMissed;
	
	private double linAccuracy;
	private double guessAccuracy;
	
	private int wins;
	private int losses;
	
	public Data(String name) {
		
		String robotName = name;
		guessAccuracy = 50;
		linAccuracy = 25;
		
		
		// Check 
		if(name.contains(" ")) {
			int i = name.indexOf(" ");
			robotName = name.substring(0, i);
			System.out.println("Multiple Instances: " + robotName);
		}	
		
		this.robotName = robotName;
	}	
	
	public String getRobotName() {
		return robotName;
	}
	
	/**
	 * Use this method to tell the Data class whether the shot hit or missed the target and what fire mode was used
	 * @param hit Did the bullet hit the target?
	 * @param fireMode The fire mode used to fire the bullet
	 */
	public void BulletHit(boolean hit, GunStrategy strat) {
		GunStrategy strategy = strat;
		if(strategy instanceof DynamicChange) {
			strategy = ((DynamicChange) strat).getCurrentFireStrategy();			
		}
		
		if(strategy instanceof LinTargeting) {
			if(hit) {
				linearTargetHits++;
			} else {
				linearTargetMissed++;
			}
		}
		
		if(strategy instanceof GuessTargeting) {
			if(hit) {
				guessTargetingHits++;
			} else {
				guessTargetingMissed++;
			}
		}	
		
		// Calculate Accuracy
		if(linearTargetHits + linearTargetMissed != 0) {
			linAccuracy = linearTargetHits / (linearTargetHits + linearTargetMissed) * 100; 
		}
		
		if(guessTargetingHits + guessTargetingMissed != 0) {
			guessAccuracy = guessTargetingHits / (guessTargetingHits + guessTargetingMissed) * 100; 
		}
		
	}
	
	public void printData(boolean waveData) {
		System.out.println("--- Start ---");
		System.out.println("Robot Name: " + robotName);
		System.out.println("Wins: " + wins);
		System.out.println("Losses: " + losses);
		System.out.println("Linear Targeting: ");
		System.out.println("Hits: " + linearTargetHits);
		System.out.println("Missed: " + linearTargetMissed);
		System.out.println("Accuracy: " + linAccuracy);
		System.out.println("GuessFactorTargeting:");
		System.out.println("Hits: " + guessTargetingHits);
		System.out.println("Missed: " + guessTargetingMissed);
		System.out.println("Accuracy: " + guessAccuracy);
		System.out.println("---- End ----");
		
		if(!waveData) {
			return;
		}		
		System.out.println("Guess Targeting Data:");
		System.out.println("---------------------------------------------------");		
		for(int i = 0; i < 13; i++) {
			for(int j = 0; j < 31; j++) {
				System.out.print(stats[i][j] + " ");
			}
			System.out.println();
		}
		System.out.println("---------------------------------------------------");
	}
	
	public void win() {
		wins++;
	}
	
	public void lost() {
		losses++;
	}
	
	public String toString() {
		return robotName + ".json";
	}
	
	public int[][] getStats() {
		return stats;
	}
	
	public double getGuessAccuracy() {
		return guessAccuracy;
	}
	
	public double getLinAccuracy() {
		return linAccuracy;
	}
	
	public double getGuessTargetingHits() {
		return guessTargetingHits;
	}
	
	public double getGuessTargetingMissed() {
		return guessTargetingMissed;
	}
	
	public boolean isReliable() {
		double linSum = linearTargetHits + linearTargetMissed;
		double guessSum = guessTargetingHits + guessTargetingMissed;
		
		if(linSum < 100 || guessSum < 100) {
			return false;
		} else {
			return true;
		}		
	}
	
}
