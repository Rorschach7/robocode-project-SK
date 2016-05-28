package robots;

import robocode.*;

public class EnemyBot {
	
	private String name;
	private double distance;
	private double bearing;
	private double heading;
	private ScannedRobotEvent info;
	
	
	public EnemyBot() {
		name = "None";
		distance = 0;
		bearing = 0;
		heading = 0;
	}
	
	public void init(ScannedRobotEvent event) {
		info = event;
		name = event.getName();
		distance = event.getDistance();
		bearing = event.getBearing();
		heading = event.getHeading();		
	}
	
	public void init(HitByBulletEvent event) {
		name = event.getName();
		distance = -1;
		bearing = event.getBearing();
		heading = event.getHeading();
	}
	
	public String toString() {
		return name;
	}
	
	public void setBearing(double bearing) {
		this.bearing = bearing;
	}
	
	public void setHeading(double heading) {
		this.heading = heading;
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public double getBearing() {
		return bearing;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public double getHeading() {
		return heading;
	}
	
	public String getName() {
		return name;
	}
	
	public ScannedRobotEvent getInfo() {
		return info;
	}	
}
