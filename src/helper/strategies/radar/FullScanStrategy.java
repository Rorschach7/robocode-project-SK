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
	 * @return
	 */
	public boolean execute(BaseBot robot) {
		if (!scanning) {
			// make a short scan of the whole battlefield			
			// System.out.println("Executing Scan");
			robot.setTurnRadarRight(360);
			scanning = true;
			return false;
		} else if (robot.getRadarTurnRemaining() < 10) {
			// Scan finished
			scanning = false;
			return true;
		}
		return false;
	}
	
	public boolean getScanning() {
		return scanning;
	}

}
