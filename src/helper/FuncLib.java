package helper;

import java.awt.Point;
import java.util.ArrayList;

public class FuncLib {

	// if a bearing is not within the -pi to pi range, alters it to provide the
	// shortest angle
	public static double normaliseBearing(double ang) {
		if (ang > Math.PI)
			ang -= 2 * Math.PI;
		if (ang < -Math.PI)
			ang += 2 * Math.PI;
		return ang;
	}
	
	/** Returns the distance between two points **/
	public static double getRange(double x1, double y1, double x2, double y2) {
		double x = x2 - x1;
		double y = y2 - y1;
		double range = Math.sqrt(x * x + y * y);
		return range;
	}
	
	/**
	 * gets the absolute bearing between to x,y coordinates
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public static double absBearing(double x1, double y1, double x2, double y2) {
		double xo = x2 - x1;
		double yo = y2 - y1;
		double h = getRange(x1, y1, x2, y2);
		if (xo > 0 && yo > 0) {
			return Math.asin(xo / h);
		}
		if (xo > 0 && yo < 0) {
			return Math.PI - Math.asin(xo / h);
		}
		if (xo < 0 && yo < 0) {
			return Math.PI + Math.asin(-xo / h);
		}
		if (xo < 0 && yo > 0) {
			return 2.0 * Math.PI - Math.asin(-xo / h);
		}
		return 0;
	}
	
	public static double absoluteBearing(Point source, Point target) {
		return Math.atan2(target.x - source.x, target.y - source.y);
	}
	
	public static double limit(double value, double min, double max) {
		return Math.min(max, Math.max(min, value));
	}
	
	/**
	 * Finds the specefied robot among all spotted enemies.	 * 
	 * @param name
	 *            The name of the robot that you want.
	 * @return the Robot if already spotted and existing, null Otherwise.
	 */
	public static Bot findBotByName(String name, ArrayList<Bot> data) {
		for (Bot bot : data) {
			if (bot.getName().equals(name)) {
				return bot;
			}
		}		
		return null;
	}

}
