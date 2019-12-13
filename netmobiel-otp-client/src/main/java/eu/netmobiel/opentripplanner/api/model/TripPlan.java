package eu.netmobiel.opentripplanner.api.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A TripPlan is a set of ways to get from point A to point B at time T.
 */
public class TripPlan {

    /**  The time and date of travel. Is either departure time or arrival time */
    public Instant date = null;
    
    /** The origin */
    public Place from = null;
    
    /** The destination */
    public Place to = null;

    /** A list of possible itineraries */
    public List<Itinerary> itineraries = new ArrayList<>();

    public TripPlan() { }

    public TripPlan(Place from, Place to, Instant date) {
        this.from = from;
        this.to = to;
        this.date = date;
    }

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
    
    @Override
	public String toString() {
		return String.format("TripPlan [%s from %s to %s \n\t%s]", formatTime(date), from, to, 
				itineraries.stream().map(i -> i.toString()).collect(Collectors.joining("\n\t")));
	}

	/**
	 * Search the itinerary and return the earliest departure time
	 * @return a Date
	 */
    public Instant getEarliestDeparture() {
    	return itineraries.stream()
    			.map(it -> it.startTime)
    			.min(Instant::compareTo)
    			.orElse(null);
    }

}
