package helper.strategies.movement;

import helper.Bot;
import helper.FuncLib;
import helper.GravPoint;
import java.util.ArrayList;
import java.util.Collections;
import robots.BaseBot;

public class AntiGravity extends MovementStrategy {

	private int midpointcount = 0; // Number of turns since that strength was
	private double midpointstrength = 0; // The strength of the gravity point in

	@Override
	public void execute(BaseBot robot) {

		double xforce = 0;
		double yforce = 0;
		double force;
		double ang;
		GravPoint p;
		ArrayList<Bot> allBots = new ArrayList<Bot>();
//		Collections.copy(allBots, robot.getEnemies());
		for (Bot bot : robot.getTeam()) {
			if (!bot.equals(this)) {
				allBots.add(bot);
			}
		}
		for (Bot bot : robot.getEnemies()) {
			if (!bot.equals(this)) {
				allBots.add(bot);
			}
		}
		// cycle through all bots. If they are alive, they are repulsive.
		// Calculate the force on us

		for (Bot bot : allBots) {
			if (!bot.isDead()) {

				double angleToBot = bot.getInfo().getBearing();

				// Calculate the angle to the scanned robot
				double angle = Math
						.toRadians((robot.getHeading() + angleToBot % 360));

				// Calculate the coordinates of the robot
				double botX = (robot.getX() + Math.sin(angle)
						* bot.getInfo().getDistance());
				double botY = (robot.getY() + Math.cos(angle)
						* bot.getInfo().getDistance());

				p = new GravPoint(botX, botY, -500);

				force = p.power
						/ Math.pow(FuncLib.getRange(robot.getX(), robot.getY(),
								p.x, p.y), 2);

				// Find the bearing from the point to us
				ang = FuncLib.normaliseBearing(Math.PI / 2
						- Math.atan2(robot.getY() - p.y, robot.getX() - p.x));
				// Add the components of this force to the total force in their
				// respective directions
				xforce += Math.sin(ang) * force;
				yforce += Math.cos(ang) * force;
			}
		}

		/**
		 * The next section adds a middle point with a random (positive or
		 * negative) strength. The strength changes every 5 turns, and goes
		 * between -1000 and 1000. This gives a better overall movement.
		 **/

		midpointcount++;
		if (midpointcount > 5) {
			midpointcount = 0;
			midpointstrength = (Math.random() * 1000) - 500;
		}
		p = new GravPoint(robot.getBattleFieldWidth() / 2,
				robot.getBattleFieldHeight() / 2, midpointstrength);
		force = p.power
				/ Math.pow(
						FuncLib.getRange(robot.getX(), robot.getY(), p.x, p.y),
						1.5);
		ang = FuncLib.normaliseBearing(Math.PI / 2
				- Math.atan2(robot.getY() - p.y, robot.getX() - p.x));
		xforce += Math.sin(ang) * force;
		yforce += Math.cos(ang) * force;

		/**
		 * The following four lines add wall avoidance. They will only affect us
		 * if the bot is close to the walls due to the force from the walls
		 * decreasing at a power 3.
		 **/
		xforce += 5000 / Math
				.pow(FuncLib.getRange(
						robot.getX(),
						robot.getY(),
						robot.getBattleFieldWidth() - 40
								- robot.getSentryBorderSize(), robot.getY()), 3);
		xforce -= 5000 / Math.pow(
				FuncLib.getRange(robot.getX(), robot.getY(),
						20 + robot.getSentryBorderSize(), robot.getY()), 3);
		yforce += 5000 / Math.pow(
				FuncLib.getRange(
						robot.getX(),
						robot.getY(),
						robot.getX(),
						robot.getBattleFieldHeight() - 40
								- robot.getSentryBorderSize()), 3);
		yforce -= 5000 / Math.pow(FuncLib.getRange(robot.getX(), robot.getY(),
				robot.getX(), 20 + robot.getSentryBorderSize()), 3);

		// Move in the direction of our resolved force.
		robot.goTo(robot.getX() - xforce, robot.getY() - yforce);

	}

}
