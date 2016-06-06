package robots;

import java.util.ArrayList;
import java.util.List;

import helper.WaveBullet;
import robocode.*;
import robocode.util.Utils;

public class GuessTG extends AdvancedRobot {

	List<WaveBullet> waves = new ArrayList<WaveBullet>();
	int[][] stats = new int[13][31]; // onScannedRobot can scan up to 1200px, so
										// there are only 13.
	// Note: this must be odd number so we can get
	// GuessFactor 0 at middle.
	int direction = 1;

	public void run() {
		do {
			// ...
			// Turn the radar if we have no more turn, starts it if it stops and
			// at the start of round
			if (getRadarTurnRemaining() == 0.0)
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			execute();
		} while (true);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		// Absolute angle towards target
		double angleToEnemy = getHeadingRadians() + e.getBearingRadians();

		// Subtract current radar heading to get the turn required to face the
		// enemy, be sure it is normalized
		double radarTurn = Utils.normalRelativeAngle(angleToEnemy - getRadarHeadingRadians());

		// Distance we want to scan from middle of enemy to either side
		// The 36.0 is how many units from the center of the enemy robot it
		// scans.
		double extraTurn = Math.min(Math.atan(36.0 / e.getDistance()), Rules.RADAR_TURN_RATE_RADIANS);

		// Adjust the radar turn so it goes that much further in the direction
		// it is going to turn
		// Basically if we were going to turn it left, turn it even more left,
		// if right, turn more right.
		// This allows us to overshoot our enemy so that we get a good sweep
		// that will not slip.
		radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);

		// Turn the radar
		setTurnRadarRightRadians(radarTurn);

		
		
		// GUESS Targeting
		// Enemy absolute bearing, you can use your one if you already declare
		// it.
		double absBearing = getHeadingRadians() + e.getBearingRadians();

		// find our enemy's location:
		double ex = getX() + Math.sin(absBearing) * e.getDistance();
		double ey = getY() + Math.cos(absBearing) * e.getDistance();

		// Let's process the waves now:
		for (int i = 0; i < waves.size(); i++) {
			WaveBullet currentWave = (WaveBullet) waves.get(i);
			if (currentWave.checkHit(ex, ey, getTime())) {
				waves.remove(currentWave);
				i--;
			}
		}

		double power = Math.min(3, Math.max(.1, 17));
		// don't try to figure out the direction they're moving
		// they're not moving, just use the direction we had before
		if (e.getVelocity() != 0) {
			if (Math.sin(e.getHeadingRadians() - absBearing) * e.getVelocity() < 0)
				direction = -1;
			else
				direction = 1;
		}
		int[] currentStats = stats[(int) (e.getDistance() / 100)]; 
		
		// show something else later
		WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, power, direction, getTime(), currentStats);

		int bestindex = 15; // initialize it to be in the middle, guessfactor 0.
		for (int i = 0; i < 31; i++) {
			if (currentStats[bestindex] < currentStats[i]) {
				bestindex = i;				
			}			
		}
		
		System.out.println("CurrentStats: " + bestindex);
		for(int i = 0; i < 31; i++) {
			if(i == 15){
				System.out.print("|" + currentStats[i] + "| ");
				continue;
			}
			System.out.print(currentStats[i] + " " );				
		}
		System.out.println();		

		// this should do the opposite of the math in the WaveBullet:
		double guessfactor = (double) (bestindex - ((currentStats.length - 1) / 2)) / ((currentStats.length - 1) / 2);
		double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
		double gunAdjust = Utils.normalRelativeAngle(absBearing - getGunHeadingRadians() + angleOffset);
		
		System.out.println("Guess Factor: " + guessfactor);
		
		setTurnGunRightRadians(gunAdjust);
		
		 if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, e.getDistance()) && setFireBullet(power) != null) {
			 waves.add(newWave);
			 System.out.println("FIRE");
		 }
             

	}

}
