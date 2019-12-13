package eu.netmobiel.commons.util;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.util.GeometricShapeFactory;

public class GeometryHelper {
	/**
	 * The approximate distance in meters that corresponds with one degree 
	 * of latitude - about 111 km (varies between 110567 m at the equator 
	 * and 111699 m at the poles).
	 * Note that the distance per degree longitude is variable, that depends on the latitude.
	 */
	public static final double LATITUDE_DISTANCE = 111320;
	public static final double EARTH_EQUATORIAL_RADIUS = 6378.137;
	public static final double TO_RADIANS_RATIO= Math.PI / 180.0;
	public static final double TO_DEGREES_RATIO= 180.0 / Math.PI;
	public static final int WHOLE_CIRCLE_DEGREE_RANGE= 360;
	public static final int LONGITUDE_DEGREE_RANGE = WHOLE_CIRCLE_DEGREE_RANGE;
	public static final int LONGITUDE_DEGREE_MIN = -LONGITUDE_DEGREE_RANGE / 2;
	public static final int LONGITUDE_DEGREE_MAX = LONGITUDE_DEGREE_RANGE / 2;
	public static final int LATITUDE_DEGREE_RANGE = WHOLE_CIRCLE_DEGREE_RANGE / 2;
	public static final int LATITUDE_DEGREE_MIN = -LATITUDE_DEGREE_RANGE / 2;
	public static final int LATITUDE_DEGREE_MAX = LATITUDE_DEGREE_RANGE / 2;
	public static final int HEADING_NORTH = 0;
	public static final int HEADING_SOUTH = 180;
	public static final int HEADING_EAST = 90;
	public static final int HEADING_WEST = 270;
	public static final double EARTH_MEAN_RADIUS_KM = 6371.0087714;
	public static final double EARTH_EQUATOR_CIRCUMFERENCE_KM = 40075.017;
	public static final double PROJECTED_LATITUDE_RANGE = Math.PI;
	public static final double PROJECTED_LONGITUDE_RANGE = 2 * Math.PI;
	
	protected static final int WGS84SRID = 4326;
	/* To change the SRID in the database, execute the following query:
	 *	-- Set the SRID of the address location to 4326 (WGS84)
	 *	UPDATE address SET location = ST_SetSRID(location, 4326);
	 */

	protected static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), WGS84SRID);

	/**
	 * Creates a point from a latitude/longitude pair. Note that in the GeoJSON/WKT standards the X stands for
	 * longitude and Y for latitude.
	 * @param latitude the WGS84 latitude
	 * @param longitude the WGS84 longitude
	 * @return A Point object.
	 */
	public static Point createPoint(double latitude, double longitude) {
		return createPoint(new Coordinate(longitude, latitude));
	}
	
	public static Point createPoint(Coordinate c) {
		return geometryFactory.createPoint(c);
	}
	
	public static Polygon createPolygon(Coordinate[] coords) {
	    LinearRing ring = geometryFactory.createLinearRing(coords);
	    Polygon polygon = geometryFactory.createPolygon(ring, null);
		return polygon;
	}
	
	public static Geometry createFromWKT(String wkt) {
		try {
			return new WKTReader(geometryFactory).read(wkt);
		} catch (ParseException ex) {
			throw new IllegalArgumentException("Unable to parse WKT", ex);
		}
	}
	
	public static String createWKT(Geometry geometry) {
		return new WKTWriter().write(geometry);
	}

	public static double getDistanceInSRID(int distance_in_meters) {
		// Great circle distance 
		return distance_in_meters / (double)LATITUDE_DISTANCE;
	}
	
	public static LineString createLine(Coordinate a, Coordinate b) {
		return geometryFactory.createLineString(new Coordinate[] { a, b});
	}
	
	public static Geometry createCircle(Coordinate center, int distance_in_meters) {
		return createCircle(center.x, center.y, distance_in_meters);
	}
	
	public static Geometry createCircle(double x, double y, int distance_in_meters) {
		// Using flat earth approximation, not near the poles
		double d_gc = getDistanceInSRID(distance_in_meters); // In degrees
		double lon_corr = Math.abs(Math.cos((Math.PI / 180) * x));
		// Prevent divide by zero
		lon_corr = Math.max(lon_corr, 0.1);
		final int SIDES = 32;
	    Coordinate coords[] = new Coordinate[SIDES + 1];
	    for( int i = 0; i < SIDES; i++){
	        double angle = ((double) i / (double) SIDES) * Math.PI * 2.0;
	        double dx = Math.cos( angle ) * d_gc;
	        double dy = Math.sin( angle ) * d_gc / lon_corr;
	        coords[i] = new Coordinate( (double) x + dx, (double) y + dy );
	    }
	    coords[SIDES] = coords[0];

	    return GeometryHelper.createPolygon(coords);
	}
	
	public static Geometry createEllipse(double lat, double lon, double widthDegrees, double heightDegrees, double rotation) {
		GeometricShapeFactory shapeFactory = new GeometricShapeFactory(geometryFactory);
		shapeFactory.setNumPoints(12); // adjustable
		shapeFactory.setCentre(new Coordinate(lon, lat));
		shapeFactory.setHeight(heightDegrees);
		shapeFactory.setWidth(widthDegrees);
		shapeFactory.setRotation(rotation);
		return shapeFactory.createEllipse();
	}

	/**
	 * Decode the encoded polyline (OpenTripPlanner) into a geometry
	 * @param encodedPolyline the poly line string
	 * @return A geometry.
	 */
	public static Geometry deserializeEncodedPolyline(String encodedPolyline) {
        EncodedPolylineBean bean = new EncodedPolylineBean(encodedPolyline, null, 0);
        List<Coordinate> coords = PolylineEncoder.decode(bean);
        return geometryFactory.createLineString(coords.toArray(new Coordinate[coords.size()]));
	}

	/**
	 * Convert the encoded polyline (OpenTripPlanner) into WKT.
	 * @param encodedPolyline the poly line string
	 * @return A string with WKT.
	 */
	public static String convertEncodedPolyline2WKT(String encodedPolyline) {
		return createWKT(deserializeEncodedPolyline(encodedPolyline)); 
	}
}
