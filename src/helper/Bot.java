package helper;

import java.util.ArrayList;

import robocode.*;

public class Bot {
	
	private String name;
	private boolean isDead = false;
	private ScannedRobotEvent info;
	private ArrayList<EnemyWave> bulletWave;	
	private double posX;
	private double posY; 
	private double aggro;
	
	
	public Bot() {
		name = "None";
		bulletWave = new ArrayList<>();
	}
	
	public Bot(HitRobotEvent event) {
		// TODO Auto-generated constructor stub
	}

	public void init(ScannedRobotEvent event) {
		info = event;
		name = event.getName();	
			
	}
	
	public void init(HitRobotEvent event) {	
		name = event.getName();	
	}
	
	public void addBulletWave(EnemyWave bulletWave){
		this.bulletWave.add(bulletWave);
	}
	
	public void init(HitByBulletEvent event) {
		name = event.getName();		
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
		return name + " Dead: " + isDead;
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
	
	/**
	 * This function does not calculate any position. It simply returns the x value.
	 * Use this only if you updated the position with the updatePos() method.
	 * @return
	 */
	public double getPosX() {
		return posX;
	}
	
	/**
	 * This function does not calculate any position. It simply returns the y value.
	 * Use this only if you updated the position with the updatePos() method.
	 * @return
	 */
	public double getPosY() {
		return posY;
	}
	
	/**
	 * Sets the x and y position of the bot.
	 * @param x
	 * @param y
	 */
	public void updatePos(double x, double y) {
		posX = x;
		posY = y;
	}
	
	public void addAggro(double aggro) {
		this.aggro += aggro;
	}
	
	public void resetAggro() {
		this.aggro = 0;
	}

	/**
	 * @return the aggro
	 */
	public double getAggro() {
		return aggro;
	}

	/**
	 * @param aggro the aggro to set
	 */
	public void setAggro(double aggro) {
		this.aggro = aggro;
	}


}
