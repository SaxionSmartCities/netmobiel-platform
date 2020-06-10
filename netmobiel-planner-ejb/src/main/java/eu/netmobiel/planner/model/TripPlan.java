package eu.netmobiel.planner.model;

import java.time.Instant;
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

    /**  
     * The time and date of departure. At least one of departure or arrival must be defined. 
     */
    private Instant departureTime;

    /**  
     * The time and date of arrival. At least one of departure or arrival must be defined. 
     */
    private Instant arrivalTime;
    
    /** 
     * The origin location. 
     */
    private GeoLocation from;
    
    /**
     * The destination location.
     */
    private GeoLocation to;
    
    /**
     * The eligible traverse modes.
     */
    private TraverseMode[] traverseModes;

    /**
     * Maximum walking distance 
     */
    private Integer maxWalkDistance;

    /**
     * Maximum number of transfers
     */
    private Integer maxTransfers;

    /**
     * If true then rideshare is an option as first leg in a multi-leg trip with public transport.
     */
    private Boolean firstLegRideshare;

    /**
     * If true then rideshare is an option as last leg in a multi-leg trip with public transport.
     */
    private Boolean lastLegRideshare;

    /**
     * Numbers of seats required.
     */
    private Integer nrSeats;
    /** 
     * A list of possible itineraries. 
     */
    private List<Itinerary> itineraries = new ArrayList<>();

    public TripPlan() { }

    public TripPlan(GeoLocation from, GeoLocation to, Instant departureTime, Instant arrivalTime, 
    		TraverseMode[] traverseModes, Integer maxWalkDistance, Integer nrSeats) {
        this.from = from;
        this.to = to;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.traverseModes = traverseModes;
        this.maxWalkDistance = maxWalkDistance;
        this.nrSeats = nrSeats; 
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

	public Instant getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(Instant departureTime) {
		this.departureTime = departureTime;
	}

	public Instant getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Instant arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public TraverseMode[] getTraverseModes() {
		return traverseModes;
	}

	public void setTraverseModes(TraverseMode[] traverseModes) {
		this.traverseModes = traverseModes;
	}

	public Integer getMaxWalkDistance() {
		return maxWalkDistance;
	}

	public void setMaxWalkDistance(Integer maxWalkDistance) {
		this.maxWalkDistance = maxWalkDistance;
	}

	public Integer getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(Integer nrSeats) {
		this.nrSeats = nrSeats;
	}

//	private String formatTime(OffsetDateTime dt) {
//    	return dt != null ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dt) : "*";
//    }

	private String formatTime(Instant instant) {
    	return instant != null ? DateTimeFormatter.ISO_INSTANT.format(instant) : "*";
    }
    
    @Override
	public String toString() {
		return String.format("TripPlan [%s - %s from %s to %s \n\t%s]", 
				formatTime(departureTime), formatTime(arrivalTime), from, to, 
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

	public Integer getMaxTransfers() {
		return maxTransfers;
	}

	public void setMaxTransfers(Integer maxTransfers) {
		this.maxTransfers = maxTransfers;
	}

	public Boolean getFirstLegRideshare() {
		return firstLegRideshare;
	}

	public void setFirstLegRideshare(Boolean firstLegRideshare) {
		this.firstLegRideshare = firstLegRideshare;
	}

	public Boolean getLastLegRideshare() {
		return lastLegRideshare;
	}

	public void setLastLegRideshare(Boolean lastLegRideshare) {
		this.lastLegRideshare = lastLegRideshare;
	}

}
