package eu.netmobiel.commons.util;

import static javax.measure.unit.NonSI.*;
import static javax.measure.unit.SI.*;

import java.util.Arrays;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.CoordinatesConverter;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.geom.util.NoninvertibleTransformationException;
import com.vividsolutions.jts.util.GeometricShapeFactory;

public class EllipseHelper extends GeometryHelper {
    /**
     * Holds converter from degree to radian. 
     */
    private static final UnitConverter DEGREE_TO_RADIAN = DEGREE_ANGLE
            .getConverterTo(RADIAN);

    /**
     * Holds converter from radian to degree. 
     */
    private static final UnitConverter RADIAN_TO_DEGREE = DEGREE_TO_RADIAN
            .inverse();

	protected EllipseHelper() {
		// Do not instantiate
	}
    public static class EligibleArea {
    	public Geometry eligibleAreaGeometry;
    	public double carthesianDistance;
    	public double carthesianBearing;
    	@Override
    	public String toString() {
    		return String.format("Distance %.0f Bearing %.2f %s", carthesianDistance, carthesianBearing, eligibleAreaGeometry);
    	}
    }

    public static Coordinate getMidPoint(Coordinate c1, Coordinate c2) {
    	LineSegment line = new LineSegment(c1, c2);
    	return line.midPoint();
    }

    /**
     * Calculate the longitudinal offset from the most close central meridian of UTM 
     * @param p1 the first point
     * @param p2 the other point
     * @return A coordinate specifying the offset to apply to force the point in the reference UTM. 
     */
    public static AffineTransformation calculateTranslationToClosestUTMCenter(Point p1, Point p2) {
    	Coordinate midPoint = getMidPoint(p1.getCoordinate(), p2.getCoordinate());
		UTM ref = polar2utm(midPoint);
		double meridian =  RADIAN_TO_DEGREE.convert(UTM.getCentralMeridian(ref.longitudeZone(), ref.latitudeZone()));
		return AffineTransformation.translationInstance(meridian - midPoint.x, 0.0);
    }

    /**
     * Calculates an ellipse with the property that the distance from one focal point (departure stop) to
     * the border of the ellipse and then to the other focal point (arrival) is equal to the maximum detour distance.
     * If a time is given then the distance is calculated from the nominal speed (m/s).
     * To calculate the ellipse a transformation is made to UTM, a flat surface. After calculation the transformation to WGS84 is done.
     * UTM has the surface of the earth subdivided in latitude and longitude zones. The Netherlands sits in a single latitude zone, 
     * but in two different longitude zones, 31 and 32. To ease the calculation a custom transformation is made to sit around the 
     * central meridian of zone 31. Then, in succession, a transformation is made to UTM (in UTM 31U), the ellipse is calculated,
     * and the results are transformed back to WGS84, and then shifted longitudinal back to the original position.
     * Of course this transformation is not without its issues, but for our use it is good enough.
     * CAVEAT: The ellipse must fit in UTM 31U, that is the two focal points. An exception is thrown if it is not possible.
     * Hopefully, someone with more knowledge about geodesic stuff finds a better solution in the future.  
     *           
     * @param f1 the departure point in WGS84.
     * @param f2 the destination point in WGS84.
     * @param focal2Border the distance from one focal point to the border along the long axis in meter. 
     * 				Along the long axis this is exact the detour divided by two (as the crow flies).
     * @param defaultMaxDetourDistancePercentage The default maximum allowed detour distance as coefficient of the line distance. 
     * 				Only used if focal2Border is null.
     * @return a polygon with the desired properties in WGS84.
     */

