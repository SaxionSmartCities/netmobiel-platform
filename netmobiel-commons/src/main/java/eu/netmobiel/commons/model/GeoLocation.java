package eu.netmobiel.commons.model;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Point;

import eu.netmobiel.commons.util.GeometryHelper;

/**
 * Manages a location on Earth. 
 * 
 * TODO This class contains old code that should be replaced with a decent library like JTS, jscience or geotools.
 *  
 * @author Jaap Reitsma
 *
 */
@Embeddable
@Vetoed
@Access(AccessType.FIELD)	// Add this annotation, otherwise no JPA ModelGen attribute is generated for Point.
public class GeoLocation implements Serializable {
	public static final GeoLocation NORTH_POLE = GeoLocation.fromDegrees( GeometryHelper.LATITUDE_DEGREE_MAX, 0 );
	public static final GeoLocation SOUTH_POLE = GeoLocation.fromDegrees( GeometryHelper.LATITUDE_DEGREE_MIN, 0 );	
	public static final int MAX_LABEL_LENGTH = 256;
	/**
	 * 
	 */
	private static final long serialVersionUID = 6781635032425264522L;

	@Basic			// Add this annotation, otherwise no JPA ModelGen attribute is generated.
	@Column(name = "point")
	private Point point;

	@Transient
	private Double latitude;
	
	@Transient
	private Double longitude;
	/**
	 * Explanatory label, non-normative.
	 */
	@Column(name = "label", length = MAX_LABEL_LENGTH)
	private String label;
	
	public GeoLocation() {
	}
	
	public GeoLocation(Double latitude, Double longitude, String label) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.label = label;
		updatePoint();
	}
	
	public GeoLocation(Double latitude, Double longitude) {
		this(latitude, longitude, null);
	}
	
	public GeoLocation(Point p, String label) {
		this.point = p;
		this.label = label;
		updateLatLon();
	}
	public GeoLocation(Point p) {
		this(p, null);
	}
	
	public GeoLocation(GeoLocation other) {
		this(other.point, other.label);
	}
	
	private void updatePoint() {
		if (latitude != null && longitude != null) {
			this.point = GeometryHelper.createPoint(latitude.doubleValue(), longitude.doubleValue());
		} else {
			this.point = null;
		}
	}
	
