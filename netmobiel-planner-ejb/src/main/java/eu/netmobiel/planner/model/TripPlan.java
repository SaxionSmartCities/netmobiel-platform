package eu.netmobiel.planner.model;

import java.time.Instant;
import java.time.OffsetDateTime;
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
    private OffsetDateTime requestedDepartureTime;

    private OffsetDateTime requestedArrivalTime;
    
    /** The origin */
    private GeoLocation from;
    
    /** The destination */
    private GeoLocation to;

    /** A list of possible itineraries */
    private List<Itinerary> itineraries = new ArrayList<>();

    public TripPlan() { }

    public TripPlan(GeoLocation from, GeoLocation to, OffsetDateTime requestedDepartureTime, OffsetDateTime requestedArrivalTime) {
        this.from = from;
        this.to = to;
        this.requestedDepartureTime = requestedDepartureTime;
        this.requestedArrivalTime = requestedArrivalTime;
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

	
	public OffsetDateTime getRequestedDepartureTime() {
		return requestedDepartureTime;
	}

	public void setRequestedDepartureTime(OffsetDateTime requestedDepartureTime) {
		this.requestedDepartureTime = requestedDepartureTime;
	}

	public OffsetDateTime getRequestedArrivalTime() {
		return requestedArrivalTime;
	}

	public void setRequestedArrivalTime(OffsetDateTime requestedArrivalTime) {
		this.requestedArrivalTime = requestedArrivalTime;
	}

	private String formatTime(OffsetDateTime dt) {
    	return dt != null ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dt) : "";
    }
    
    @Override
	public String toString() {
		return String.format("TripPlan [<%s, %s> from %s to %s \n\t%s]", 
				formatTime(requestedDepartureTime), formatTime(requestedArrivalTime), from, to, 
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
