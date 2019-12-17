package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netmobiel.commons.model.GeoLocation;

@MappedSuperclass
@Vetoed
@Access(AccessType.FIELD)
public class Itinerary implements Serializable {

	private static final long serialVersionUID = 509814730629943904L;

    private static final Logger log = LoggerFactory.getLogger(Itinerary.class);

    @NotNull
    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @NotNull
    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    /**
     * Duration of the trip on this itinerary, in seconds.
     */
    @Basic
    private Integer duration;

    /**
     * The number of transfers this trip has.
     */
    @Basic
    private Integer transfers;
    
    /**
     * How much time is spent walking, in seconds.
     */
    @Column(name = "walk_time")
    private Integer walkTime;
    /**
     * How much time is spent on transit, in seconds.
     */
    @Column(name = "transit_time")
    private Integer transitTime;
    /**
     * How much time is spent waiting for transit and other modalities to arrive, in seconds.
     */
    @Column(name = "waiting_time")
    private Integer waitingTime;

    /**
     * How far the user has to walk, in meters.
     */
    @Column(name = "walk_distance")
    private Integer walkDistance;

    /**
     * The legs in this itinerary.
     */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "trip", foreignKey = @ForeignKey(name = "leg_trip_fk"))
	@OrderColumn(name = "leg_ix")
	private List<Leg> legs;

	@Transient
    public Double score;
    
	public Itinerary() {
    	
    }

    public Itinerary(Itinerary other) {
		this.duration = other.duration;
		this.departureTime = other.departureTime;
		this.arrivalTime = other.arrivalTime;
		this.walkTime = other.walkTime;
		this.transitTime = other.transitTime;
		this.waitingTime = other.waitingTime;
		this.walkDistance = other.walkDistance;
		this.transfers = other.transfers;
		this.legs = new ArrayList<>(other.legs);
    }
    
    public Itinerary copy() {
    	return new Itinerary(this);
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

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}


	public Integer getTransfers() {
		return transfers;
	}

	public void setTransfers(Integer transfers) {
		this.transfers = transfers;
	}

	public Integer getWalkTime() {
		return walkTime;
	}

	public void setWalkTime(Integer walkTime) {
		this.walkTime = walkTime;
	}

	public Integer getWaitingTime() {
		return waitingTime;
	}

	public void setWaitingTime(Integer waitingTime) {
		this.waitingTime = waitingTime;
	}

	public Integer getWalkDistance() {
		return walkDistance;
	}

	public void setWalkDistance(Integer walkDistance) {
		this.walkDistance = walkDistance;
	}

	public List<Leg> getLegs() {
		return legs;
	}

	public void setLegs(List<Leg> legs) {
		this.legs = legs;
	}

    public Integer getTransitTime() {
		return transitTime;
	}

	public void setTransitTime(Integer transitTime) {
		this.transitTime = transitTime;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

//    private String formatTime(Instant instant) {
//    	return DateTimeFormatter.ISO_TIME.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
//    }
//    
//
//	@Override
//	public String toString() {
//		return String.format("Itinerary [D %s A %s from %s to %s]",
//				DateTimeFormatter.ISO_DATE_TIME.format(departureTime), DateTimeFormatter.ISO_DATE_TIME.format(departureTime));
//	}

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
    
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Itinerary [");
		if (score != null) {
			builder.append(String.format("%.1f", score)).append(" *** ");
		}
		builder.append(formatTime(departureTime)).append(" ");
		builder.append(formatTime(arrivalTime)).append(" ");
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

	public Itinerary filterLegs(GeoLocation from, GeoLocation to) {
		Itinerary it = new Itinerary(this);
		int startIx = -1;
		int endIx = it.legs.size();
		for (int ix = 0; ix < it.legs.size(); ix++) {
			Leg leg = it.legs.get(ix);
			if (startIx < 0) {
				// If within x meter it must be the start (a bit shady)
				if (from.getDistanceFlat(leg.getFrom().getLocation()) < 0.020) {
					startIx = ix;
				}
			} 
			// If within x meter it must be the end
			if (startIx >= 0 && to.getDistanceFlat(leg.getTo().getLocation()) < 0.020) {
				endIx = ix + 1;
				break;
			}
		}
		if (startIx < 0) {
			log.warn("Unable to find starting position of car leg, using whole itinerary");
			startIx = 0;
		}
		it.legs = it.legs.subList(startIx, endIx);
		return it;
		
	}
	
	public List<Itinerary> appendTransits(Collection<Itinerary> transitItineraries) {
		List<Itinerary> carTransitIts = new ArrayList<>();
		for (Itinerary transitIt: transitItineraries) {
			Itinerary mmit = new Itinerary(this); 
			mmit.legs.addAll(transitIt.legs);
			mmit.arrivalTime = transitIt.arrivalTime;
			mmit.duration = Math.toIntExact(Duration.between(mmit.departureTime, mmit.arrivalTime).getSeconds());
			mmit.transfers = transitIt.transfers + 1;
			mmit.transitTime = transitIt.transitTime;
			double legsDuration = transitIt.legs.stream().mapToDouble(leg -> leg.getDuration()).sum();
			mmit.waitingTime = Math.toIntExact(Math.round(mmit.duration - legsDuration));
			mmit.walkDistance = transitIt.walkDistance;
			mmit.walkTime = transitIt.walkTime;
	    	carTransitIts.add(mmit);
		}
		return carTransitIts;
	}

	public List<Itinerary> prependTransits(Collection<Itinerary> transitItineraries) {
		List<Itinerary> carTransitIts = new ArrayList<>();
		for (Itinerary transitIt: transitItineraries) {
			Itinerary mmit = new Itinerary(this); 
			mmit.legs.addAll(0, transitIt.legs);
			mmit.departureTime = transitIt.departureTime;
			mmit.duration = Math.toIntExact(Duration.between(mmit.departureTime, mmit.arrivalTime).getSeconds());
			mmit.transfers = transitIt.transfers + 1;
			mmit.transitTime = transitIt.transitTime;
			double legsDuration = transitIt.legs.stream().mapToDouble(leg -> leg.getDuration()).sum();
			mmit.waitingTime = Math.toIntExact(Math.round(mmit.duration - legsDuration));
			mmit.walkDistance = transitIt.walkDistance;
			mmit.walkTime = transitIt.walkTime;
	    	carTransitIts.add(mmit);
		}
		return carTransitIts;
	}

}
