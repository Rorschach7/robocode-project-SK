package helper.strategies;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import helper.Bot;
import helper.EnemyWave;
import helper.FuncLib;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import robots.BaseBot;

public class WaveSurfing extends MovementStrategy{
	private ArrayList<Bot> enemies = new ArrayList<>();
	public Point _myLocation; // our bot's location
	BaseBot robot;
	public static int BINS = 47;
	public static double _surfStats[] = new double[BINS]; // we'll use 47 bins
	public ArrayList<Integer> _surfDirections = new ArrayList<>();
	public ArrayList<Double> _surfAbsBearings = new ArrayList<>();
	public static Rectangle2D.Double _fieldRect = new java.awt.geom.Rectangle2D.Double(
			18, 18, 764, 564);
	private int moveDirection;

	@Override
	public void execute(BaseBot robot) {
		this.robot = robot;
		enemies = robot.getEnemies();
		_myLocation = new Point((int)robot.getX(), (int)robot.getY());
		robot.setMaxVelocity(8);
		moveDirection = robot.getMoveDirection();
		
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

		setBackAsFront(robot, goAngle);
		
	}
	

	// CREDIT: Iterative WallSmoothing by Kawigi
	// - return absolute angle to move at after account for WallSmoothing
	// robowiki.net?WallSmoothing
	public double wallSmoothing(Point botLocation, double angle, int orientation) {
		while (!_fieldRect.contains(project(botLocation, angle, 160))) {
			angle += orientation * 0.05;
		}
		return angle;
	}

	// CREDIT: from CassiusClay, by PEZ
	// - returns point length away from sourceLocation, at angle
	// robowiki.net?CassiusClay
	public static Point project(Point sourceLocation, double angle,
			double length) {
		return new Point((int) (sourceLocation.x + Math.sin(angle) * length),
				(int) (sourceLocation.y + Math.cos(angle) * length));
	}
	
	public Point predictPosition(EnemyWave surfWave, int direction) {
		Point predictedPosition = (Point) _myLocation.clone();
		double predictedVelocity = robot.getVelocity();
		double predictedHeading = robot.getHeadingRadians();
		double maxTurning, moveAngle;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {
			moveAngle = wallSmoothing(
					predictedPosition,
					FuncLib.absoluteBearing(surfWave.getFireLocation(),
							predictedPosition) + (direction * (Math.PI / 2)),
					direction)
					- predictedHeading;
			moveDirection = 1;

			if (Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDirection = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this
			// in one tick
			maxTurning = Math.PI / 720d
					* (40d - 3d * Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading
					+ FuncLib.limit(-maxTurning, moveAngle, maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to breack down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity += (predictedVelocity * moveDirection < 0 ? 2 * moveDirection
					: moveDirection);
			predictedVelocity = FuncLib.limit(-8, predictedVelocity, 8);

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
	
	public static void setBackAsFront(AdvancedRobot robot, double goAngle) {

		double angle = Utils.normalRelativeAngle(goAngle
				- robot.getHeadingRadians());
		System.out.println("angle " + angle);
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
	
	public EnemyWave getClosestSurfableWave() {
		double closestDistance = 50000; // I juse use some very big number here
		double closestDist2 = 50000;

		EnemyWave surfWave = null;
		EnemyWave surfWave2 = null;
		for (Bot enemy : enemies) {
			for (int x = 0; x < enemy.getBulletWave().size(); x++) {
				EnemyWave ew = (EnemyWave) enemy.getBulletWave().get(x);
				double distance = _myLocation.distance(ew.getFireLocation())
						- ew.getDistanceTraveled();

				if (distance > ew.getBulletVelocity()
						&& distance < closestDistance) {
					surfWave = ew;
					closestDistance = distance;
				}
			}
			if (closestDistance < closestDist2) {
				closestDist2 = closestDistance;
				surfWave2 = surfWave;
			}
		}
		return surfWave2;
	}
	
	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = getFactorIndex(surfWave,
				predictPosition(surfWave, direction));

		return _surfStats[index];
	}
	
	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, calculate the index into our stat array for that factor.
	public static int getFactorIndex(EnemyWave ew, Point targetLocation) {
		double offsetAngle = (FuncLib.absoluteBearing(ew.getFireLocation(),
				targetLocation) - ew.getDirectAngle());
		double factor = Utils.normalRelativeAngle(offsetAngle)
				/ maxEscapeAngle(ew.getBulletVelocity()) * ew.getDirection();

		return (int) FuncLib.limit(0, (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
				BINS - 1);
	}
	
	public void collectWaveSurfData(BaseBot robot, ScannedRobotEvent event) {
		if(robot.getTarget().getInfo() == null) {
			return;
		}
		//System.out.println("targetE: " + target.getInfo().getEnergy() + " botE: " + robot.getEnergy());
		// wave surfing stuff
		ScannedRobotEvent targetBot = robot.getTarget().getInfo();
		double absBearing = robot.getHeadingRadians()
				+ targetBot.getBearingRadians();
		double lateralVelocity = robot.getVelocity()
				* Math.sin(targetBot.getBearingRadians());
		_surfDirections.add(0, new Integer((lateralVelocity >= 0) ? 1
				: -1));
		_surfAbsBearings.add(0, new Double(absBearing + Math.PI));

		if (robot.getBulletPower() < 3.01 && robot.getBulletPower() > 0.09
				&& _surfDirections.size() > 2) {
			EnemyWave ew = new EnemyWave();
			ew.setFireTime(robot.getTime() - 1);
			ew.setBulletVelocity(20D - (3D * robot.getBulletPower()));
			ew.setDistanceTraveled(20D - (3D * robot.getBulletPower()));
			ew.setDirection(((Integer) _surfDirections.get(2))
					.intValue());
			ew.setDirectAngle(((Double) _surfAbsBearings.get(2))
					.doubleValue());
			ew.setFireLocation((Point) _myLocation.clone()); // last
																// tick

			robot.getTarget().addBulletWave(ew);
			// TODO move
			// doSurfing();

		}

		_myLocation = new Point((int) (robot.getX() + absBearing
				* targetBot.getDistance()), (int) (robot.getY() + absBearing
				* targetBot.getDistance()));

		updateWaves(robot);
	}
	
	public void updateWaves(BaseBot robot) {
		for (Bot enemy : enemies) {
			for (int x = 0; x < enemy.getBulletWave().size(); x++) {
				EnemyWave ew = (EnemyWave) enemy.getBulletWave().get(x);
				ew.setDistanceTraveled((robot.getTime() - ew.getFireTime())
						* ew.getBulletVelocity());
				if (ew.getDistanceTraveled() > new Point((int) robot.getX(),
						(int) robot.getY()).distance(ew.getFireLocation()) + 50) {
					enemy.getBulletWave().remove(x);
					x--;
				}
			}
		}
	}

	public static double maxEscapeAngle(double velocity) {
		return Math.asin(8.0 / velocity);
	}

}
