package helper.strategies.movement;

import helper.Bot;
import helper.Enums.State;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;
import robocode.ScannedRobotEvent;
import robots.BaseBot;

public class RandomMovement extends MovementStrategy {

	private boolean isEvading;
	private Point randPoint;
	private double evadeRounds;

	@Override
	public void execute(BaseBot robot) {
		if (isEvading) {
			robot.goTo(randPoint.getX(), randPoint.getY());
			evadeRounds--;
			// check if we reached desired position
			if (new Point((int) robot.getX(), (int) robot.getY())
					.distance(randPoint) < 10 || evadeRounds < 0) {
				isEvading = false;
				robot.setState(State.Attacking);
				// System.out.println("reached point");
			}
		} else {
			// System.out.println("Start randomMovement");
			randPoint = calculateRandomPoint(robot);
			isEvading = true;
		}
	}

	/**
	 * Calculates a random point to avoid incoming bullet
	 * 
	 * @param robot
	 * @return the random point
	 */
	private Point calculateRandomPoint(BaseBot robot) {
		ScannedRobotEvent bot = robot.getTarget().getInfo();
		double botBearing = bot.getBearing();
		double heading = robot.getHeading();
		Rectangle field = new Rectangle(
				new Point(20 + robot.getSentryBorderSize(),
						+robot.getSentryBorderSize()), new Dimension(
						(int) robot.getBattleFieldWidth() - 40
								- robot.getSentryBorderSize(),
						(int) robot.getBattleFieldHeight() - 40
								- robot.getSentryBorderSize()));

		double deltaAngle = 100;
		int teamBotArea = 60;

		ArrayList<Bot> allBots = new ArrayList<Bot>();

		for (Bot eBot : robot.getEnemies()) {
			allBots.add(eBot);
		}
		for (Bot tBot : robot.getTeam()) {
			allBots.add(tBot);
		}

		Random rand = new Random();
		Point target = new Point();
		double randAngle;

		// Random velocity between 5 and 8 and if its closer than 100 its 8
		double velo;
		if (bot.getDistance() < 100) {
			velo = 8;
		} else {
			velo = 5 + new Random().nextDouble() * 3;
		}
		robot.setMaxVelocity(velo);

		double turnsTillBulletHits = bot.getDistance()
				/ robot.getBulletVelocity();

		// to stop rand movement when the bullet passed
		evadeRounds = turnsTillBulletHits * 2 / 3;

		// distance to move to dodge bullet
		double dist = turnsTillBulletHits * 2 / 3 * velo;

		boolean closeToTeamMember = false;
		// gives a random angle which is not to the enemy or the opposite
		// direction
		int counter = 0;
		do {
			// to prevent infinity loop if bot is pushed in an edge;
			if (counter >= 100) {
				deltaAngle -= 10;
				teamBotArea -= 5;
				counter = 0;
			}

			randAngle = rand.nextDouble() * 360;

			double randAngTotal = (randAngle + heading + botBearing - deltaAngle / 2) % 360;
			if (randAngTotal < 0) {
				randAngTotal += 360;
			}

			// calculate the target coordinates to move according to the random
			// angle
			double tx = robot.getX() + dist
					* Math.sin(Math.toRadians(randAngTotal));
			double ty = robot.getY() + dist
					* Math.cos(Math.toRadians(randAngTotal));
			target.setLocation(tx, ty);

			// prevent to move into other bots
			for (Bot aBot : allBots) {
				if (!aBot.isDead()) {
					int teamBotX = (int) aBot.getPosX();
					int teamBotY = (int) aBot.getPosY();
					Rectangle botField = new Rectangle(new Point(teamBotX
							- teamBotArea, teamBotY - teamBotArea),
							new Dimension(teamBotX + teamBotArea, teamBotY
									+ teamBotArea));
					if (botField.contains(target)) {
						closeToTeamMember = true;
					} else {
						closeToTeamMember = false;
					}
				}
			}

			counter++;
		} while (randAngle > 0 && randAngle < deltaAngle || randAngle > 180
				&& randAngle < 180 + deltaAngle || !field.contains(target)
				|| closeToTeamMember);

		return target;
	}
}