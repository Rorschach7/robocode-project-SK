package robots;

import robocode.*;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * Sparrow 2.5 - a robot by Kwok-Cheung Li A melee robot written to participate
 * in Robotobe's MiniBot Challenge http://tobe.homelinux.net/robocode/ Sparrow
 * is a Micro-class Robot (codesize less than 750)
 * 
 * Features: - (a slight variant on) Anti-gravity movement - Sparrow (usually)
 * doesn't need full 360 degrees radarsweeps to scan for enemies, even with
 * multiple enemies in melee - Random linear aim - Current codesize : 749
 * 
 * 
 * VERSION HISTORY
 * 
 * (2.5) - Tries to avoid getting hit by bots that aim straight by Adding
 * anti-gravity sources at spots previously occopied by Sparrow, at random
 * intervals. - Took a lot of effort keeping codesize under 750
 * 
 * (2.4) - Some tweaks in the wall-gravity - Tries to move perpendicular to as
 * many bots as possible instead of just the current target
 * 
 * (2.3) - Some tweaking in gravity movement - added some more comments -
 * Codesize reduced to 666 Bytes!
 * 
 * (2.24) - Some fooling around with the movements, making Sparrow a bit more
 * agressive with less bots in the battlefield. - Current codesize : 736
 * 
 * (2.21) - Some changes in firepower - Cleaned up the code - Current codesize :
 * 633
 * 
 * (2.2) - Some tweaks here and there...
 * 
 * (2.19) - It appears that setTurnRadarRightRadians( large-enough-number );
 * Gives the same results as manually turning setAdjustRadarXXX on and off...
 * codesize reduced to 722 - Did away with the circular predictor... I wasn't
 * hitting anyone with that thing anyway. It's been replaced by a random-aim
 * until I can come up with something better ;-)
 * 
 * (2.16) - Reduced Codesize - My favorite feature returns! Extra Wide Scanning!
 * Thanks GrayGoo! :-)
 * 
 * (2.15) - Just some minor tweaks in firepower and antigravity
 * 
 * (2.14) - Added some random-movements (...of sorts :p) for 1-on-1 situations -
 * spent quite some time to get the codesize beneath 750 (while keeping its
 * colours! :) )
 * 
 * (2.1) - Replaced the previous predictor with a linear/circular predictor! -
 * had to ditch the Meerkat-behaviour to fit the new aiming in - had to drop the
 * extra-wide scanning feature as well :-(
 * 
 * 
 * (2.07) - Using less firepower at greater distance didn't seem to do Sparrow's
 * performance much good... back to setFire(3) only.. :-( - Reduction of
 * codesize (from 725 back to 679!) - So yeah, this is actually a downgrade back
 * to 2.05...only smaller in codesize...
 * 
 * 
 * (2.06) - Conserves more energy if enemy is further away - Reduction of
 * codesize
 * 
 * (2.05) - Some minor tweaks in the aiming and anti-gravity Bots moving
 * straight away do not repel anymore, and bots moving in repel with twice the
 * force of a non-moving bot. - Reduction of codesize
 * 
 * (2.04) - Version History in SourceCode (WHEE! :-)) - antigravity force now
 * also dependent on whether a bot is moving away from sparrow, or closing in. -
 * Reduction of codesize : Replaced some if-statements in my angle_90 method
 * with Math.atan(Math.tan(..)) - Added Colors (meant to do this for a long
 * time...I just kept forgetting :-p)
 * 
 * (2.0) - Added Meerkat's behaviour for getOthers()<3 situations - altered
 * antigravity... the strength of the force from other bots is now
 * 1/getDistance() instead if 1/(getDistance()*getDistance()) - Reduction of
 * codesize
 *
 * (1.41) - Small tweaks - Reduction of codesize
 *
 * (1.4) - Added GrayGoo's extra-wide scanning idea - Reduction of codesize
 *
 * (1.3) - Removed nasty glitches where some bots are scanned twice, and another
 * not scanned at all - some tweaking on Anti-gravity
 *
 * (1.23) - more conservative fireing when energy drops below 30
 * 
 * (1.22) - Tweaked one-on-one strategy
 * 
 * (1.21) - Reduction of codesize : The extra anti-grav source in the middle of
 * the battlefield seemed to make things worse, so I removed it
 * 
 * (1.2) - Added Woodpecker's gun
 * 
 * (1.1) - Reduction of codesize : Replaced a method that was quite large in
 * codesize with Math.atan2( .. ) - Added a one-on-one strategy (worth 2 lines
 * of code... some strategy, eh? ;-) )
 * 
 * (1.0) - Anti-gravity Movement - Aims straight at closest target
 * 
 *
 * 
 * CREDITS: - Robotobe, I believe he was the first to implement anti-gravity in
 * his bot Relativity. (I remember reading that somewhere...I could be wrong,
 * though.) - Robocode community for ideas on reducing codesize. The topic on
 * reducing codesize can be found here:
 * http://robocoderepository.com/jive/thread.jsp?forum=31&thread=512
 * 
 */

