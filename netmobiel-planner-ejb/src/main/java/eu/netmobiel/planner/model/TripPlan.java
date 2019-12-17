package eu.netmobiel.planner.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.bind.annotation.JsonbTransient;

import eu.netmobiel.commons.model.GeoLocation;

/**
 * A TripPlan is a set of ways to get from point A to point B at time T.
 */
public class TripPlan {

    /**  The time and date of travel. Is either departure time or arrival time */
    private Instant date;

    private boolean isArrivalTime;
    
    /** The origin */
    private GeoLocation from;
    
    /** The destination */
    private GeoLocation to;

    /** A list of possible itineraries */
    private List<Itinerary> itineraries = new ArrayList<>();

    public TripPlan() { }

    public TripPlan(GeoLocation from, GeoLocation to, Instant date, boolean isArrival) {
        this.from = from;
        this.to = to;
        this.date = date;
        this.isArrivalTime = isArrival;
    }

	public Instant getDate() {
		return date;
	}

	public void setDate(Instant date) {
		this.date = date;
	}

	public boolean isArrivalTime() {
		return isArrivalTime;
	}

	public void setArrivalTime(boolean isArrivalTime) {
		this.isArrivalTime = isArrivalTime;
	}

	public GeoLocation getFrom() {
		return from;
	}

	public void setFrom(GeoLocation from) {
		this.from = from;
	}

	public GeoLocation getTo() {
		return to;
	}

	public void setTo(GeoLocation to) {
		this.to = to;
	}

	public List<Itinerary> getItineraries() {
		return itineraries;
	}

	public void setItineraries(List<Itinerary> itineraries) {
		this.itineraries = itineraries;
	}

	private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
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
	@JsonbTransient
    public Instant getEarliestDeparture() {
    	return itineraries.stream()
    			.map(it -> it.getDepartureTime())
    			.min(Instant::compareTo)
    			.orElse(null);
    }

}
