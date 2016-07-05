package helper;

import helper.strategies.gun.DynamicChange;
import helper.strategies.gun.GuessTargeting;
import helper.strategies.gun.GunStrategy;
import helper.strategies.gun.LinTargeting;
import helper.strategies.movement.AntiGravity;
import helper.strategies.movement.DynamicMovementChange;
import helper.strategies.movement.MovementStrategy;
import helper.strategies.movement.RandomMovement;
import helper.strategies.movement.SingleWaveSurfing;


public class Data {
	
	private String robotName;
	private int[][] stats = new int[13][31];
	
	private double linearTargetHits;
	private double linearTargetMissed;
	
	private double guessTargetingHits;
	private double guessTargetingMissed;
	
	private double linAccuracy;
	private double guessAccuracy;
	
	private double detectedBulletsRandom;
	private double hitBulletsRandom;
	
	private double detectedBulletsSurfing;
	private double hitBulletsSurfing;
	
	private double prevHitBullets;
	
	private double surfingSuccRate;
	private double randomSuccRate;
	
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
	public void bulletHit(boolean hit, GunStrategy strat) {
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
	
/**
 * Use this method to tell the Data class that the robot detected a shot, how many bullets hit him
 * already and what movement strategy is used
 * @param strats which movement strategy is used?
 * @param enemyBulletsHit how many bullets hit us already?
 */
	public void detectedBullet(MovementStrategy strats, double enemyBulletsHit){
		MovementStrategy strategy = strats;
		if(strategy instanceof DynamicMovementChange){
			strategy = ((DynamicMovementChange)strats).getCurrentMovementStrategy();
		}
		
		if(strategy instanceof RandomMovement){
			hitBulletsRandom += enemyBulletsHit - prevHitBullets;		
			detectedBulletsRandom++;
		}
		
		if(strategy instanceof AntiGravity){
			//TODO			
		}
		
		if(strategy instanceof SingleWaveSurfing){
		   hitBulletsSurfing += enemyBulletsHit - prevHitBullets;
		   detectedBulletsSurfing++;
		}
		
		
		//calculate success rate
		if(detectedBulletsRandom + hitBulletsRandom != 0){
			randomSuccRate = (detectedBulletsRandom - hitBulletsRandom) / detectedBulletsRandom * 100;
		}
		
		if(detectedBulletsSurfing + hitBulletsSurfing != 0){
			surfingSuccRate = (detectedBulletsSurfing - hitBulletsSurfing) / detectedBulletsSurfing * 100;
		}
		
		prevHitBullets = enemyBulletsHit;
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
		System.out.println("RandomMovement:");
		System.out.println("Detected Bullets: " + detectedBulletsRandom);
		System.out.println("Enemy Bullets Hit: " + hitBulletsRandom);
		System.out.println("Success Rate: " + randomSuccRate);
		System.out.println("SingleWaveSurfing:");
		System.out.println("Detected Bullets: " + detectedBulletsSurfing);
		System.out.println("Enemy Bullets Hit: " + hitBulletsSurfing);
		System.out.println("Success Rate: " + surfingSuccRate);		
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
	
	public double getSurfingSuccRate() {
		return surfingSuccRate;
	}

	public void setSurfingSuccRate(double surfingSuccRate) {
		this.surfingSuccRate = surfingSuccRate;
	}

	public double getRandomSuccRate() {
		return randomSuccRate;
	}

	public void setRandomSuccRate(double randomSuccRate) {
		this.randomSuccRate = randomSuccRate;
	}

	public boolean movementIsReliable(){
		if(detectedBulletsRandom < 100 || detectedBulletsSurfing < 100){
			return false;
		}else{
			return true;
		}		
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