package eu.netmobiel.commons.util;

import org.jscience.geography.coordinates.UTM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Take a Point in WGS84, convert to a local projection and buffer at 1km then
 * reproject to WGS84 and print.
 * 
 * 
 * @author ian
 *
 */
public class EllipseHelperTestApp {
	private static Logger log = LoggerFactory.getLogger(EllipseHelperTestApp.class);
	
	public static void main(String args[]) {
		UTM utm = EllipseHelper.polar2utm(52.5, 6.5);
		Coordinate c = EllipseHelper.utm2Coordinate(utm);
		log.info(String.format("X = %f, Y = %f", c.x, c.y, c.z));
		Coordinate latlong = EllipseHelper.utm2Polar(utm);
		log.info(String.format("lat = %f, lon = %f", latlong.y, latlong.x));
		
	}

}
