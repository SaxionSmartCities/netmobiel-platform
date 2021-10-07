package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
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
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.util.PlannerUrnHelper;


/**
 * A TripPlan is a set of ways to get from point A to point B at time T.
 */

@NamedNativeQueries({
	@NamedNativeQuery(
		name = TripPlan.RGP_11_TRIP_PLAN_SHOUT_OUT_ISSUED_COUNT,
		query = "select u.managed_identity as managed_identity, "
        		+ "date_part('year', p.creation_time) as year, " 
        		+ "date_part('month', p.creation_time) as month, "
        		+ "count(*) as count "
        		+ "from trip_plan p "
        		+ "join pl_user u on u.id = p.traveller "
        		+ "where p.creation_time >= ? and p.creation_time < ? and p.plan_type = 'SHO' "
        		+ "group by u.managed_identity, year, month "
        		+ "order by u.managed_identity, year, month",
        resultSetMapping = TripPlan.PN_TRIP_PLAN_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
		name = TripPlan.RGP_12_TRIP_PLAN_SHOUT_OUT_AT_LEAST_ONE_OFFER_COUNT,
				// --> Count the shout-outs that have itineraries attached
		query = "select u.managed_identity as managed_identity, "
        		+ "date_part('year', p.creation_time) as year, " 
        		+ "date_part('month', p.creation_time) as month, "
        		+ "count(distinct it.trip_plan) as count "
        		+ "from trip_plan p "
        		+ "join pl_user u on u.id = p.traveller "
        		+ "join itinerary it on it.trip_plan = p.id "
        		+ "where p.creation_time >= ? and p.creation_time < ? and p.plan_type = 'SHO' "
        		+ "group by u.managed_identity, year, month "
        		+ "order by u.managed_identity, year, month",
        resultSetMapping = TripPlan.PN_TRIP_PLAN_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
		name = TripPlan.RGP_13_TRIP_PLAN__SHOUT_OUT_ACCEPTED_COUNT,
				// --> Shout-outs that have an itinerary that is also used by an trip.
		query = "select u.managed_identity as managed_identity, "
        		+ "date_part('year', p.creation_time) as year, " 
        		+ "date_part('month', p.creation_time) as month, "
        		+ "count(*) as count "
        		+ "from trip_plan p "
        		+ "join pl_user u on u.id = p.traveller "
        		+ "join itinerary it on it.trip_plan = p.id "
        		+ "join trip t on t.itinerary = it.id "
        		+ "where p.creation_time >= ? and p.creation_time < ? and p.plan_type = 'SHO' "
        		+ "group by u.managed_identity, year, month "
        		+ "order by u.managed_identity, year, month",
        resultSetMapping = TripPlan.PN_TRIP_PLAN_USER_YEAR_MONTH_COUNT_MAPPING),
})
@SqlResultSetMapping(
		name = TripPlan.PN_TRIP_PLAN_USER_YEAR_MONTH_COUNT_MAPPING, 
		classes = @ConstructorResult(
			targetClass = NumericReportValue.class, 
			columns = {
					@ColumnResult(name = "managed_identity", type = String.class),
					@ColumnResult(name = "year", type = int.class),
					@ColumnResult(name = "month", type = int.class),
					@ColumnResult(name = "count", type = int.class)
			}
		)
	)

