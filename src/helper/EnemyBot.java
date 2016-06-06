package helper;

import java.util.ArrayList;
import java.util.List;

import robocode.*;

public class EnemyBot {
	
	private String name;
	private boolean isDead;
	private ScannedRobotEvent info;
	//private List<WaveBullet> waves = new ArrayList<WaveBullet>();
	
	public EnemyBot() {
		name = "None";
	}
	
	public void init(ScannedRobotEvent event) {
		info = event;
		name = event.getName();	
		isDead = false;
	}
	
	public void init(HitByBulletEvent event) {
		name = event.getName();
		isDead = false;
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
	
	public void died() {
		isDead = true;
	}
	
	public boolean isDead() {
		return isDead;
	}
	
//	public List<WaveBullet> getWaves() {
//		return waves;
//	}
}
