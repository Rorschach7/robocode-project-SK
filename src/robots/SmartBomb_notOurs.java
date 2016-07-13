package robots;

import robocode.*;
import java.util.Random;
import java.awt.Color;
import java.awt.geom.Point2D;

interface Constants {
	public static final int AVG_SIZE = 50;
	// number of velocity readings to average
	public static final int GRAV_EXTENT = 50;
	// amount of objects to track
	public static final int GRAV_WALL = 20;
} // amount of field from the walls

// SmartBomb - All code by Falnar

public class SmartBomb_notOurs extends AdvancedRobot implements Constants {
	private boolean lockedOn; // whether or not the enemy was found yet
	private double xSize; // shorter than getBattleFieldWidth()
	private double ySize; // ditto
	private Random generator;
	private double[] avgQ; // queue of last avgSize velocities
	private int avgPos; // head of queue in array
								// avoided
	private AntiGravity aGrav;

	// *******
	// *********Built in Robocode functions and events:
	// *******

	public void run() {
		aGrav = new AntiGravity();
		setColors(new Color(0, 0, 255), Color.black, new Color(128, 128, 128));
		lockedOn = false;
		avgPos = 0;
		generator = new Random();
		avgQ = new double[AVG_SIZE];
		for (int j = 0; j < AVG_SIZE; j++)
			// initialize the velocity queue with 4, half speed
			avgQ[j] = 4;
		xSize = getBattleFieldWidth();
		ySize = getBattleFieldHeight();
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		lockedOn = false;
		setAhead(200);
		setTurnRight(360);
		while (true) {
			if (getRadarTurnRemaining() == 0) {
				setTurnRadarRight(20);
			}
			move();
			if (Math.abs(getTurnRemaining()) == 0)
				setTurnRight(generator.nextGaussian() * 30);
			execute();
		}

	}

	public void onScannedRobot(ScannedRobotEvent e) {
		double x = Math.sin(e.getBearingRadians() + getHeadingRadians())
				* e.getDistance() + getX();
		double y = Math.cos(e.getBearingRadians() + getHeadingRadians())
				* e.getDistance() + getY();

		if (lockedOn == false) {
			lockedOn = true;
			setAhead(0);
			setTurnLeft(0);
		}

		radarLock(e);

		if (e.getDistance() > 400 || getTime() % 90 < 45)
			aGrav.addObject(x, y, -20);
		else
			aGrav.addObject(x, y, 20);

		adjustAvg(e.getVelocity());

	}

	// ******End of events, custom functions follow:
	// ##denotes primary function
	// #denotes secondary functions
	// others are trivial math functions

	// ##radarLock: maintains radar lock on enemy
	// moves the radar to where the enemy will be based on velocity
	private void radarLock(ScannedRobotEvent e) {
		double distance = e.getDistance();
		double absoluteBearing = e.getBearingRadians() + getHeadingRadians();
		double heading = e.getHeadingRadians();
		double velocity = e.getVelocity();
		double aimX = Math.sin(absoluteBearing) * distance + Math.sin(heading)
				* velocity - Math.sin(getHeadingRadians()) * getVelocity();
		double aimY = Math.cos(absoluteBearing) * distance + Math.cos(heading)
				* velocity - Math.cos(getHeadingRadians()) * getVelocity();
		setTurnRadarRightRadians(fixAngleRad(Math.atan2(aimX, aimY)
				- getRadarHeadingRadians()));
	}

	// ##move: takes care of robot movement
	// uses anti gravity movement
	//
	private void move() {
		double centerStrength;
		centerStrength = 10;
		if (getTime() % 60 < 30)
			centerStrength += 10;
		aGrav.addObject(xSize / 2, ySize / 2, centerStrength);
		go(aGrav.getDirection());
	}


	// ##go: moves the robot in the specified direction
	// reverses direction if it's quicker
	private void go(double direction) {
		double turn = fixAngleRad(direction - getHeadingRadians());
		if (turn < -Math.PI / 2 || turn > Math.PI / 2) {
			setTurnRightRadians(fixAngleRad(Math.PI - turn));
			setBack(100);
			return;
		}
		setTurnRightRadians(turn);
		setAhead(100);
	}

	// ##adjustAvg: keeps a queued average of the enemies velocity
	// average is of the past avgSize readings of velocity
	// effective against bots that stop and reverse direction often
	private void adjustAvg(double newV) {
		avgQ[avgPos] = newV;
		avgPos++;
		if (avgPos >= AVG_SIZE)
			avgPos = 0;
	}

	// same as fixAngle but for radians
	private double fixAngleRad(double angle) {
		while (angle <= -Math.PI)
			angle = angle + 2 * Math.PI;

		while (angle > Math.PI)
			angle = angle - 2 * Math.PI;

		return angle;
	}

	// AntiGravity: make virtual magnets and get attracted/repelled from them
	private class AntiGravity {
		Point2D.Double[] points; // stores all of the objects, their positions,
									// and their field strength
									// objects[objects][property]
		double[] fields;

		// properties:
		// 0: x
		// 1: y
		// 2: strength
		// if strength is negative, it attracts

		private AntiGravity() {
			points = new Point2D.Double[GRAV_EXTENT];
			fields = new double[GRAV_EXTENT];

		}

		public double getDirection() {
			double Xcomp = 0;
			double Ycomp = 0;
			double x = getX();
			double y = getY();
			double magnitude, angle;

			addWalls();

			for (int i = 0; i < GRAV_EXTENT; i++) {
				if (points[i] != null && points[i].distanceSq(x, y) != 0) {
					magnitude = fields[i] / points[i].distance(x, y);
					angle = Math.atan2(x - points[i].getX(),
							y - points[i].getY());
					Xcomp += magnitude * Math.sin(angle);
					Ycomp += magnitude * Math.cos(angle);
				}
			}
			clearObjects();
			return Math.atan2(Xcomp, Ycomp);
		}

		private void clearObjects() {
			for (int i = 0; i < GRAV_EXTENT; i++)
				points[i] = null;
		}

		private void addWalls() {
			addObject(getX(), 0, GRAV_WALL);
			addObject(getX(), ySize, GRAV_WALL);
			addObject(0, getY(), GRAV_WALL);
			addObject(xSize, getY(), GRAV_WALL);
			addObject(0, 0, GRAV_WALL);
			addObject(0, ySize, GRAV_WALL);
			addObject(xSize, 0, GRAV_WALL);
			addObject(xSize, ySize, GRAV_WALL);
		}

		public boolean addObject(double x, double y, double field) {
			int i = 0;
			while (i < GRAV_EXTENT && points[i] != null)
				i++;
			if (i == GRAV_EXTENT)
				return false;
			points[i] = new Point2D.Double(x, y);
			fields[i] = field;
			return true;
		}
	}
}