@NamedEntityGraphs({
	@NamedEntityGraph(
			name = TripPlan.DETAILED_ENTITY_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "itineraries", subgraph = "subgraph.itinerary"),		
					@NamedAttributeNode(value = "traveller"),		
					@NamedAttributeNode(value = "requestor"),		
					@NamedAttributeNode(value = "traverseModes"),
					@NamedAttributeNode(value = "referenceItinerary", subgraph = "subgraph.itinerary"),		
			}, subgraphs = {
					@NamedSubgraph(
							name = "subgraph.itinerary",
							attributeNodes = {
									@NamedAttributeNode(value = "legs", subgraph = "subgraph.leg")
							}
					),
					@NamedSubgraph(
							name = "subgraph.leg",
							attributeNodes = {
									@NamedAttributeNode(value = "from"),
									@NamedAttributeNode(value = "to"),
//									@NamedAttributeNode(value = "guideSteps")
							}
					)
			}
	),
	@NamedEntityGraph(
			name = TripPlan.SHOUT_OUT_ENTITY_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "traveller"),		
					@NamedAttributeNode(value = "traverseModes"),
					@NamedAttributeNode(value = "referenceItinerary", subgraph = "subgraph.itinerary"),		
			}, subgraphs = {
					@NamedSubgraph(
							name = "subgraph.itinerary",
							attributeNodes = {
									@NamedAttributeNode(value = "legs", subgraph = "subgraph.leg")
							}
					),
					@NamedSubgraph(
							name = "subgraph.leg",
							attributeNodes = {
									@NamedAttributeNode(value = "from"),
									@NamedAttributeNode(value = "to"),
							}
					)
			}
	)
})
@Entity
@Table(name = "trip_plan")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "trip_plan_sg", sequenceName = "trip_plan_id_seq", allocationSize = 1, initialValue = 50)
public class TripPlan implements Serializable {
	private static final long serialVersionUID = -4434111737718478002L;
	public static final String DETAILED_ENTITY_GRAPH = "list-detailed-trip-plan-entity-graph";
	public static final String SHOUT_OUT_ENTITY_GRAPH = "list-shout-out-trip-plan-entity-graph";
	public static final String URN_PREFIX = PlannerUrnHelper.createUrnPrefix(TripPlan.class);

	public static final String PN_TRIP_PLAN_USER_YEAR_MONTH_COUNT_MAPPING = "PNTripPlanUserYearMonthCountMapping";
	public static final String RGP_11_TRIP_PLAN_SHOUT_OUT_ISSUED_COUNT = "ListTripPlanShoutOutIssuedCount";
	public static final String RGP_12_TRIP_PLAN_SHOUT_OUT_AT_LEAST_ONE_OFFER_COUNT = "ListTripPlanShoutOutAtLeastOneOfferCount";
	public static final String RGP_13_TRIP_PLAN__SHOUT_OUT_ACCEPTED_COUNT = "ListTripPlanShoutOutAcceptedCount";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_plan_sg")
    private Long id;

    @Transient
    private String planRef;

	/**
	 * The creation time of the plan. This field reflects actual point of time.  
	 */
    @NotNull
    @Column(name = "creation_time", nullable = false)
    private Instant creationTime;

    /**
	 * The time of the receiving request of the plan. The request time is the reference point for validating other request parameters. For 
	 * the planner the request time is the 'now'.  
	 */
    @NotNull
    @Column(name = "request_time", nullable = false)
    private Instant requestTime;

	/**
	 * The time it took to complete the plan. Regular plans are calculated real-time in a matter of seconds. Shout-out plans have a duration of hours or perhaps even days. 
	 */
    @Column(name = "request_duration", nullable = true)
    private Long requestDuration;

