package eu.netmobiel.opentripplanner.api.model;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import eu.netmobiel.commons.api.EncodedPolylineBean;

/** 
* A Place is where a journey starts or ends, or a transit stop along the way.
*/ 
public class Place {

    /** 
     * For transit stops, the name of the stop.  For points of interest, the name of the POI.
     */
    public String name = null;

    /** 
     * The ID of the stop. This is often something that users don't care about.
     */
    public String stopId = null;

    /** 
     * The "code" of the stop. Depending on the transit agency, this is often
     * something that users care about.
     */
    public String stopCode = null;

    /**
      * The code or name identifying the quay/platform the vehicle will arrive at or depart from
      *
    */
    public String platformCode = null;

    /**
     * The longitude of the place.
     */
    public Double lon = null;
    
    /**
     * The latitude of the place.
     */
    public Double lat = null;

    /**
     * The time the rider will arrive at the place.
     */
    public Instant arrival = null;

    /**
     * The time the rider will depart the place.
     */
    public Instant departure = null;

    public String orig;

    public String zoneId;

    /**
     * For transit trips, the stop index (numbered from zero from the start of the trip
     */
    public Integer stopIndex;

    /**
     * For transit trips, the sequence number of the stop. Per GTFS, these numbers are increasing.
     */
    public Integer stopSequence;

    /**
     * Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit stop)
     * Mostly used for better localization of bike sharing and P+R station names
     */
    public VertexType vertexType;

    /**
     * In case the vertex is of type Bike sharing station.
     */
    public String bikeShareId;

    /**
     * This is an optional field which can be used to distinguish among ways a passenger's
     * boarding or alighting at a stop can differ among services operated by a transit agency.
     * This will be "default" in most cases. Currently the only non-default values are for
     * GTFS-Flex board or alight types.
     */
    public BoardAlightType boardAlightType;

    /**
     * Board or alight area for flag stops
     */
    public EncodedPolylineBean flagStopArea;

    public Place() {
    }

    public Place(Double lon, Double lat, String name) {
        this.lon = lon;
        this.lat = lat;
        this.name = name;
	    this.vertexType = VertexType.NORMAL;
    }

    public Place(Double lon, Double lat, String name, Instant arrival, Instant departure) {
        this(lon, lat, name);
        this.arrival = arrival;
        this.departure = departure;
    }

    public Place(Place other) {
    	this.arrival = other.arrival;
    	this.bikeShareId = other.bikeShareId;
    	this.boardAlightType = other.boardAlightType;
    	this.departure = other.departure;
    	this.flagStopArea = other.flagStopArea;
    	this.lat = other.lat;
    	this.lon = other.lon;
    	this.name = other.name;
    	this.orig = other.orig;
    	this.platformCode = other.platformCode;
    	this.stopCode = other.stopCode;
    	this.stopId = other.stopId;
    	this.stopIndex = other.stopIndex;
    	this.stopSequence = other.stopSequence;
    	this.vertexType = other.vertexType;
    	this.zoneId = other.zoneId;
    }
    
    public Place copy() {
    	return new Place(this);
    }
    
    private static String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atOffset(ZoneOffset.UTC));
    }
    
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.000000");
		StringBuilder builder = new StringBuilder();
		builder.append("Place [");
		builder.append(name).append(" ");
		builder.append(df.format(lat)).append(", ");
		builder.append(df.format(lon)).append(" ");
		if (arrival != null) {
			builder.append("A ").append(formatTime(arrival)).append(" ");
		}
		if (departure != null) {
			builder.append("D ").append(formatTime(departure));
		}
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(arrival, departure, lat, lon, name);
	}

	/**
	 * Temporal Spatial and name equals.
	 */
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
		Place other = (Place) obj;
		return Objects.equals(arrival, other.arrival) && Objects.equals(departure, other.departure)
				&& Objects.equals(lat, other.lat) && Objects.equals(lon, other.lon) && Objects.equals(name, other.name);
	}

	public boolean spatialEquals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Place other = (Place) obj;
		return Objects.equals(lat, other.lat) && Objects.equals(lon, other.lon);
	}

//	public GeoLocation toGeoLocation() {
//		return new GeoLocation(this.lat, this.lon, this.name);
//	}
//
//	public static OtpPlace fromGeoLocation(GeoLocation gl) {
//		return new OtpPlace(gl.getLongitude(), gl.getLatitude(), gl.getLabel());
//	}
}
