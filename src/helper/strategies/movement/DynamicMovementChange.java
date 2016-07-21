package helper.strategies.movement;


import helper.Data;
import helper.FuncLib;
import robocode.ScannedRobotEvent;
import robots.BaseBot;

public class DynamicMovementChange extends MovementStrategy {
	
	// Strategies
	private SingleWaveSurfing singleWaveSurfing = new SingleWaveSurfing();
	private RandomMovement randomMovement = new RandomMovement();
	private AntiGravity antigravMovement = new AntiGravity();
	private MovementStrategy strategy = randomMovement;
	
	// Statistics	
	private double successRate = 100;
	private double highestSuccessRate = 100;
	private double highestHitsTaken;
	private double highestDetectedBullets;
	private int passedAllMovements = 0;
	
	// Timer
	private int count = 0;
	private int interval = 50;
	
	@Override
	public void execute(BaseBot robot) {
		// Check if we have reliable data about our target
		Data data = FuncLib.findDataByName(robot.getTarget().getName(), robot.getDataList());
		if(data.movementIsReliable()){
			double randSucc = data.getRandomSuccRate();
			double antiSucc = data.getAntiGravSuccRate();
			double surfSucc = data.getSurfingSuccRate();
			
			//choose strategy with highest success rate
			if(randSucc > antiSucc && randSucc > surfSucc){
				randomMovement.execute(robot);
			}else if(antiSucc > randSucc && antiSucc > surfSucc){
				antigravMovement.execute(robot);
			}else{
				singleWaveSurfing.execute(robot);
			}
			

		} else {
			strategy.execute(robot);
			count++;
			if(count >= interval) {
				highestSuccessRate = successRate;
				
				//sometimes a bullet on the fly while changing creates false data
				if(robot.getEnemyBulletsDetected() < robot.getHitsTaken() && highestDetectedBullets != 0){
					highestSuccessRate = (highestDetectedBullets - (highestHitsTaken+1)) / highestDetectedBullets * 100;
					robot.setHitsTaken(robot.getHitsTaken()-1);
				}
				
				successRate = (robot.getEnemyBulletsDetected() - robot.getHitsTaken()) / robot.getEnemyBulletsDetected() * 100;
				if(BaseBot.DEBUG_MODE) {								
					System.out.println("current success rate: " + successRate + " bullets detected: " + robot.getEnemyBulletsDetected() + " hits Taken: " + robot.getHitsTaken());
				}
				
				count = 0;
				robot.setEnemyBulletsDetected(0);
				robot.setHitsTaken(0);
				highestHitsTaken = robot.getHitsTaken();
				highestDetectedBullets = robot.getEnemyBulletsDetected();				
				
				if(successRate > highestSuccessRate && passedAllMovements >= 3){
					//Don't change, current movement strategy is better
					return;
				}
				
				if(strategy instanceof RandomMovement){
					if(BaseBot.DEBUG_MODE) {								
						System.out.println("MOVE: switch to surfing");	
					}
					strategy = singleWaveSurfing;
					passedAllMovements++;
				}else if(strategy instanceof SingleWaveSurfing){
					if(BaseBot.DEBUG_MODE) {								
						System.out.println("MOVE: switch to anti grav");
					}
					strategy = antigravMovement;
					passedAllMovements++;
				}else if(strategy instanceof AntiGravity){
					if(BaseBot.DEBUG_MODE) {								
						System.out.println("MOVE: switch to rand");
					}
					strategy = randomMovement;
					passedAllMovements++;
				}
			}
		}		
	}
	
	@Override
	public void collectData(BaseBot robot, ScannedRobotEvent e) {
		if(strategy instanceof SingleWaveSurfing){
			strategy.collectData(robot, e);
		}
	
	}
	
	public MovementStrategy getCurrentMovementStrategy() {
		return strategy;
	}
	
}