package helper.strategies.movement;

import helper.Enums.State;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Random;

import robocode.ScannedRobotEvent;
import robots.BaseBot;

public class RandomMovement extends MovementStrategy {
	
	private boolean isEvading;
	private Point randPoint; // use Point2D instead?
	private double evadeRounds;
	
	@Override
	public void execute(BaseBot robot) {		
		if (isEvading) {
			System.out.println("evading");
			robot.goTo(randPoint.getX(), randPoint.getY());
			evadeRounds--;
			// check if we reached desired position
			if (new Point((int) robot.getX(), (int) robot.getY()).distance(randPoint) < 10
					|| evadeRounds < 0) {
				isEvading = false;
				robot.setState(State.Attacking);
				System.out.println("reached point");
			}
		} else {
			System.out.println("Start randomMovement");
			randPoint = calculateRandomPoint(robot);
			isEvading = true;
		}
	}
	
	/**
	 * Calculates a radnom point to avoid incoming bullet
	 * @param robot
	 * @return
	 */
	private Point calculateRandomPoint(BaseBot robot) {
		ScannedRobotEvent bot = robot.getTarget().getInfo();
		double botBearing = bot.getBearing();
		double heading = robot.getHeading();
		Rectangle field = new Rectangle(new Point(16, 16), new Dimension(
				(int) robot.getBattleFieldWidth() - 32,
				(int) robot.getBattleFieldHeight() - 32));

		// TODO: change deltaAngle according to the distance to the enemy
		double deltaAngle = 100;

		Random rand = new Random();
		Point target = new Point();
		double randAngle;

		// Random velocity between 5 and 8
		double velo = 5 + new Random().nextDouble() * 3;
		robot.setMaxVelocity(velo);

		double turnsTillBulletHits = bot.getDistance() / robot.getBulletVelocity();

		// to stop rand movement when the bullet passed or
		evadeRounds = turnsTillBulletHits * 2 / 3;

		// distance to move to dodge bullet
		double dist = turnsTillBulletHits * 2 / 3 * velo;

		// gives a random angle which is not to the enemy or the opposite
		// direction
		do {
			randAngle = rand.nextDouble() * 360;
			double randAngTotal = (randAngle + heading + botBearing - deltaAngle / 2) % 360;
			if (randAngTotal < 0) {
				randAngTotal += 360;
			}
			double tx = robot.getX() + dist * Math.sin(Math.toRadians(randAngTotal));
			double ty = robot.getY() + dist * Math.cos(Math.toRadians(randAngTotal));
			target.setLocation(tx, ty);
		} while (randAngle > 0 && randAngle < deltaAngle || randAngle > 180
				&& randAngle < 180 + deltaAngle || !field.contains(target));

		// System.out.println("pos: " + getX() + " " + getY());
		// System.out.println("ang: " + randAngTotal);
		// System.out.println("dist: " + dist);
		// System.out.println("target: " + target);

		return target;
	}

}
