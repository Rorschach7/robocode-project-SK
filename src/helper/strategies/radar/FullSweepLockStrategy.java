package helper.strategies.radar;

import robocode.ScannedRobotEvent;
import robots.BaseBot;

public class FullSweepLockStrategy extends RadarStrategy {
	
	enum RadarState {
		Lock, Sweep, FullScan
	}
	
	// Variables
	private boolean isEnemyLocked = false;
	private RadarState radarState;
	private int sweepScanCount;
	
	// Periodic scan
	private int interval = 30;
	private int count = 0;
	
	// Scanning Strategies
	private FullScanStrategy fullScan = new FullScanStrategy();
	private SweepScanStrategy sweepScan = new SweepScanStrategy();
	private LockScanStrategy lockScan = new LockScanStrategy();


	@Override
	public void attackingScan(BaseBot robot) {		
		// Radar Scanning
		// FullScan finished, start sweep scan
		if (!fullScan.getScanning() && radarState == RadarState.FullScan) {
			System.out.println("Full scan finished.");
			// Sweep search for our target at last known position
			sweepScan.attackingScan(robot);
			radarState = RadarState.Sweep;
		}	

		if (isEnemyLocked) {
			robot.getGunStrategy().execute(robot);
		} else {
			System.out.println("Enemy no longer locked.");
			// Use sweep to find target again
			// do a full scan if target cannot be found after given rounds
			if (sweepScanCount < 10 && (radarState == RadarState.Lock || radarState == RadarState.Sweep)) {
				sweepScan.execute(robot);
				radarState = RadarState.Sweep;
			} else {
				fullScan.execute(robot);
				radarState = RadarState.FullScan;
				sweepScanCount = 0;
			}
		}
		// Reset isEnemyLocked
		isEnemyLocked = false;
		
	}
	
	@Override
	public void periodicScan(BaseBot robot) {
		count++;
		
		if(count >= interval) {
			System.out.println("perform periodic scan");
			
			radarState = RadarState.FullScan;
			if(!fullScan.execute(robot)) {
				return;
			}			
			count = 0;
		}
	}

	@Override
	public void scanningScan(BaseBot robot) {
		fullScan.scanningScan(robot);
		radarState = RadarState.FullScan;
	}
	
	@Override
	public boolean execute(BaseBot robot, ScannedRobotEvent e) {
		// Sweep scan found target, lock on
		if (radarState == RadarState.Sweep
				&& robot.getTarget().getName().equals(e.getName())) {
			//System.out.println("Sweep found target, lock on");
			radarState = RadarState.Lock;
			isEnemyLocked = true;
		}

		if (radarState == RadarState.Lock) {
			if (robot.getTarget().getName().equals(e.getName())) {
				isEnemyLocked = true;
				lockScan.execute(robot);
			}
		}		
		return false;
	}
	
	@Override
	public String toString() {		
		return super.toString() + "Full Sweep Lock";
	}
}
