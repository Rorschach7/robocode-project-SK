package helper.strategies.radar;

import robots.BaseBot;

public class FullScanStrategy extends ScanStrategy {
	
	private boolean scanning = false;
	
	@Override
	public boolean attackingScan(BaseBot robot) {
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
	
	@Override
	public void scanningScan(BaseBot robot) {
		if(attackingScan(robot)) {
			super.scanningScan(robot);			
		}
	}
	
	public boolean getScanning() {
		return scanning;
	}

}
