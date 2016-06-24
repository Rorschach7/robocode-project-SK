package helper.strategies.radar;

import robocode.ScannedRobotEvent;
import robots.BaseBot;

public class FullSweepLockStrategy extends ScanStrategy {
	
	enum RadarState {
		Lock, Sweep, FullScan
	}
	
	// Variables
	private boolean isEnemyLocked = false;
	private RadarState radarState;
	private int sweepScanCount;
	
	// Scanning Strategies
	private FullScanStrategy fullScan = new FullScanStrategy();
	private SweepScanStrategy sweepScan = new SweepScanStrategy();
	private LockScanStrategy lockScan = new LockScanStrategy();


	@Override
	public boolean attackingScan(BaseBot robot) {
		// Radar Scanning
		// FullScan finished, start sweep scan
		if (!fullScan.attackingScan(robot) && radarState == RadarState.FullScan) {
			//System.out.println("Full scan finished.");
			// Sweep search for our target at last known position
			sweepScan.attackingScan(robot);
			radarState = RadarState.Sweep;
		}	

		if (isEnemyLocked) {
			robot.getAimStrategy().execute(robot);
		} else {
			System.out.println("Enemy no longer locked.");
			// Use sweep to find target again
			// do a full scan if target cannot be found after given rounds
			if (sweepScanCount < 10
					&& (radarState == RadarState.Lock || radarState == RadarState.Sweep)) {
				sweepScan.attackingScan(robot);
			} else {
				fullScan.attackingScan(robot);
				radarState = RadarState.FullScan;
				sweepScanCount = 0;
			}
		}	
		return false;
	}

	@Override
	public void scanningScan(BaseBot robot) {
		fullScan.scanningScan(robot);
		radarState = RadarState.FullScan;
	}
	
	@Override
	public void updateScan(BaseBot robot, ScannedRobotEvent e) {
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
				lockScan.attackingScan(robot);
			}
		}	
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString() + "Full Sweep Lock";
	}
}