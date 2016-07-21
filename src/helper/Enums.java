package helper;

public class Enums {
	
	public enum State {
		Scanning, Attacking, Evading,
	}		
	
	public enum AvoidWall {
		West, North, East, South, None;
	}	
	
	public enum PrevDynMov {
		AntiGrav, Random, Surf;
	}
}