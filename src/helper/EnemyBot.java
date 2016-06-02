package helper;

import robocode.*;

public class EnemyBot {
	
	private String name;
	private ScannedRobotEvent info;
	
	
	public EnemyBot() {
		name = "None";
		}
	
	public void init(ScannedRobotEvent event) {
		info = event;
		name = event.getName();				
	}
	
	public void init(HitByBulletEvent event) {
		name = event.getName();		
	}
	
	public String getName() {
		return name;
	}
	
	public String toString() {
		return name;
	}	
		
	public ScannedRobotEvent getInfo() {
		return info;
	}	
}
