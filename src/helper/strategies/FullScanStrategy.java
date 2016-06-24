package helper.strategies;

import helper.Enums.State;
import robots.BaseBot;

public class FullScanStrategy extends ScanStrategy {
	
	private boolean scanStarted = false;
	
	@Override
	public void execute(BaseBot robot) {
		if (!scanStarted) {
			// make a short scan of the whole battlefield			
			// System.out.println("Executing Scan");
			robot.setTurnRadarRight(360);
			scanStarted = true;
		} else if (robot.getRadarTurnRemaining() < 10) {
			// Scan finished
			scanStarted = false;
			robot.setState(State.Attacking);
		}
	}

}
