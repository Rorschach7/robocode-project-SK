package helper;

import helper.Enums.*;

public class Data {
	
	private String robotName;
	private int[][] stats = new int[13][31];
	
	private int linearTargetHits;
	private int linearTargetMissed;
	
	private int guessTargetingHits;
	private int guessTargetingMissed;
	
	double linAccuracy;
	double guessAccuracy;
	
	private int wins;
	private int losses;
	
	public Data(String name) {
		
		String robotName = name;
		
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
	public void BulletHit(boolean hit, FireMode fireMode) {
		
		if(fireMode == FireMode.LinearTargeting) {
			if(hit) {
				linearTargetHits++;
			} else {
				linearTargetMissed++;
			}
		}
		
		if(fireMode == FireMode.GuessFactor) {
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
		
	}
	
//	public void saveData() {
//		//System.out.println("Saving Data " + dataList.size());
//		
//		Gson gson = new Gson();
//					
//	}
	
	
}
