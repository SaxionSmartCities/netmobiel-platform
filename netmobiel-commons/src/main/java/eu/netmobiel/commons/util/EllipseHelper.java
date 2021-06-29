package eu.netmobiel.commons.util;

import java.util.Arrays;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.CoordinatesConverter;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.GeometricShapeFactory;

public class EllipseHelper extends GeometryHelper {

    public static class EligibleArea {
    	public Polygon eligibleAreaGeometry;
    	public double carthesianDistance;
    	public double carthesianBearing;
    	
    }

    /**
     * Calculates an ellipse with the property that the distance from one focal point (departure stop) to
     * the border of the ellipse and then to the other focal point (arrival) is equal to the maximum detour distance.
     * If a time is given then the distance is calculated from the nominal speed (m/s). 
     * @param f1 the departure point in WGS84.
     * @param f2 the destination point in WGS84.
     * @param focal2Border the distance from one focal point to the border along the long axis.
     * @param defaultMaxDetourDistancePercentage The default maximum allowed detour distance as coefficient of the line distance. 
     * @return a polygon with the desired properties in WGS84.
     */

    public static EligibleArea calculateEllipse(Point f1, Point f2, Double focal2Border, double defaultFocal2BorderFactor) {
    	// See https://en.wikipedia.org/wiki/Ellipse
    	// In order to proper calculate the shape we have to switch to a different coordinate system: 
    	// UTM (Universal Transverse Mercator) 
    	// https://en.wikipedia.org/wiki/Universal_Transverse_Mercator_coordinate_system
    	// In UTM we can work in meters and is the earth flat, at least is a small area.
    	
    	// Calculate centroid of the ellipse
    	EligibleArea ea = new EligibleArea();
    	UTM f1_utm = polar2utm(f1.getCoordinate());
    	UTM f2_utm = polar2utm(f2.getCoordinate());
    	Coordinate c1_utm = utm2Coordinate(f1_utm);
    	Coordinate c2_utm = utm2Coordinate(f2_utm);
    	LineString line = createLine(c1_utm, c2_utm);
    	Point center = line.getCentroid();

    	
    	// Calculate large side and small side
    	// sqr(long half a) = sqr(short half b) + sqr(focal half c)
    	// Distance of ellipse border on large axis is half of the maximum allowed detour.
    	double focalDistance = line.getLength(); // in meter
    	double c = focalDistance / 2;
    	// the large radius
    	double a = c + (focal2Border != null ? focal2Border : focalDistance * defaultFocal2BorderFactor);
    	// the small radius
    	double b = Math.sqrt(a * a - c * c);
    	double rx = a;
    	double ry = b;
    	// Rotation angle is taken from right rotated coordinate system. Must be in radians.
    	double ra = Angle.angle(utm2Coordinate(f1_utm), utm2Coordinate(f2_utm));
		GeometricShapeFactory shapeFactory = new GeometricShapeFactory(geometryFactory);
		shapeFactory.setNumPoints(32); // adjustable
		shapeFactory.setCentre(center.getCoordinate());
		shapeFactory.setWidth(rx * 2);
		shapeFactory.setHeight(ry * 2);
		shapeFactory.setRotation(ra);
		Polygon p = shapeFactory.createEllipse();
		Coordinate[] ucs = p.getCoordinates();
		Coordinate[] pcs = Arrays.asList(ucs).stream()
				.map(uc -> utm2Polar(uc, f1_utm))
				.toArray(Coordinate[]::new);
		ea.eligibleAreaGeometry = geometryFactory.createPolygon(pcs);
		ea.carthesianDistance = line.getLength();
		// Calculate the angle. Note that in UTM the positive X is east
		// Rotate counterclockwise (North is up), invert it (clockwise is positive) and make it positive 
		ea.carthesianBearing = getBearing(c1_utm, c2_utm);
		return ea;
    }

