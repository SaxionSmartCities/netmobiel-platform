package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.opentripplanner.client.OpenTripPlannerClient;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@NamedEntityGraph(
		name = Itinerary.LIST_ITINERARIES_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "stops"),		
				@NamedAttributeNode(value = "legs", subgraph = "leg-details")		
		}, subgraphs = {
				// Without this subgraph no leg details are retrieved
				@NamedSubgraph(
						name = "leg-details",
						attributeNodes = {
								@NamedAttributeNode(value = "guideSteps")
						}
					)
				}

	)
@NamedEntityGraph(
		name = Itinerary.LIST_ITINERARY_DETAIL_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "stops"),		
				@NamedAttributeNode(value = "legs", subgraph = "leg-details")		
		}, subgraphs = {
				// Without this subgraph no leg details are retrieved
				@NamedSubgraph(
						name = "leg-details",
						attributeNodes = {
								@NamedAttributeNode(value = "guideSteps")
						}
					)
				}

	)

@Entity
@Table(name = "itinerary")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "itinerary_sg", sequenceName = "itinerary_id_seq", allocationSize = 1, initialValue = 50)
public class Itinerary implements Serializable {

	private static final long serialVersionUID = 509814730629943904L;
	public static final String URN_PREFIX = PlannerUrnHelper.createUrnPrefix(Itinerary.class);
	public static final String LIST_ITINERARIES_ENTITY_GRAPH = "list-itineraries-graph";
	public static final String LIST_ITINERARY_DETAIL_ENTITY_GRAPH = "list-itinerary-detail-graph";

    private static final Logger log = LoggerFactory.getLogger(Itinerary.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "itinerary_sg")
    private Long id;

    @Transient
    private String itineraryRef;
    

    /**  
     * The time and date of departure in this itinerary. Is by definition the same as the departure time from the first stop.   
     */
    @NotNull
    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    /**  
     * The time and date of arrival in this itinerary. Is by definition the same as the arrival time at the last stop.   
     */
    @NotNull
    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    /**
     * Duration of the trip on this itinerary, in seconds. Why is this field present? Because it is much easier to use in JPQL queries.
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
     * The stops (vertices) in this itinerary.
     */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "itinerary", foreignKey = @ForeignKey(name = "stop_itinerary_fk"), nullable = false)
	@OrderColumn(name = "stop_ix")
	private List<Stop> stops;

	/**
     * The legs (edges) in this itinerary.
     */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "itinerary", foreignKey = @ForeignKey(name = "leg_itinerary_fk"), nullable = false)
	@OrderColumn(name = "leg_ix")
	private List<Leg> legs;

    @Column(name = "score")
    private Double score;
   
	/** 
	 * The plan this itinerary is part of.
	 */
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan", foreignKey = @ForeignKey(name = "itinerary_plan_fk"), nullable = false)
	private TripPlan plan;


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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getItineraryRef() {
    	if (itineraryRef == null) {
    		itineraryRef = PlannerUrnHelper.createUrn(Itinerary.URN_PREFIX, getId());
    	}
		return itineraryRef;
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

	public List<Stop> getStops() {
		if (stops == null) {
			stops = new ArrayList<>();
		}
		return stops;
	}

	public void setStops(List<Stop> stops) {
		this.stops = stops;
	}

	public List<Leg> getLegs() {
		if (legs == null) {
			legs = new ArrayList<>();
		}
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

	public TripPlan getPlan() {
		return plan;
	}

	public void setPlan(TripPlan plan) {
		this.plan = plan;
	}

	/**
	 * Searches through the legs of this trip for a leg with a specific tripId. The trip id is a reference from the transport provider
	 * and refers to a specific ride of a vehicle, both in rideshare and in public transport. In public transport the tripId refers 
	 * to a specific instance of a ride over a route in a day, but not necessarily unique over time. In rideshare the tripId is an unique id.  
	 * @param tripId the trip id to find
	 * @return An Optional with the leg containing the trip id or null if not found.  
	 */
	public Optional<Leg> findLegByTripId(String tripId) {
		Leg leg = null;
		if (getLegs() != null) {
			leg = getLegs().stream().filter(lg -> tripId.equals(lg.getTripId())).findFirst().orElse(null);
		}
		return Optional.ofNullable(leg);
	}

	/**
	 * Searches through the legs of this trip for a leg with a specific bookingId.  
	 * @param bookingId the booking id to find
	 * @return An Optional with the leg containing the booking id or null if not found.  
	 */
	public Optional<Leg> findLegByBookingId(String bookingId) {
		Leg leg = null;
		if (getLegs() != null) {
			leg = getLegs().stream().filter(lg -> bookingId.equals(lg.getBookingId())).findFirst().orElse(null);
		}
		return Optional.ofNullable(leg);
	}

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    public String toStringCompact() {
		StringBuilder builder = new StringBuilder();
		if (duration != null) {
			builder.append("Duration ").append(duration).append("s ");
		}
		if (walkTime != null) {
			builder.append("Walk ").append(walkTime).append("s ");
		}
		if (walkDistance != null) {
			builder.append(Math.round(walkDistance)).append("m ");
		}
		if (transitTime != null) {
			builder.append("Transit ").append(transitTime).append("s ");
		}
		if (waitingTime != null) {
			builder.append("Waiting ").append(waitingTime).append("s ");
		}
		if (transfers != null) {
			builder.append("Transfers ").append(transfers).append(" ");
		}
//		builder.append("\n");
		if (legs != null) {
//			builder.append("\t\t").append(legs.stream().map(leg -> leg.toString()).collect(Collectors.joining("\n\t\t")));
			Stop previous = null;
			for (Leg leg : legs) {
				if (previous == null) {
					builder.append("\n\t\t").append(leg.getFrom());
				} else if (! previous.equals(leg.getFrom())) {
					builder.append("\n\t\t").append(leg.getFrom());
				}
				builder.append("\n\t\t\t").append(leg);
				builder.append("\n\t\t").append(leg.getTo());
				previous = leg.getTo();
			}
		}
		return builder.toString();
    }
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Itinerary ");
		if (score != null) {
			builder.append(String.format("%.1f", score)).append(" *** ");
		}
		builder.append(formatTime(departureTime)).append(" ");
		builder.append(formatTime(arrivalTime)).append(" ");
		builder.append(toStringCompact());
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
				if (from.getDistanceFlat(leg.getFrom().getLocation()) < (OpenTripPlannerClient.MINIMUM_PLANNING_DISTANCE_METERS * 2 / 1000.0)) {
					startIx = ix;
				}
			} 
			// If within x meter it must be the end
			if (startIx >= 0 && to.getDistanceFlat(leg.getTo().getLocation()) < (OpenTripPlannerClient.MINIMUM_PLANNING_DISTANCE_METERS * 2 / 1000.0)) {
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
