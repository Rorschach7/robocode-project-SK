package helper;

public class Enums {
	
	public enum State {
		Scanning, Attacking, Evading, Finishing
	}

	public enum MovementPattern {
		Circle, Eight, Scanning, Stop, UpAndDown, Random
	}

	public enum RadarState {
		Lock, Sweep, FullScan
	}

	public enum FireMode {
		LinearTargeting, GuessFactor
	}
	
	public enum AvoidWall {
		West, North, East, South, None;
	}	

}
