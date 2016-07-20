package helper.strategies.radar;

import helper.Enums.State;
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
			// Find target			
			//robot.setTarget(robot.getTargetStrategy().execute(robot));
			if(BaseBot.DEBUG_MODE) {								
				System.out.println("Full scan finished.");
			}
			
			if(robot.getTarget().isDead()) {
				System.out.println("WARNING. SEARCHING FOR DEAD TARGET");
				System.out.println("MAKE SURE A VALID TARGET IS ASSIGNED");
			} 			
			// Sweep search for our target at last known position
			sweepScan.attackingScan(robot);
			radarState = RadarState.Sweep;
		}	

		if (isEnemyLocked) {
			robot.getGunStrategy().execute(robot);
		} else {
			//System.out.println("Enemy no longer locked.");
			// Use sweep to find target again
			// do a full scan if target cannot be found after given rounds
			if (sweepScanCount < 7 && (radarState == RadarState.Lock || radarState == RadarState.Sweep)) {
				sweepScan.execute(robot);
				radarState = RadarState.Sweep;
				//System.out.println("Do Sweep");
				sweepScanCount++;
			} else {
				fullScan.execute(robot);
				radarState = RadarState.FullScan;
				sweepScanCount = 0;
				//System.out.println("Sweep no found. Full Scan");
			}
		}
		// Reset isEnemyLocked
		isEnemyLocked = false;
		
	}
	
	@Override
	public void periodicScan(BaseBot robot) {
		count++;
		
		if(count >= interval) {
			if(BaseBot.DEBUG_MODE) {								
				System.out.println("perform periodic scan");
			}
			
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
		
		//System.out.println("TARGET: LOCK ON " + robot.getTarget());
		
		if (radarState == RadarState.Sweep
				&& robot.getTarget().getName().equals(e.getName())) {
			//System.out.println("Sweep found target, lock on");
			radarState = RadarState.Lock;
			isEnemyLocked = true;
		}

		if (radarState == RadarState.Lock) {
			if(robot.getTarget() == null){
				robot.setState(State.Scanning);
			}
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
