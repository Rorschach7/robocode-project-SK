package helper.strategies.movement;

import helper.EnemyWave;
import helper.FuncLib;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import robots.BaseBot;

public class SingleWaveSurfing extends MovementStrategy {
	public static int BINS = 47;
	public static double _surfStats[] = new double[BINS]; // we'll use 47 bins
	public Point2D.Double _myLocation; // our bot's location
	public Point2D.Double _enemyLocation; // enemy bot's location

	public ArrayList<EnemyWave> _enemyWaves = new ArrayList<EnemyWave>();
	public ArrayList<Integer> _surfDirections = new ArrayList<Integer>();
	public ArrayList<Double> _surfAbsBearings = new ArrayList<Double>();
	private BaseBot robot;

	public Rectangle2D.Double _fieldRect;
	public static double WALL_STICK = 160;

	@Override
	public void execute(BaseBot robot) {
		if(_fieldRect == null) {
			_fieldRect = new java.awt.geom.Rectangle2D.Double(
					20 + robot.getSentryBorderSize(), 20 + robot.getSentryBorderSize(),
					760 - robot.getSentryBorderSize(),
					560 - robot.getSentryBorderSize());
		}
		robot.setMaxVelocity(8);
	}

	@Override
	public void collectData(BaseBot robot, ScannedRobotEvent e) {
		this.robot = robot;
		_myLocation = new Point2D.Double(robot.getX(), robot.getY());

		double lateralVelocity = robot.getVelocity()
				* Math.sin(e.getBearingRadians());
		double absBearing = e.getBearingRadians() + robot.getHeadingRadians();

		_surfDirections.add(0, new Integer((lateralVelocity >= 0) ? 1 : -1));
		_surfAbsBearings.add(0, new Double(absBearing + Math.PI));

		double bulletPower = robot.getBulletPower();
		if (bulletPower < 3.01 && bulletPower > 0.09
				&& _surfDirections.size() > 2) {
			EnemyWave ew = new EnemyWave();
			ew.setFireTime(robot.getTime() - 1);
			ew.setBulletVelocity(bulletVelocity(bulletPower));
			ew.setDistanceTraveled(bulletVelocity(bulletPower));
			ew.setDirection(((Integer) _surfDirections.get(2)).intValue());
			ew.setDirectAngle(((Double) _surfAbsBearings.get(2)).doubleValue());
			ew.setFireLocation((Point2D.Double) _enemyLocation.clone()); // last
																			// tick

			_enemyWaves.add(ew);
		}

		// update after EnemyWave detection, because that needs the previous
		// enemy location as the source of the wave
		_enemyLocation = project(_myLocation, absBearing, e.getDistance());

		updateWaves(robot);
		doSurfing(robot);
	}

	public void updateWaves(BaseBot robot) {
		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) _enemyWaves.get(x);

			ew.setDistanceTraveled((robot.getTime() - ew.getFireTime())
					* ew.getBulletVelocity());
			if (ew.getDistanceTraveled() > _myLocation.distance(ew
					.getFireLocation()) + 50) {
				_enemyWaves.remove(x);
				x--;
			}
		}
	}

	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = getFactorIndex(surfWave,
				predictPosition(surfWave, direction));

		return _surfStats[index];
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, calculate the index into our stat array for that factor.
	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
		double offsetAngle = (FuncLib.absoluteBearing(ew.getFireLocation(),
				targetLocation) - ew.getDirectAngle());
		double factor = Utils.normalRelativeAngle(offsetAngle)
				/ maxEscapeAngle(ew.getBulletVelocity()) * ew.getDirection();

		return (int) limit(0, (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
				BINS - 1);
	}

	// CREDIT: Iterative WallSmoothing by Kawigi
	// - return absolute angle to move at after account for WallSmoothing
	// robowiki.net?WallSmoothing
	public double wallSmoothing(Point2D.Double botLocation, double angle,
			int orientation) {
		while (!_fieldRect.contains(project(botLocation, angle, 160))) {
			angle += orientation * 0.05;
		}
		return angle;
	}

	// CREDIT: mini sized predictor from Apollon, by rozu
	// http://robowiki.net?Apollon
	public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
		Point2D.Double predictedPosition = (Point2D.Double) _myLocation.clone();
		double predictedVelocity = robot.getVelocity();
		double predictedHeading = robot.getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {
			moveAngle = wallSmoothing(
					predictedPosition,
					FuncLib.absoluteBearing(surfWave.getFireLocation(),
							predictedPosition) + (direction * (Math.PI / 2)),
					direction)
					- predictedHeading;
			moveDir = 1;

			if (Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this
			// in one tick
			maxTurning = Math.PI / 720d
					* (40d - 3d * Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading
					+ limit(-maxTurning, moveAngle, maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to breack down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir
					: moveDir);
			predictedVelocity = limit(-8, predictedVelocity, 8);

			// calculate the new predicted position
			predictedPosition = project(predictedPosition, predictedHeading,
					predictedVelocity);

			counter++;

			if (predictedPosition.distance(surfWave.getFireLocation()) < surfWave
					.getDistanceTraveled()
					+ (counter * surfWave.getBulletVelocity())
					+ surfWave.getBulletVelocity()) {
				intercepted = true;
			}
		} while (!intercepted && counter < 500);

		return predictedPosition;
	}

	public void doSurfing(BaseBot robot) {
		EnemyWave surfWave = getClosestSurfableWave();

		if (surfWave == null) {
			return;
		}

		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);

		double goAngle = FuncLib.absoluteBearing(surfWave.getFireLocation(),
				_myLocation);
		if (dangerLeft < dangerRight) {
			goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI / 2), -1);
		} else {
			goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI / 2), 1);
		}

		setBackAsFront(goAngle, robot);
	}

	private EnemyWave getClosestSurfableWave() {
		double closestDistance = 50000; // I juse use some very big number here
		EnemyWave surfWave = null;

		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) _enemyWaves.get(x);
			double distance = _myLocation.distance(ew.getFireLocation())
					- ew.getDistanceTraveled();
			if (distance > ew.getBulletVelocity() && distance < closestDistance) {
				surfWave = ew;
				closestDistance = distance;
			}
		}
		return surfWave;
	}

	public static void setBackAsFront(double goAngle, BaseBot robot) {
		double angle = Utils.normalRelativeAngle(goAngle
				- robot.getHeadingRadians());
		if (Math.abs(angle) > (Math.PI / 2)) {
			if (angle < 0) {
				robot.setTurnRightRadians(Math.PI + angle);
			} else {
				robot.setTurnLeftRadians(Math.PI - angle);
			}
			robot.setBack(100);
		} else {
			if (angle < 0) {
				robot.setTurnLeftRadians(-1 * angle);
			} else {
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}

	// CREDIT: from CassiusClay, by PEZ
	// - returns point length away from sourceLocation, at angle
	// robowiki.net?CassiusClay
	public static Point2D.Double project(Point2D.Double sourceLocation,
			double angle, double length) {
		return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
				sourceLocation.y + Math.cos(angle) * length);
	}

	public static double limit(double min, double value, double max) {
		return Math.max(min, Math.min(value, max));
	}

	public static double bulletVelocity(double power) {
		return (20D - (3D * power));
	}

	public static double maxEscapeAngle(double velocity) {
		return Math.asin(8.0 / velocity);
	}
}