    public static EligibleArea calculateEllipse(Point f1, Point f2, Double focal2Border, double defaultFocal2BorderFactor) {
    	// See https://en.wikipedia.org/wiki/Ellipse
    	// In order to proper calculate the shape we have to switch to a different coordinate system: 
    	// UTM (Universal Transverse Mercator) 
    	// https://en.wikipedia.org/wiki/Universal_Transverse_Mercator_coordinate_system
    	// In UTM we can work in meters and is the earth flat, at least is a small area.
    	// To prevent working in two different UTM sectors we translate the focus points around thre meridian of the closest UTM zone.
    	
    	AffineTransformation translation = calculateTranslationToClosestUTMCenter(f1, f2);
    	Point f1t = translation.transform(f1).getCentroid();
    	Point f2t = translation.transform(f2).getCentroid();
    	// Calculate centroid of the translated ellipse
    	EligibleArea ea = new EligibleArea();
    	UTM f1t_utm = polar2utm(f1t.getCoordinate());
    	UTM f2t_utm = polar2utm(f2t.getCoordinate());
    	if (f1t_utm.latitudeZone() != f2t_utm.latitudeZone() || f1t_utm.longitudeZone() != f2t_utm.longitudeZone()) {
    		throw new IllegalArgumentException(String.format("Cannot calculate ellipse, points are too far away: %s vs %s", f1, f2));
    	}
    	Coordinate c1t_utm = utm2Coordinate(f1t_utm);
    	Coordinate c2t_utm = utm2Coordinate(f2t_utm);
    	LineString linet_utm = createLine(c1t_utm, c2t_utm);
    	Point centert_utm = linet_utm.getCentroid();

    	
    	// Calculate large side and small side
    	// sqr(long half a) = sqr(short half b) + sqr(focal half c)
    	// Distance of ellipse border on large axis is half of the maximum allowed detour.
    	double focalDistance = linet_utm.getLength(); // in meter
    	double c = focalDistance / 2;
    	// the large radius
    	double a = c + (focal2Border != null ? focal2Border : focalDistance * defaultFocal2BorderFactor);
    	// the small radius
    	double b = Math.sqrt(a * a - c * c);
    	double rx = a;
    	double ry = b;
    	// Rotation angle is taken from right rotated coordinate system. Must be in radians.
    	double ra = Angle.angle(utm2Coordinate(f1t_utm), utm2Coordinate(f2t_utm));
		GeometricShapeFactory shapeFactory = new GeometricShapeFactory(geometryFactory);
		shapeFactory.setNumPoints(32); // adjustable
		shapeFactory.setCentre(centert_utm.getCoordinate());
		shapeFactory.setWidth(rx * 2);
		shapeFactory.setHeight(ry * 2);
		shapeFactory.setRotation(ra);
		Polygon pt = shapeFactory.createEllipse();
		// UTM coordinates (translated)
		Coordinate[] ucst = pt.getCoordinates();
		// Polar coordinate (translated). The reference utm is one of the points, both are in the same UTM.
		Coordinate[] pcst = Arrays.asList(ucst).stream()
				.map(uc -> utm2Polar(uc, f1t_utm))
				.toArray(Coordinate[]::new);
		Polygon pt_pcs = geometryFactory.createPolygon(pcst);
    	AffineTransformation inverseTranslation;
		try {
			inverseTranslation = translation.getInverse();
		} catch (NoninvertibleTransformationException e) {
    		throw new IllegalStateException("Cannot invert translation", e);
		}
		ea.eligibleAreaGeometry = inverseTranslation.transform(pt_pcs);
		ea.carthesianDistance = focalDistance;
		// Calculate the angle. Note that in UTM the positive X is east
		// Rotate counterclockwise (North is up), invert it (clockwise is positive) and make it positive 
		ea.carthesianBearing = getBearing(c1t_utm, c2t_utm);
		return ea;
    }

    /**
     * Creates a circle. Could also be done with the ellipse, but as this circle is used in queries quite often, 
     * we create a more efficient one.
     * Although the circle point might get outside the UTM sector, the transformation back is likely to be good enough.
     * @param center The center of a circle in WGS-84 coordinates.
     * @param radius The radius in meters.
     * @return A polygon with 16 sides representing a constant distance on the flattened earth.
     */
    public static Polygon calculateCircle(Point center, Integer radius) {
    	// In order to proper calculate the shape we have to switch to a different coordinate system: 
    	// UTM (Universal Transverse Mercator) 
    	// https://en.wikipedia.org/wiki/Universal_Transverse_Mercator_coordinate_system
    	// In UTM we can work in meters and is the earth flat, at least in a small area.
    	
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

	public static UTM polar2utm(Coordinate c) {
		return polar2utm(c.y, c.x);
	}

	public static Coordinate utm2Coordinate(UTM utm) {
		return new Coordinate(utm.getCoordinates()[0], utm.getCoordinates()[1]);
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
