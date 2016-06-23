package helper;

import java.util.ArrayList;
import robocode.*;

public class Bot {
	
	private String name;
	private boolean isDead;
	private ScannedRobotEvent info;
	private ArrayList<EnemyWave> bulletWave;	
	
	public Bot() {
		name = "None";
		bulletWave = new ArrayList<>();
	}
	
	public void init(ScannedRobotEvent event) {
		info = event;
		name = event.getName();	
		isDead = false;
	}
	
	public void addBulletWave(EnemyWave bulletWave){
		this.bulletWave.add(bulletWave);
	}
	
	public void init(HitByBulletEvent event) {
		name = event.getName();
		isDead = false;
	}
	
	public ArrayList<EnemyWave> getBulletWave() {
		return bulletWave;
	}

	public void setBulletWave(ArrayList<EnemyWave> bulletWave) {
		this.bulletWave = bulletWave;
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
	
	public void alive() {
		isDead = false;
	}
	
	public boolean isDead() {
		return isDead;
	}

}
