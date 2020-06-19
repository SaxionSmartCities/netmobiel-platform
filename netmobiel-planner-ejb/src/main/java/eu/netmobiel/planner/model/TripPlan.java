package eu.netmobiel.planner.model;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.util.PlannerUrnHelper;

/**
 * A TripPlan is a set of ways to get from point A to point B at time T.
 */
@Entity
@Table(name = "trip_plan")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "trip_plan_sg", sequenceName = "trip_plan_id_seq", allocationSize = 1, initialValue = 50)
public class TripPlan {
	public static final String URN_PREFIX = PlannerUrnHelper.createUrnPrefix(Itinerary.class);

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_plan_sg")
    private Long id;

	/**
	 * The time of creation of the plan.
	 */
    @NotNull
    @Column(name = "creation_time", nullable = false)
    private Instant creationTime;

    @NotNull
    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "traveller", nullable = false, foreignKey = @ForeignKey(name = "trip_plan_traveller_fk"))
    private User traveller;
    
    /**  
     * The time and date of departure. At least one of departure or arrival must be defined. 
     */
    @Column(name = "departure_time", nullable = true)
    private Instant departureTime;

    /**  
     * The time and date of arrival. At least one of departure or arrival must be defined. 
     */
    @Column(name = "arrival_time", nullable = true)
    private Instant arrivalTime;
    
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "from_label")), 
    	@AttributeOverride(name = "point", column = @Column(name = "from_point", nullable = false)), 
   	} )
    private GeoLocation from;
    
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "to_label")), 
    	@AttributeOverride(name = "point", column = @Column(name = "to_point", nullable = false)), 
   	} )
    private GeoLocation to;
    
    /**
     * The eligible traverse modes
     */
    @ElementCollection()
    @CollectionTable(name = "pr_traverse_modes", foreignKey = @ForeignKey(foreignKeyDefinition = "traverse_mode_planner_report_fk"))
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
    private Boolean firstLegRideshare;

    /**
     * If true then rideshare is an option as last leg in a multi-leg trip with public transport.
     */
    @Column(name = "last_leg_rs")
    private Boolean lastLegRideshare;

    /**
     * Numbers of seats required.
     */
    @Positive
    @Column(name = "nr_seats")
    private Integer nrSeats;
    
    /** 
     * A list of possible itineraries. 
     */
	@OneToMany(cascade = CascadeType.PERSIST, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "trip_plan", foreignKey = @ForeignKey(name = "itinerary_trip__plan_fk"), nullable = false)
	@OrderColumn(name = "itinerary_ix")
    private List<Itinerary> itineraries;

	/**
     * The planner reports for creating this plan.
     */
	@OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<PlannerReport> plannerReports;

    public TripPlan() { }

    public TripPlan(User traveller, GeoLocation from, GeoLocation to, Instant departureTime, Instant arrivalTime, 
    		Set<TraverseMode> traverseModes, Integer maxWalkDistance, Integer nrSeats) {
    	this.traveller = traveller;
        this.from = from;
        this.to = to;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.traverseModes = traverseModes;
        this.maxWalkDistance = maxWalkDistance;
        this.nrSeats = nrSeats; 
    }

    @PrePersist
    protected void onCreate() {
        if (creationTime == null) { 
        	creationTime = Instant.now();
        }
    }


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getTraveller() {
		return traveller;
	}

	public Set<TraverseMode> getTraverseModes() {
		return traverseModes;
	}

	public void setTraverseModes(Set<TraverseMode> traverseModes) {
		this.traverseModes = traverseModes;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public void setTraveller(User traveller) {
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

	public List<Itinerary> getItineraries() {
		if (itineraries == null) {
			itineraries = new ArrayList<>();
		}
		return itineraries;
	}

	public void addItineraries(List<Itinerary> itineraries) {
		itineraries.forEach(it -> it.setPlan(this));
		getItineraries().addAll(itineraries);
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

	private String formatTime(Instant instant) {
    	return instant != null ? DateTimeFormatter.ISO_INSTANT.format(instant) : "*";
    }
    
    @Override
	public String toString() {
		return String.format("TripPlan [%s - %s from %s to %s \n\t%s]", 
				formatTime(departureTime), formatTime(arrivalTime), from, to, 
				itineraries.stream().map(i -> i.toString()).collect(Collectors.joining("\n\t")));
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

}