    /**
     * The traveller applying the plan.
     */
    @NotNull
    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "traveller", nullable = false, foreignKey = @ForeignKey(name = "trip_plan_traveller_fk"))
    private PlannerUser traveller;
    
    /**
     * A URN reference to the traveller. 
     */
    @Transient
    private String travellerRef;
    
    /**  
     * The time and date of the travel. This time can be used in the planner as time of departure or time of arrival, depending
     * on the flag arrivalTimePinned. 
     */
    @Column(name = "travel_time", nullable = false)
    private Instant travelTime;

    /**
     * If true then use the travel time as the arrival time, i.e. close to arrival time is better.
     * Otherwise departure time is more important.
     */
    @Column(name = "use_as_arrival_time")
    private boolean useAsArrivalTime;

    /**  
     * The time and date of earliest departure.  
     */
    @Column(name = "earliest_departure_time", nullable = true)
    private Instant earliestDepartureTime;

    /**  
     * The time and date of latest arrival. 
     */
    @Column(name = "latest_arrival_time", nullable = true)
    private Instant latestArrivalTime;
    
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "from_label", length = GeoLocation.MAX_LABEL_LENGTH)), 
    	@AttributeOverride(name = "point", column = @Column(name = "from_point", nullable = false)), 
   	} )
    private GeoLocation from;
    
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "to_label", length = GeoLocation.MAX_LABEL_LENGTH)), 
    	@AttributeOverride(name = "point", column = @Column(name = "to_point", nullable = false)), 
   	} )
    private GeoLocation to;
    
    /**
     * The eligible traverse modes
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plan_traverse_mode", joinColumns = { 
        	@JoinColumn(name = "plan_id", foreignKey = @ForeignKey(name = "traverse_mode_trip_plan_fk")) 
    })
    @Column(name = "traverse_mode", length = 2)
    // The following definition is required by OnDelete, just a copy of the same column in @CollectionTable
    @JoinColumn(name = "plan_id")
    // Added Cascade for easier integration testing
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<TraverseMode> traverseModes;

    /**
     * Maximum walking distance 
     */
    @Column(name = "max_walk_distance")
    private Integer maxWalkDistance;

    /**
     * Maximum number of transfers
     */
    @Transient
    private Integer maxTransfers;

    /**
     * If true then rideshare is an option as first leg in a multi-leg trip with public transport.
     */
    @Column(name = "first_leg_rs")
    private boolean firstLegRideshareAllowed;

    /**
     * If true then rideshare is an option as last leg in a multi-leg trip with public transport.
     */
    @Column(name = "last_leg_rs")
    private boolean lastLegRideshareAllowed;

    /**
     * Numbers of seats required.
     */
    @Positive
    @Column(name = "nr_seats")
    private int nrSeats;
    
    /** 
     * A list of possible itineraries. 
     */
	@OneToMany(mappedBy = "tripPlan", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@OrderBy("score desc")
    private Set<Itinerary> itineraries;

	/**
     * The planner reports for creating this plan.
     */
	@OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<PlannerReport> plannerReports;

	/**
	 * The type of plan created.
	 */
	@Column(name = "plan_type", length = 3)
	private PlanType planType;

    /**
     * The user requesting the plan. In most cases this is the traveller, but with delegation active the plan is requested by the delegate.
     */
    @NotNull
    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "requestor", nullable = false, foreignKey = @ForeignKey(name = "trip_plan_requestor_fk"))
    private PlannerUser requestor;
    
    /**
     * Geodesic distance. Just for fun.   
     */
    @Column(name = "geodesic_distance")
    private Integer geodesicDistance;
    
    /**
     * ShoutOut only. The reference itinerary used to calculate distance, duration, fare and also the leg geometry.
     * The from and to are the pickup and drop-off locations of the traveller.
     *   
     */
    @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JoinColumn(name = "reference_itinerary", nullable = true, foreignKey = @ForeignKey(name = "trip_plan_reference_itinerary_fk"))
    private Itinerary referenceItinerary;

	public TripPlan() { 
    	this.creationTime = Instant.now();
       	this.requestTime = creationTime;
    }

    public TripPlan(PlannerUser traveller, GeoLocation from, GeoLocation to, Instant travelTime, boolean useAsArrivalTime, 
    		Set<TraverseMode> traverseModes, Integer maxWalkDistance, int nrSeats) {
    	this();
    	this.traveller = traveller;
    	this.requestor = traveller;
        this.from = from;
        this.to = to;
       	this.travelTime = travelTime;
        this.useAsArrivalTime = useAsArrivalTime;
        this.traverseModes = traverseModes;
        this.maxWalkDistance = maxWalkDistance;
        this.nrSeats = nrSeats; 
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPlanRef() {
    	if (planRef == null) {
    		planRef = UrnHelper.createUrn(TripPlan.URN_PREFIX, getId());
    	}
		return planRef;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public PlannerUser getTraveller() {
		return traveller;
	}

	public String getTravellerRef() {
		if (travellerRef == null) {
			travellerRef = UrnHelper.createUrn(PlannerUser.URN_PREFIX, getTraveller().getId());
		}
		return travellerRef;
	}


	public Set<TraverseMode> getTraverseModes() {
		return traverseModes;
	}

	public void setTraverseModes(Set<TraverseMode> traverseModes) {
		this.traverseModes = traverseModes;
	}

	public Instant getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(Instant requestTime) {
		this.requestTime = requestTime;
	}


	public Long getRequestDuration() {
		return requestDuration;
	}

	public void setRequestDuration(Long requestDuration) {
		this.requestDuration = requestDuration;
	}

	public boolean isInProgress() {
		return getRequestDuration() == null;
	}
	
	public PlanType getPlanType() {
		return planType;
	}

	public void setPlanType(PlanType planType) {
		this.planType = planType;
	}

	public void setTraveller(PlannerUser traveller) {
		this.traveller = traveller;
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

	public Set<Itinerary> getItineraries() {
		if (itineraries == null) {
			itineraries = new LinkedHashSet<>();
		}
		return itineraries;
	}

	public void addItinerary(Itinerary itinerary) {
		itinerary.setTripPlan(this);
		getItineraries().add(itinerary);
	}

	public void addItineraries(Collection<Itinerary> theItineraries) {
		theItineraries.forEach(it -> addItinerary(it));
	}

	public Instant getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(Instant travelTime) {
		this.travelTime = travelTime;
	}

	public boolean isUseAsArrivalTime() {
		return useAsArrivalTime;
	}

	public void setUseAsArrivalTime(boolean useAsArrivalTime) {
		this.useAsArrivalTime = useAsArrivalTime;
	}

	public Instant getEarliestDepartureTime() {
		return earliestDepartureTime;
	}

	public void setEarliestDepartureTime(Instant earliestDepartureTime) {
		this.earliestDepartureTime = earliestDepartureTime;
	}

	public Instant getLatestArrivalTime() {
		return latestArrivalTime;
	}

	public void setLatestArrivalTime(Instant latestArrivalTime) {
		this.latestArrivalTime = latestArrivalTime;
	}

	public Integer getMaxWalkDistance() {
		return maxWalkDistance;
	}

	public void setMaxWalkDistance(Integer maxWalkDistance) {
		this.maxWalkDistance = maxWalkDistance;
	}

	public int getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(int nrSeats) {
		this.nrSeats = nrSeats;
	}

	public Integer getMaxTransfers() {
		return maxTransfers;
	}

	public void setMaxTransfers(Integer maxTransfers) {
		this.maxTransfers = maxTransfers;
	}


	public boolean isFirstLegRideshareAllowed() {
		return firstLegRideshareAllowed;
	}

	public void setFirstLegRideshareAllowed(boolean firstLegRideshareAllowed) {
		this.firstLegRideshareAllowed = firstLegRideshareAllowed;
	}

	public boolean isLastLegRideshareAllowed() {
		return lastLegRideshareAllowed;
	}

	public void setLastLegRideshareAllowed(boolean lastLegRideshareAllowed) {
		this.lastLegRideshareAllowed = lastLegRideshareAllowed;
	}

	public boolean isRideshareLegAllowed() {
		return isFirstLegRideshareAllowed() || isLastLegRideshareAllowed();
	}
	
	public PlannerUser getRequestor() {
		return requestor;
	}

	public void setRequestor(PlannerUser requestor) {
		this.requestor = requestor;
	}

	public Integer getGeodesicDistance() {
		return geodesicDistance;
	}

	public void setGeodesicDistance(Integer geodesicDistance) {
		this.geodesicDistance = geodesicDistance;
	}

	public Itinerary getReferenceItinerary() {
		return referenceItinerary;
	}

	public void setReferenceItinerary(Itinerary referenceItinerary) {
		this.referenceItinerary = referenceItinerary;
	}

	private static String formatTime(Instant instant) {
    	return instant != null ? DateTimeFormatter.ISO_INSTANT.format(instant) : "*";
    }
    
    @Override
	public String toString() {
		return String.format("TripPlan [%s %s (%s %s) from %s to %s \n\t%s]",
				useAsArrivalTime ? "A" : "D", formatTime(travelTime), 
				formatTime(earliestDepartureTime), formatTime(latestArrivalTime), 
				from, to, 
				itineraries.stream().map(i -> i.toString()).collect(Collectors.joining("\n\t")));
	}

	public List<PlannerReport> getPlannerReports() {
		if (plannerReports == null) {
			plannerReports = new ArrayList<>();
		}
		return plannerReports;
	}
	
	public void addPlannerReport(PlannerReport report) {
		report.setPlan(this);
		getPlannerReports().add(report);
	}

	public boolean isOpen() {
		return getRequestDuration() == null;
	}
	
	public void close() {
		if (isOpen()) {
			setRequestDuration(Instant.now().toEpochMilli() - getCreationTime().toEpochMilli());
		}
	}

	public void addPlannerResult(PlannerResult result) {
		addItineraries(result.getItineraries());
		addPlannerReport(result.getReport());
	}
}