    /**
     * Creates a circle. Could also be done with the ellipse, but as this circle is used in queries quite often, 
     * we create a more efficient one.
     * @param center The center of a circle in WGS-84 coordinates.
     * @param radius The radius in meters.
     * @return A polygon with 16 sides representing a constant distance on the flattened earth.
     */
    public static Polygon calculateCircle(Point center, Integer radius) {
    	// In order to proper calculate the shape we have to switch to a different coordinate system: 
    	// UTM (Universal Transverse Mercator) 
    	// https://en.wikipedia.org/wiki/Universal_Transverse_Mercator_coordinate_system
    	// In UTM we can work in meters and is the earth flat, at least is a small area.
    	
    	UTM center_utm = polar2utm(center.getCoordinate());
    	Coordinate center_utm_coord = utm2Coordinate(center_utm);
		GeometricShapeFactory shapeFactory = new GeometricShapeFactory(geometryFactory);
		shapeFactory.setNumPoints(16); // adjustable
		shapeFactory.setCentre(center_utm_coord);
		shapeFactory.setWidth(radius * 2.0);
		shapeFactory.setHeight(radius * 2.0);
		Polygon p = shapeFactory.createCircle();
		Coordinate[] ucs = p.getCoordinates();
		Coordinate[] pcs = Arrays.asList(ucs).stream()
				.map(uc -> utm2Polar(uc, center_utm))
				.toArray(Coordinate[]::new);
		return geometryFactory.createPolygon(pcs);
    }

    public static UTM polar2utm(double latitude, double longitude) {
		LatLong latlong = LatLong.valueOf(latitude, longitude, NonSI.DEGREE_ANGLE);
		return UTM.latLongToUtm(latlong, ReferenceEllipsoid.WGS84);
	}

	public static Coordinate utm2Coordinate(UTM utm) {
		return new Coordinate(utm.getCoordinates()[0], utm.getCoordinates()[1]);
	}
	
	public static UTM polar2utm(Coordinate c) {
		LatLong latlong = LatLong.valueOf(c.y, c.x, NonSI.DEGREE_ANGLE);
		UTM utm = UTM.latLongToUtm(latlong, ReferenceEllipsoid.WGS84);
		return utm;
	}

	public static Coordinate utm2Polar(UTM utm) {
		LatLong latlong = UTM.utmToLatLong(utm, ReferenceEllipsoid.WGS84);
		return new Coordinate(latlong.longitudeValue(NonSI.DEGREE_ANGLE), latlong.latitudeValue(NonSI.DEGREE_ANGLE));
	}

	public static Coordinate utm2Polar(Coordinate cutm, UTM utmRef) {
//		Coordinates<ProjectedCRS<Coordinates<?>>> coord2d;
		UTM utm = UTM.valueOf(utmRef.longitudeZone(), utmRef.latitudeZone(), cutm.x, cutm.y, SI.METER);
		CoordinatesConverter<UTM, LatLong> utmToLatLong = UTM.CRS.getConverterTo(LatLong.CRS);
	    LatLong latlong = utmToLatLong.convert(utm);
//		LatLong latlong = utm.getCoordinateReferenceSystem().getConverterTo(LatLong.CRS).convert(utm);
		return new Coordinate(latlong.longitudeValue(NonSI.DEGREE_ANGLE), latlong.latitudeValue(NonSI.DEGREE_ANGLE));
	}

	public static double getBearing(Point polar1, Point polar2) {
    	UTM utm1 = polar2utm(polar1.getCoordinate());
    	UTM utm2 = polar2utm(polar2.getCoordinate());
		return getBearing(utm2Coordinate(utm1), utm2Coordinate(utm2));
	}

	/**
	 * Returns the compass bearing in degrees. 0 is North.
	 * @param utmc1 The UTM coordinate of the tail
	 * @param utmc2 The UTM coordinat of the tip
	 * @return The bearing in degrees in the range [0, 360>
	 */
	public static double getBearing(Coordinate utmc1, Coordinate utmc2) {
		return Angle.toDegrees(Angle.normalizePositive((Angle.angle(utmc1, utmc2) - Angle.PI_OVER_2) * -1));
	}

}
