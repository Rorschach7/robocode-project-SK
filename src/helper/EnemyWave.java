package helper;

import java.awt.Point;

public class EnemyWave {
    private Point fireLocation;
    private long fireTime;
    private double bulletVelocity, directAngle, distanceTraveled;
    
    private int direction;

    public EnemyWave() { }

	public Point getFireLocation() {
		return fireLocation;
	}

	public void setFireLocation(Point fireLocation) {
		this.fireLocation = fireLocation;
	}

	public long getFireTime() {
		return fireTime;
	}

	public void setFireTime(long fireTime) {
		this.fireTime = fireTime;
	}

	public double getBulletVelocity() {
		return bulletVelocity;
	}

	public void setBulletVelocity(double bulletVelocity) {
		this.bulletVelocity = bulletVelocity;
	}

	public double getDirectAngle() {
		return directAngle;
	}

	public void setDirectAngle(double directAngle) {
		this.directAngle = directAngle;
	}

	public double getDistanceTraveled() {
		return distanceTraveled;
	}

	public void setDistanceTraveled(double distanceTraveled) {
		this.distanceTraveled = distanceTraveled;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}
    
}
