package eu.netmobiel.opentripplanner.api.model;


import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.netmobiel.commons.api.EncodedPolylineBean;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle (or on foot).
 */

public class Leg {

    /**
     * The date and time this leg begins.
     */
    public Instant startTime;
    
    /**
     * The date and time this leg ends.
     */
    public Instant endTime;
    
    /**
     * For transit leg, the offset from the scheduled departure-time of the boarding stop in this leg.
     * "scheduled time of departure at boarding stop" = startTime - departureDelay
     */
    public Integer departureDelay;
    /**
     * For transit leg, the offset from the scheduled arrival-time of the alighting stop in this leg.
     * "scheduled time of arrival at alighting stop" = endTime - arrivalDelay
     */
    public Integer arrivalDelay;
    /**
     * Whether there is real-time data about this Leg
     */
    public Boolean realTime = false;
    
    /**
     * Is this a frequency-based trip with non-strict departure times?
     */
    public Boolean isNonExactFrequency = null;
    
    /**
     * The best estimate of the time between two arriving vehicles. This is particularly important 
     * for non-strict frequency trips, but could become important for real-time trips, strict 
     * frequency trips, and scheduled trips with empirical headways.
     */
    public Integer headway = null;
    
    /**
     * The distance traveled while traversing the leg in meters.
     */
    public Double distance = null;
    
    /**
     * Is this leg a traversing pathways?
     */
    public Boolean pathway = false;

    /**
     * The mode (e.g., <code>Walk</code>) used when traversing this leg.
     */
    public TraverseMode mode;

    /**
     * For transit legs, the route of the bus or train being used. For non-transit legs, the name of
     * the street being traversed.
     */
    public String route = "";

    public String agencyName;

    public String agencyUrl;

    public String agencyBrandingUrl;

    public Integer agencyTimeZoneOffset;

    /**
     * For transit leg, the route's (background) color (if one exists). For non-transit legs, null.
     */
    public String routeColor = null;

    /**
     * For transit legs, the type of the route. Non transit -1
     * When 0-7: 0 Tram, 1 Subway, 2 Train, 3 Bus, 4 Ferry, 5 Cable Car, 6 Gondola, 7 Funicular
     * When equal or highter than 100, it is coded using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard
     * Also see http://groups.google.com/group/gtfs-changes/msg/ed917a69cf8c5bef
     */
    public Integer routeType = null;
    
    /**
     * For transit legs, the ID of the route.
     * For non-transit legs, null.
     */
    public String routeId = null;

    /**
     * For transit leg, the route's text color (if one exists). For non-transit legs, null.
     */
    public String routeTextColor = null;

    /**
     * For transit legs, if the rider should stay on the vehicle as it changes route names.
     */
    public Boolean interlineWithPreviousLeg;

    
    /**
     * For transit leg, the trip's short name (if one exists). For non-transit legs, null.
     */
    public String tripShortName = null;

    /**
     * For transit leg, the trip's block ID (if one exists). For non-transit legs, null.
     */
    public String tripBlockId = null;
    
    /**
     * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
     */
    public String headsign = null;

    /**
     * For transit legs, the ID of the transit agency that operates the service used for this leg.
     * For non-transit legs, null.
     */
    public String agencyId = null;
    
    /**
     * For transit legs, the ID of the trip.
     * For non-transit legs, null.
     */
    public String tripId = null;
    
    /**
     * For transit legs, the service date of the trip.
     * For non-transit legs, null.
     */
    public String serviceDate = null;

     /**
      * For transit leg, the route's branding URL (if one exists). For non-transit legs, null.
      */
     public String routeBrandingUrl = null;

     /**
     * The Place where the leg originates.
     */
    public Place from = null;
    
    /**
     * The Place where the leg begins.
     */
    public Place to = null;

    /**
     * For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends.
     * For non-transit legs, null.
     * This field is optional i.e. it is always null unless "showIntermediateStops" parameter is set to "true" in the planner request.
     */
    public List<Place> intermediateStops;

    /**
     * The leg's geometry.
     */
    public EncodedPolylineBean legGeometry;

    /**
     * A series of turn by turn instructions used for walking, biking and driving. 
     */
    @JsonProperty("steps")
    public List<WalkStep> walkSteps;

    public List<LocalizedAlert> alerts;

    public String routeShortName;

    public String routeLongName;

    public String boardRule;

    public String alightRule;

    public Boolean rentedBike;

     /**
      * True if this is a call-and-ride leg.
      */
    public Boolean callAndRide;

    /* For call-n-ride leg, supply maximum start time based on calculation. */
    public Instant flexCallAndRideMaxStartTime = null;

     /* For call-n-ride leg, supply minimum end time based on calculation. */
    public Instant flexCallAndRideMinEndTime = null;

    /** trip.drt_advance_book_min if this is a demand-response leg */
    public double flexDrtAdvanceBookMin;

     /**
      *  Agency message if this is leg has a demand-response pickup and the Trip has
      *  `drt_pickup_message` defined.
      */
    public String flexDrtPickupMessage;

     /**
      * Agency message if this is leg has a demand-response dropoff and the Trip has
      * `drt_drop_off_message` defined.
      */
    public String flexDrtDropOffMessage;

     /**
      * Agency message if this is leg has a flag stop pickup and the Trip has
      * `continuous_pickup_message` defined.
      */
    public String flexFlagStopPickupMessage;

     /**
      * Agency message if this is leg has a flag stop dropoff and the Trip has
      * `continuous_drop_off_message` defined.
      */
    public String flexFlagStopDropOffMessage;

    public Leg() {
    }

	/**
     * Whether this leg is a transit leg or not.
     * @return Boolean true if the leg is a transit leg
     */
    public Boolean isTransitLeg() {
        if (mode == null) return null;
        else if (mode == TraverseMode.WALK) return false;
        else if (mode == TraverseMode.CAR) return false;
        else if (mode == TraverseMode.BICYCLE) return false;
        else return true;
    }
    
    /** 
     * The leg's duration in seconds
     */
    public double getDuration() {
        return Duration.between(startTime, endTime).getSeconds();
    }

    public void setTimeZone(TimeZone timeZone) {
        agencyTimeZoneOffset = timeZone.getOffset(startTime.getEpochSecond() * 1000);
    }

    private static String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atOffset(ZoneOffset.UTC));
    }
    
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Leg [");
		builder.append(formatTime(startTime)).append(" ");
		builder.append(formatTime(endTime)).append(" ");
		builder.append(getDuration()).append("s\n");
		
		builder.append("\t\t\tFrom ").append(from).append(" ");
		builder.append("To ").append(to).append(" ");
		builder.append(Math.round(distance)).append("m\n");
		
		
		builder.append("\t\t\tdelays D ").append(departureDelay).append(", ");
		builder.append("A ").append(arrivalDelay).append(", ");
		builder.append(mode).append(" ");
		if (routeLongName != null) {
			builder.append(route).append(" ").append(routeLongName);
		}
		if (intermediateStops != null && !intermediateStops.isEmpty()) {
			builder.append("\n\t\t\t").append(intermediateStops.stream().map(p -> p.toString()).collect(Collectors.joining("\n\t\t\t"))).append("");
		}
		builder.append("]");
		return builder.toString();
	}
    
}