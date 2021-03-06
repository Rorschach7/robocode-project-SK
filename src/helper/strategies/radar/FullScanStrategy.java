package helper.strategies.radar;

import robocode.ScannedRobotEvent;
import robots.BaseBot;

public class FullScanStrategy extends RadarStrategy {
	
	private boolean scanning = false;
	
	@Override
	public boolean execute(BaseBot robot, ScannedRobotEvent e) {
		return execute(robot);
	}
	
	@Override
	public void attackingScan(BaseBot robot) {
		
	}
	
	@Override
	public void scanningScan(BaseBot robot) {
		if(execute(robot)) {
			super.scanningScan(robot);			
		}
	}
	
	/**
	 * The scan code for this strategy doesn't need the ScannedRobotEvent.
	 * With this function we can call the code without the event as parameter.
	 * @param robot
	 * @return returns true when scan finished.
	 */
	public boolean execute(BaseBot robot) {
		if (!scanning) {
			// make a short scan of the whole battlefield
			if(BaseBot.DEBUG_MODE) {								
				System.out.println("Executing Scan");
			}
			robot.setTurnRadarRight(360);
			scanning = true;
			return false;
		} else if (robot.getRadarTurnRemaining() < 10) {
			// Scan finished
			if(BaseBot.DEBUG_MODE) {								
				System.out.println("Scan finished");
			}
			scanning = false;
			return true;
		}
		return false;
	}
	
	public boolean getScanning() {
		return scanning;
	}

}
