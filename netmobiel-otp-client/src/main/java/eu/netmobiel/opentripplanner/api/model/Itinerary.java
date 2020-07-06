package eu.netmobiel.opentripplanner.api.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An Itinerary is one complete way of getting from the start location to the end location.
 */
public class Itinerary {

//    private static final Logger log = LoggerFactory.getLogger(OtpItinerary.class);

	/**
     * Duration of the trip on this itinerary, in seconds.
     */
    public Long duration = 0L;

    /**
     * Time that the trip departs.
     */
    public Instant startTime = null;
    /**
     * Time that the trip arrives.
     */
    public Instant endTime = null;

    /**
     * How much time is spent walking, in seconds.
     */
    public Long walkTime;
    /**
     * How much time is spent on transit, in seconds.
     */
    public Long transitTime;
    /**
     * How much time is spent waiting for transit to arrive, in seconds.
     */
    public Long waitingTime;

    /**
     * How far the user has to walk, in meters.
     */
    public Double walkDistance;
    
    /**
     * Indicates that the walk limit distance has been exceeded for this itinerary when true.
     */
    public Boolean walkLimitExceeded;

    /**
     * How much elevation is lost, in total, over the course of the trip, in meters. As an example,
     * a trip that went from the top of Mount Everest straight down to sea level, then back up K2,
     * then back down again would have an elevationLost of Everest + K2.
     */
    public Double elevationLost;
    /**
     * How much elevation is gained, in total, over the course of the trip, in meters. See
     * elevationLost.
     */
    public Double elevationGained;

    /**
     * The number of transfers this trip has.
     */
    public Integer transfers;

//    /**
//     * The cost of this trip
//     */
//    public OtpFare fare = new OtpFare();

    /**
     * A list of Legs. Each Leg is either a walking (cycling, car) portion of the trip, or a transit
     * trip on a particular vehicle. So a trip where the use walks to the Q train, transfers to the
     * 6, then walks to their destination, has four legs.
     */
    public List<Leg> legs = new ArrayList<>();

    /**
     * This itinerary has a greater slope than the user requested (but there are no possible 
     * itineraries with a good slope). 
     */
    public Boolean tooSloped = false;

    public Itinerary() {
    	
    }

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atOffset(ZoneOffset.UTC));
    }
    
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Itinerary [");
		builder.append(formatTime(startTime)).append(" ");
		builder.append(formatTime(endTime)).append(" ");
		builder.append(duration).append(" [s] ");
		builder.append("Walk ").append(walkTime).append("s ");
		if (walkDistance != null) {
			builder.append(Math.round(walkDistance)).append("m ");
		}
		if (transitTime > 0) {
			builder.append("transit ").append(transitTime).append("s ");
		}
		if (waitingTime > 0) {
			builder.append("waiting ").append(waitingTime).append("s ");
		}
		if (transfers != null && transfers > 0) {
			builder.append("transfers=").append(transfers).append(" ");
		}
		builder.append("\n");
		if (legs != null) {
			builder.append("\t\t").append(legs.stream().map(i -> i.toString()).collect(Collectors.joining("\n\t\t")));
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Recalculates some characteristics of an itinerary from the legs.
	 */
	public void updateCharacteristics( ) {
		walkDistance = legs.stream().filter(leg -> leg.mode == TraverseMode.WALK).mapToDouble(leg -> leg.distance).sum(); 
		walkTime = Math.round(legs.stream().filter(leg -> leg.mode == TraverseMode.WALK).mapToDouble(leg -> leg.getDuration()).sum()); 

	}
}
