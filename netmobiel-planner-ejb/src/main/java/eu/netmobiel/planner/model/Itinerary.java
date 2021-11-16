package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import eu.netmobiel.commons.util.ClosenessFilter;
import eu.netmobiel.commons.util.UrnHelper;
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
	public static final ClosenessFilter connectingStopCheck = 
			new ClosenessFilter(OpenTripPlannerClient.MINIMUM_PLANNING_DISTANCE_METERS * 2);

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
	@JoinColumn(name = "trip_plan", foreignKey = @ForeignKey(name = "itinerary_trip_plan_fk"), nullable = true)
	private TripPlan tripPlan;

    /**
     * The costs of travelling the itinerary in NetMobiel credits. This is the sum of the underlying leg costs.
     */
    @Column(name = "fare_credits")
    private Integer fareInCredits;
    
	public Itinerary() {
    	
    }

	/**
	 * Copy constructor, creates a shallow copy.
	 * @param other the itinerary to copy.
	 */
    public Itinerary(Itinerary other) {
		this.arrivalTime = other.arrivalTime;
		this.departureTime = other.departureTime;
		this.duration = other.duration;
		this.fareInCredits = other.fareInCredits;
		this.transfers = other.transfers;
		this.transitTime = other.transitTime;
		this.waitingTime = other.waitingTime;
		this.walkDistance = other.walkDistance;
		this.walkTime = other.walkTime;
		if (other.legs != null) {
			this.legs = new ArrayList<>(other.legs);
		}
		if (other.stops!= null) {
			this.stops = new ArrayList<>(other.stops);
		}
    }
    
    public Itinerary copy() {
    	return new Itinerary(this);
    }

    public Itinerary deepCopy() {
    	// shallow copy
    	Itinerary copy = new Itinerary(this);
    	List<Leg> newLegs = copy.getLegs();
    	// now convert to deep copy
    	copy.setLegs(new ArrayList<>());
    	copy.getStops().clear();
    	Stop lastStop = null;
    	for (Leg oldLeg : newLegs) {
    		// Deep copy, convert to graph
			Leg newLeg = new Leg(oldLeg);
			if (lastStop != null) {
				newLeg.setFrom(lastStop);
			}
			lastStop = newLeg.getTo();
			copy.getLegs().add(newLeg);
		}
    	copy.getStops().add(copy.getLegs().get(0).getFrom());
		copy.getLegs().forEach(leg -> copy.getStops().add(leg.getTo()));
    	return copy;
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public GeoLocation getFrom() {
		return getLegs().isEmpty() ? null : getLegs().get(0).getFrom().getLocation(); 
	}

	public GeoLocation getTo() {
		return getLegs().isEmpty() ? null : getLegs().get(getLegs().size() - 1).getTo().getLocation(); 
	}

	public String getItineraryRef() {
    	if (itineraryRef == null) {
    		itineraryRef = UrnHelper.createUrn(Itinerary.URN_PREFIX, getId());
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

	public TripPlan getTripPlan() {
		return tripPlan;
	}

	public void setTripPlan(TripPlan plan) {
		this.tripPlan = plan;
	}

	public Integer getFareInCredits() {
		return fareInCredits;
	}

	public void setFareInCredits(Integer fareInCredits) {
		this.fareInCredits = fareInCredits;
	}

	public Set<TraverseMode> getModalities() {
		return getLegs().stream()
				.map(leg -> leg.getTraverseMode())
				.collect(Collectors.toSet());
	}

	public boolean isConfirmationRequested() {
		return getLegs().stream()
				.map(leg -> leg.isConfirmationRequested() || leg.isConfirmationByProviderRequested())
				.reduce(Boolean.FALSE, Boolean::logicalOr);
	}
	
	public List<Leg> findLegsToConfirm() {
		return getLegs().stream()
				.filter(leg -> leg.isConfirmationRequested() || leg.isConfirmationByProviderRequested())
				.collect(Collectors.toList());
	}

	public boolean isConfirmationComplete() {
		// The confirmation is complete when both parties have answered for all relevant legs, i.e. when there is not at least one unanswered question.
		return ! getLegs().stream()
				.filter(leg -> (leg.isConfirmationRequested() && leg.getConfirmed() == null) || 
						       (leg.isConfirmationByProviderRequested() && leg.getConfirmedByProvider() == null))
				.findFirst()
				.isPresent();
	}

	public boolean isWalkOnly() {
		// Walk only if no legs with other traverse modes are found
		return !getLegs().stream()
				.anyMatch(leg -> leg.getTraverseMode() != TraverseMode.WALK);
	}

	public void updateFare() {
		Integer fare = getLegs().stream()
	    		.filter(leg -> leg.getFareInCredits() != null)
	    		.mapToInt(leg -> leg.getFareInCredits())
	    		.sum();
    	if (fare > 0) {
    		setFareInCredits(fare);
    	}
	}

	public int getTotalCarDistance() {
		return getLegs().stream()
	    		.filter(leg -> leg.getTraverseMode() == TraverseMode.CAR || leg.getTraverseMode() == TraverseMode.RIDESHARE)
	    		.mapToInt(leg -> leg.getDistance())
	    		.sum();
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
	 * Searches through the legs of this trip for a leg with a specific driverId. The driver id is a urn reference to the driver of the vehicle 
	 * in rideshare. In public transport the driverId is null. 
	 * @param driverId the driver id to look for.
	 * @return An Optional with the leg containing the driver id.  
	 */
	public Optional<Leg> findLegByDriverId(String driverId) {
		Optional<Leg> leg = Optional.empty();
		if (getLegs() != null) {
			leg = getLegs().stream().filter(lg -> driverId.equals(lg.getDriverId())).findFirst();
		}
		return leg;
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

    private static String formatTime(Instant instant) {
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
		if (fareInCredits != null) {
			builder.append("Fare (cr) ").append(fareInCredits).append(" ");
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

	/**
	 * Waiting is the time spent waiting after arriving somewhere and before departing again, in seconds. 
	 */
	void updateWaitingTime() {
		waitingTime = getLegs().stream()
				.map(leg -> leg.getFrom().getWaitingTime())
				.collect(Collectors.summingInt(Integer::intValue));
	}

	/**
	 * WalkDistance is how far the traveller has to walk, in meters. 
	 */
	void updateWalkDistance() {
		walkDistance = getLegs().stream()
				.filter(leg -> leg.getTraverseMode() == TraverseMode.WALK)
				.map(leg -> leg.getDistance())
				.collect(Collectors.summingInt(Integer::intValue));
	}

	/**
	 * WalkTime is how much time is spent walking, in seconds. 
	 */
	void updateWalkTime() {
		walkTime = getLegs().stream()
				.filter(leg -> leg.getTraverseMode() == TraverseMode.WALK)
				.map(leg -> leg.getDuration())
				.collect(Collectors.summingInt(Integer::intValue));
	}

	/**
	 * TransitTime is how much time is spent in travelling with transit, in seconds. 
	 */
	void updateTransitTime() {
		transitTime = getLegs().stream()
				.filter(leg -> leg.getTraverseMode().isTransit())
				.map(leg -> leg.getDuration())
				.collect(Collectors.summingInt(Integer::intValue));
	}

	/**
	 * Transfers is the number of switches between different vehicles.
	 * It is calculated by counting the number of <traveseMode, tripId> combinations, minus 1.  
	 */
	public void updateTransfers() {
		Set<String> transferNames = getLegs().stream()
				.filter(leg -> leg.getTraverseMode().isDriving() || leg.getTraverseMode().isTransit())
				.map(leg -> leg.getTraverseMode().toString() + leg.getTripId())
				.collect(Collectors.toSet());
		transfers = transferNames.isEmpty() ? 0 : transferNames.size() - 1;
	}

	public void updateCharacteristics( ) {
		Leg firstLeg = getLegs().get(0);
		Leg lastLeg = getLegs().get(getLegs().size() - 1);
		setDepartureTime(firstLeg.getFrom().getDepartureTime());
		setArrivalTime(lastLeg.getTo().getArrivalTime());
		setDuration(Math.toIntExact(Duration.between(getDepartureTime(), getArrivalTime()).getSeconds()));
		updateTransfers();
		updateTransitTime();
		updateWaitingTime();
		updateWalkDistance();
		updateWalkTime();
	}

	/**
	 * Extract an itinerary from this itinerary using the the specified locations, comprising a single leg.
	 * Issue: There can be tiny deviations from the results from the planner vs the input parameters, so we need to do a bit
	 * of estimating. We want the leg with the smallest deviation from departure and arrival location.
	 * @param from the location to start from 
	 * @param to the destination location
	 * @return the sub itinerary. This is a deep copy. The original graph will be not be altered.
	 */
	public Itinerary createSingleLeggedItinerary(GeoLocation from, GeoLocation to) {
		if (getLegs().isEmpty()) {
			throw new IllegalStateException("Did not expect an empty itinerary");
		}
		List<Double> deviations = new ArrayList<>();
		for (Leg leg: getLegs()) {
			deviations.add(leg.getFrom().getLocation().getDistanceFlat(from) + leg.getTo().getLocation().getDistanceFlat(to));
		}
		Optional<Integer> optIx = IntStream.range(0, deviations.size()).boxed()
	            .min(Comparator.comparingDouble(deviations::get));
	    if (optIx.isEmpty()) {
			throw new IllegalStateException("Did not expect to find no deviations");
	    }
	    int ix = optIx.get();
		Leg leg = getLegs().get(ix);
		if (deviations.get(ix) > 0.1) {
			log.warn(String.format("Trouble finding correct leg, deviation is %dm", Math.round(deviations.get(ix) * 1000)));
		}
		Itinerary it = new Itinerary();
		// Copy the legs
		it.getLegs().add(leg);
		// Now make a deep copy of the shallow copy
		it = it.deepCopy();
		leg.getFrom().setArrivalTime(null);
		leg.getTo().setDepartureTime(null);
		it.updateCharacteristics();
		return it;
	}

	/**
	 * Create new itinerary by appending the specified itinerary to this one.
	 * This itinerary is unchanged, a deep copy is made. The appended itinerary is altered slightly. 
	 * The append and prepend are not commutative because the this object stays unchanged only. 
	 * The stop objects in the appended graph all stay.
	 * @param other the appended itinerary. 
	 * @return A new itinerary comprising this one and the appended other one
	 */
	public Itinerary append(Itinerary other) {
		Itinerary it = this.deepCopy();
		Stop arrivingStop = it.getStops().get(it.getStops().size() - 1);
		Stop departingStop = other.getStops().get(0);
		if (!connectingStopCheck.test(arrivingStop.getLocation(), departingStop.getLocation())) {
			log.warn(String.format("Appending a non-connected itinerary: %s <--> %s", arrivingStop, departingStop));
		}
		// To connect the graph, one of the stops has to go. The first stop of the connecting itinerary stays.
		departingStop.setArrivalTime(arrivingStop.getArrivalTime());
		it.getLegs().get(it.getLegs().size() - 1).setTo(departingStop);
		// Remove the arriving stop
		it.getStops().remove(arrivingStop);
		// ... and add the new departing stop instead
		it.getStops().add(departingStop);
		// Add all legs
		it.getLegs().addAll(other.getLegs());
		// Add all new stops
		it.getStops().addAll(other.getLegs().stream().map(leg -> leg.getTo()).collect(Collectors.toList()));
		it.updateCharacteristics();
		return it;
	}

	/**
	 * Find the successive legs in an itinerary connecting two locations.
	 * @param from If set the departure location.
	 * @param to If set the arrival location.
	 * @param epsilonInMeter the maximum distance to acccept.
	 * @return
	 */
	public List<Leg> findConnectingLegs(GeoLocation from, GeoLocation to) {
		List<Leg> connectingLegs = Collections.emptyList();
		if (from == null && to == null) {
			throw new IllegalArgumentException("Specify and/or to location to find leg");
		}
		Optional<Stop> departingStop = getStops().stream()
				.filter(s -> connectingStopCheck.test(from, s.getLocation()))
				.findFirst();
		Optional<Stop> arrivingStop = getStops().stream()
				.filter(s -> connectingStopCheck.test(to, s.getLocation()))
				.findFirst();
		if (departingStop.isPresent() && arrivingStop.isPresent()) {
			Leg firstLeg = getLegs().stream()
					.filter(leg -> leg.getFrom() == departingStop.get())
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Cannot find departure stop in legs"));
			Leg lastLeg = getLegs().stream()
					.filter(leg -> leg.getTo() == arrivingStop.get())
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Cannot find arrival in legs"));
			int firstLegIx = getLegs().indexOf(firstLeg);
			int lastLegIx = getLegs().indexOf(lastLeg);
			if (firstLegIx == -1 || lastLegIx == -1) {
				throw new IllegalStateException("Unable to lookup my own legs!");
			}
			connectingLegs = getLegs().subList(firstLegIx, lastLegIx + 1);
		}
		return connectingLegs;
	}
	
	/**
	 * Create new itinerary by prepending the specified itinerary to this one.
	 * This itinerary is unchanged, a deep copy is made. The prepended itinerary is altered slightly.
	 * The append and prepend are not commutative because the this object stays unchanged only. 
	 * The stop objects in the prepended graph all stay.
	 * @param other the itinerary to prepend this one. 
	 * @return A new itinerary comprising other one and this one
	 */
	public Itinerary prepend(Itinerary other) {
		Itinerary it = this.deepCopy();
		Stop arrivingStop = other.getStops().get(other.getStops().size() - 1);
		Stop departingStop = it.getStops().get(0);
		if (!connectingStopCheck.test(arrivingStop.getLocation(), departingStop.getLocation())) {
			log.warn(String.format("Appending a non-connected itinerary: %s <--> %s", arrivingStop, departingStop));
		}
		// To connect the graph, one of the stops has to go. The last stop of the connecting itinerary stays.
		arrivingStop.setDepartureTime(departingStop.getDepartureTime());
		it.getLegs().get(0).setFrom(arrivingStop);
		// Remove the departing stop
		it.getStops().remove(departingStop);
		// Add all new legs
		it.getLegs().addAll(0, other.getLegs());
		// Add all new arriving stops
		it.getStops().addAll(0, other.getLegs().stream().map(leg -> leg.getTo()).collect(Collectors.toList()));
		// Prepend departing stop too
		it.getStops().add(0, other.getLegs().get(0).getFrom());
		it.updateCharacteristics();
		return it;
	}

	public void shiftLinear(Duration delta) {
		departureTime = departureTime.plus(delta);
		arrivalTime = arrivalTime.plus(delta);
		getStops().forEach(stop -> stop.shiftLinear(delta));
	}

	/**
	 * Shifts the timing of an itinerary such that the arrival or departure time of the
	 * reference stop will be equal to the specified target time.  
	 * @param referenceStop The stop to use as reference for the target time
	 * @param targetTime The time to arrive at or depart from the reference stop
	 * @param useAsArrivalTime If true then use target time as arrival time.
	 */
	public void shiftItineraryTiming(GeoLocation referenceStop, Instant targetTime, boolean useAsArrivalTime) {
		Optional<Stop> refStopOpt = getStops().stream()
				.filter(stop -> Itinerary.connectingStopCheck.test(referenceStop, stop.getLocation()))
				.findFirst();
		if (refStopOpt.isPresent()) {
			Stop refStop = refStopOpt.get();
			Instant currTime = useAsArrivalTime ? refStop.getArrivalTime() : refStop.getDepartureTime();
			Duration delta = Duration.between(currTime, targetTime);
			shiftLinear(delta);
		} else {
			log.warn("Cannot find connecting stop: " + referenceStop.toString());
		}
	}
	
	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
	@Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
         if (!(o instanceof Itinerary)) {
            return false;
        }
         Itinerary other = (Itinerary) o;
        return id != null && id.equals(other.getId());
    }

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
    @Override
    public int hashCode() {
        return 31;
    }

    /**
     * Validate an itinerary by checking a number of invariants.
     * @throws IllegalStateException in case a constraint is violated
     */
    public void validate() {
    	Instant depTime = getDepartureTime();
    	Instant arrTime = getArrivalTime();
    	for (Leg leg: getLegs()) {
    		if (leg.getStartTime().isBefore(depTime)) {
    			throw new IllegalStateException("Leg starts too early");
    		}
    		if (!leg.getEndTime().isAfter(leg.getStartTime())) {
    			throw new IllegalStateException("Leg has zero or negative duration");
    		}
			depTime = leg.getStartTime();
    		arrTime = leg.getEndTime();
		}
		if (getArrivalTime().isBefore(arrTime)) {
			throw new IllegalStateException("Itinerary arrives before last leg");
		}
    }

}