public class Sparrow_notOurs extends AdvancedRobot {
	static double x = 0;
	static double y = 0;
	static double radarturn = 1;
	static double heading;
	static int accountedfor;
	static String target;
	double closest = Double.POSITIVE_INFINITY;
	static ArrayList enemies = new ArrayList();
	static double bp;
	static double stime;
	static double timer;
	static double pX, pX1;
	static double pY, pY1;
	static double grav;

	public static double angle_180(double ang) {
		return Math.atan2(Math.sin(ang), Math.cos(ang));
	}

	public void addGravity(double force, double angle) {
		x += force * Math.sin(angle);
		y += force * Math.cos(angle);
	}

	public double wallGrav(double d) {
		return (1.0 / d) * 20.0 / Math.max((d - 40), 20);
	}

	public void run() {
		timer = 0;
		accountedfor = 1000; // to make sure my static arraylist gets cleared.
								// the arraylist was made static to save
								// bytecode.
		setColors(new Color(6567695), Color.black, Color.yellow);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		do {
			setTurnRadarRightRadians(radarturn);
			double robotturn = angle_180(heading - getHeadingRadians());

			// Make sure the robot's turn angle is between +/- 90 degrees
			int forward = 60;
			double t = Math.atan(Math.tan(robotturn));
			if (Math.abs(robotturn - t) > 1)
				forward = -60;
			setAhead(forward);
			//

			setTurnRightRadians(t);
			if (Math.abs(getGunTurnRemaining()) < 10 && getOthers() > 0)
				setFire(bp);
			execute();
		} while (true);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		double myX = getX();
		double myY = getY();
		double time = getTime();
		String enemyName = e.getName();
		double enemyDist = e.getDistance();

		// try not to stick at a spot for too long
		if (time > timer) {
			pX = pX1;
			pY = pY1;
			pX1 = myX;
			pY1 = myY;
			grav = 0;
			// timer += 16 + (Math.random() * 64);
		}
		if (time - timer > 12) {
			timer += 20 + (Math.random() * 48);
			grav = 1;
		}
		//

		double absbearing = e.getBearingRadians() + getHeadingRadians();
		double angle = e.getHeadingRadians() - absbearing;
		if (!enemies.contains(enemyName)) {
			enemies.add(enemyName);
			accountedfor++;
			// Mind the Minus sign!
			// addGravity((-(1 -
			// (1.0/24.0)*e.getVelocity()*Math.cos(angle))/enemyDist),
			// absbearing);
			addGravity(-1 / enemyDist, absbearing);
			//

			if (getOthers() < 2) {
				addGravity(.0025, absbearing);
			}

		}

		if ((accountedfor >= getOthers()) || (time > stime)) {
			stime = time + 9;

			// try not to stick around in one spot for too long
			angle = Math.atan2(myX - pX, myY - pY);
			addGravity(
					.3 * grav
							/ Math.max(Point2D.distance(myX, myY, pX, pY), 10),
					angle);
			//

			addGravity((.004) * Math.sin(.09 * time),
					(Math.atan2(x, y) + .5 * Math.PI)); // try to move
														// perpendicular
														// to as many bots as
														// possible

			// wall gravity:
			// double rw=(getBattleFieldWidth()-myX);
			// double uw=(getBattleFieldHeight()-myY);
			x += wallGrav(myX) - wallGrav(getBattleFieldWidth() - myX);
			y += wallGrav(myY) - wallGrav(getBattleFieldHeight() - myY);
			//

			// Evaluate new heading:
			heading = Math.atan2(x, y);
			//

			// Reset variables and have the radar turn the opposite way:
			accountedfor = 0;
			enemies.clear();
			x = 0;
			y = 0;
			radarturn = -radarturn;
			//
		}

		if ((enemyName.equals(target)) || (enemyDist - closest < -100)) {
			target = enemyName;
			closest = enemyDist;
			bp = Math.min(Math.min(3, getEnergy() / 10), e.getEnergy() / 4);

			double gr = 1.3 * Math.random() - .3;
			double aim = Math.asin(gr * e.getVelocity() / (20 - 3 * bp)
					* Math.sin(e.getHeadingRadians() - absbearing));

			setTurnGunRightRadians(angle_180(aim + absbearing
					- getGunHeadingRadians()));

		}
	}

	public void onRobotDeath(RobotDeathEvent e) {
		if (e.getName().equals(target)) {
			closest = Double.POSITIVE_INFINITY;
		}
	}
}
