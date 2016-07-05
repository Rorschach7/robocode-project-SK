package helper.strategies.movement;


import helper.Data;
import helper.FuncLib;
import robocode.RobotDeathEvent;
import robots.BaseBot;

public class DynamicMovementChange extends MovementStrategy {
	
	// Strategies
	private AntiGravity antiGravity = new AntiGravity();
	private SingleWaveSurfing singleWaveSurfing = new SingleWaveSurfing();
	private RandomMovement randomMovement = new RandomMovement();
	private MovementStrategy strategy = randomMovement;
	
	// Statistics	
	private double successRate = 100;
	private double formerSuccessRate = 100;
	
	// Timer
	private int count = 0;
	private int interval = 100;
	
	@Override
	public void execute(BaseBot robot) {
		// Check if we have reliable data about our target
		Data data = FuncLib.findDataByName(robot.getTarget().getName(), robot.getDataList());
		
		//if(data.isReliable()) {		
		if(false){
		//TODO collect data and verify
		} else {
			strategy.execute(robot);
			count++;
			if(count >= interval) {
				formerSuccessRate = successRate;
				successRate = (robot.getEnemyBulletsDetected() - robot.getHitsTaken()) / robot.getEnemyBulletsDetected() * 100;
				System.out.println("current success rate: " + successRate + " bullets detected: " + robot.getEnemyBulletsDetected() + " hits Taken: " + robot.getHitsTaken());
				
				robot.setHitsTaken(0);
				robot.setEnemyBulletsDetected(0);
				count = 0;
				
				//TODO wont move at wave surfing
				if(strategy instanceof RandomMovement){
					System.out.println("switch to surfing");
					strategy = singleWaveSurfing;
				}else{
					System.out.println("switch to rand");
					strategy = randomMovement;
				}				
			}
		}
		
	}
	
	public MovementStrategy getCurrentMovementStrategy() {
		return strategy;
	}
	
}