//	@PostLoad
	// Cannot use PostLoad at tghis place in Hibernate 5.3.10 (wildfly 17) due to 
	// a nasty bug: https://hibernate.atlassian.net/browse/HHH-13110#icft=HHH-13110
	private void updateLatLon() {
		if (point != null) {
			this.latitude = this.point.getY();
			this.longitude = this.point.getX();
		} else {
			this.latitude = null;
			this.longitude = null;
		}
	}

	public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
		updateLatLon();
	}

	public Double getLatitude() {
		if (latitude == null) {
			updateLatLon();
		}
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
		updatePoint();
	}

	public Double getLongitude() {
		if (longitude == null) {
			updateLatLon();
		}
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
		updatePoint();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Transient
	public double getLatitudeRad() {
		return getLatitude() * GeometryHelper.TO_RADIANS_RATIO;
	}

	@Transient
	public double getLongitudeRad() {
		return getLongitude() * GeometryHelper.TO_RADIANS_RATIO;
	}
	
	@Transient
	public boolean isDefined() {
		return getLatitude() != null && getLongitude() != null;
	}

	/**
	 * @param latitude in degrees
	 * @param longitude in degrees
	 * @return a point with coordinates given in degrees
	 */
	public static GeoLocation fromDegrees(double latitude, double longitude) {
		return new GeoLocation( normalizeLatitude( latitude ), normalizeLongitude( longitude ) );
	}

	/**
	 * @param latitude in degrees
	 * @param longitude in degrees
	 * @return a point with coordinates given in degrees
	 */
	public static GeoLocation fromDegreesInclusive(double latitude, double longitude) {
		return new GeoLocation( normalizeLatitude( latitude ), normalizeLongitudeInclusive( longitude ) );
	}

	/**
	 * @param longitude in degrees
	 * @return longitude normalized in ]-180;+180]
	 */
	public static double normalizeLongitude(double longitude) {
		if(longitude ==  ( -GeometryHelper.LONGITUDE_DEGREE_RANGE / 2.0 ) ) {
			return GeometryHelper.LONGITUDE_DEGREE_RANGE / 2.0 ;
		}
		return normalizeLongitudeInclusive( longitude );
	}

	/**
	 * @param longitude in degrees
	 * @return longitude normalized in [-180;+180]
	 */
	public static double normalizeLongitudeInclusive(double longitude) {

		if( (longitude < -( GeometryHelper.LONGITUDE_DEGREE_RANGE / 2.0 ) ) || (longitude > ( GeometryHelper.LONGITUDE_DEGREE_RANGE / 2.0 ) ) ) {
			double _longitude;
			// shift 180 and normalize full circle turn
			_longitude = ( ( longitude + ( GeometryHelper.LONGITUDE_DEGREE_RANGE / 2.0 ) ) % GeometryHelper.WHOLE_CIRCLE_DEGREE_RANGE );
			// as Java % is not a math modulus we may have negative numbers so the unshift is sign dependant
			if( _longitude < 0) {
				_longitude = _longitude + ( GeometryHelper.LONGITUDE_DEGREE_RANGE / 2.0 );
			} else {
				_longitude = _longitude - ( GeometryHelper.LONGITUDE_DEGREE_RANGE / 2.0 );
			}
			return _longitude;
		}
		return longitude;
	}

	/**
	 * @param lat in degrees
	 * @return latitude normalized in [-90;+90]
	 */
	public static double normalizeLatitude(double lat) {
		if ( lat > GeometryHelper.LATITUDE_DEGREE_MAX || lat < GeometryHelper.LATITUDE_DEGREE_MIN ) {
			// shift 90, normalize full circle turn and 'symmetry' on the lat axis with abs
			double _latitude = Math.abs( ( lat + ( GeometryHelper.LATITUDE_DEGREE_RANGE / 2.0 ) ) % ( GeometryHelper.WHOLE_CIRCLE_DEGREE_RANGE ) );
			// Push 2nd and 3rd quadran in 1st and 4th by 'symmetry'
			if( _latitude > GeometryHelper.LATITUDE_DEGREE_RANGE ) {
				_latitude= GeometryHelper.WHOLE_CIRCLE_DEGREE_RANGE- _latitude;
			}
			// unshift
			_latitude= _latitude - ( GeometryHelper.LATITUDE_DEGREE_RANGE / 2.0 );

			return _latitude;
		}
		return lat;
	}

	/**
	 * @param latitude in radians
	 * @param longitude in radians
	 * @return a point with coordinates given in radians
	 */
	public static GeoLocation fromRadians(double latitude, double longitude) {
		return fromDegrees( latitude * GeometryHelper.TO_DEGREES_RATIO, longitude * GeometryHelper.TO_DEGREES_RATIO);
	}

	/**
	 * Calculate end of travel point
	 *
	 * @param distance to travel
	 * @param heading of travel in decimal degree
	 * @return arrival point
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Compute destination</a>
	 */
	public GeoLocation computeDestination(double distance, double heading) {
		double headingRadian = heading * GeometryHelper.TO_RADIANS_RATIO;

		double destinationLatitudeRadian = Math.asin(
				Math.sin( getLatitudeRad() ) * Math.cos( distance / GeometryHelper.EARTH_MEAN_RADIUS_KM ) + Math.cos(
						getLatitudeRad()
				) * Math.sin( distance / GeometryHelper.EARTH_MEAN_RADIUS_KM ) * Math.cos(
						headingRadian
				)
		);

		double destinationLongitudeRadian = getLongitudeRad() + Math.atan2(
				Math.sin( headingRadian ) * Math.sin(
						distance / GeometryHelper.EARTH_MEAN_RADIUS_KM
				) * Math.cos( getLatitudeRad() ),
				Math.cos( distance / GeometryHelper.EARTH_MEAN_RADIUS_KM ) - Math.sin( getLatitudeRad() ) * Math.sin(
						destinationLatitudeRadian
				)
		);

		return fromRadians( destinationLatitudeRadian, destinationLongitudeRadian );
	}

	/**
	 * Compute distance between two points
	 *
	 * @param other a {@link org.hibernate.search.spatial.impl.Point} object.
	 * @return the distance in kilometers
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Distance haversine formula</a>
	 */
	public double getDistanceTo(GeoLocation other) {
		return getDistanceTo( other.getLatitude(), other.getLongitude() );
	}

	/**
	 * Compute distance point and other location given by its latitude and longitude in decimal degrees
	 *
	 * @param lat in decimal degrees
	 * @param lon in decimal degrees
	 * @return the distance in kilometers
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Distance haversine formula</a>
	 */
	public final double getDistanceTo(final double lat, final double lon) {
		double destLatRad = normalizeLatitude(lat) * GeometryHelper.TO_RADIANS_RATIO;
		double destLonRad = normalizeLongitude(lon) * GeometryHelper.TO_RADIANS_RATIO;
		final double dLat= (destLatRad - getLatitudeRad()) / 2.0d;
		final double dLon= (destLonRad - getLongitudeRad()) / 2.0d;
		final double a = Math.pow( Math.sin(dLat), 2) +
				Math.pow(Math.sin(dLon), 2) * Math.cos(getLatitudeRad()) * Math.cos(destLatRad);
		final double c = 2.0d * Math.atan2(Math.sqrt( a ), Math.sqrt(1.0d - a));
//		DistanceUtils.
		return c * GeometryHelper.EARTH_MEAN_RADIUS_KM;
	}

	/* See http://williams.best.vwh.net/avform.htm#flat */
	public double getDistanceFlat(GeoLocation otherLoc) {
		// Using flat earth approximation
		double dlat = (Math.PI / 180) * (getLatitude() - otherLoc.getLatitude());
		double dlon = (Math.PI / 180) * (getLongitude() - otherLoc.getLongitude());
		double d_north = dlat * GeometryHelper.EARTH_EQUATORIAL_RADIUS;
		double d_east = dlon * Math.cos((Math.PI / 180) * otherLoc.getLatitude()) * GeometryHelper.EARTH_EQUATORIAL_RADIUS;
		return Math.sqrt(d_north * d_north + d_east * d_east);
	}
	
	/**
	 * Returns the location as a "[label::]latitude,longitude" string. This is the same format as used by OpenTripPlanner.
	 * @returns The location in string format.
	 */
	@Override
	public String toString() {
		String latlong = String.format((Locale) null, "%.06f,%.06f", getLatitude(), getLongitude());
		return label != null ? label + "::" + latlong : latlong;
	}

	/**
	 * Create a GeoLocation from string format "[label::]latitude,longitude", the reverse process of the {@link toString}. 
	 * The Number format is not localized.
	 * @param labelLatlong the string value.
	 * @return A GeoLOcation object.
	 */
	public static GeoLocation fromString(String labelLatlong) {
		String latlong = labelLatlong;
		String label = null;
		String[] lableCoordsPair = labelLatlong.split("::", 2);
		if (lableCoordsPair.length == 2) {
			latlong = lableCoordsPair[1];
			label = lableCoordsPair[0];
		}
		String[] pair = latlong.split(",");
		if (pair.length != 2) {
			throw new IllegalArgumentException("Expected a pair of values: latitude, longitude");
		}
		double latitude = Double.parseDouble(pair[0]);
		double longitude = Double.parseDouble(pair[1]);
		return new GeoLocation(latitude, longitude, label);
	}

	/**
	 * Calculates the angle of the position w.r.t. the other point. East is 0.
	 * @return the angle in radians.
	 */
	public double getAngle(GeoLocation origin) {
		return Angle.angle(origin.getPoint().getCoordinate(), getPoint().getCoordinate());
	}
	
	/**
	 * Formula: 	θ = atan2( sin Δλ ⋅ cos φ2 , cos φ1 ⋅ sin φ2 − sin φ1 ⋅ cos φ2 ⋅ cos Δλ )
	 * where 	φ1,λ1 is the start point, φ2,λ2 the end point (Δλ is the difference in longitude)
	 * JavaScript: (all angles in radians)
	 * var y = Math.sin(λ2-λ1) * Math.cos(φ2);
	 * var x = Math.cos(φ1)*Math.sin(φ2) - Math.sin(φ1)*Math.cos(φ2)*Math.cos(λ2-λ1);
	 * var brng = Math.atan2(y, x).toDegrees();	 
	 * @return the angle in radians.
	 */
	public double getInitialBearing(GeoLocation origin) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, point);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		GeoLocation other = (GeoLocation) obj;
		return Objects.equals(label, other.label) && Objects.equals(point, other.point);
	}

}
