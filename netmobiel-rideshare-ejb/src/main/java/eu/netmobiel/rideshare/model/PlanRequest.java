package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import eu.netmobiel.commons.model.GeoLocation;


/**
 * A PlanRequest is a request to plan ea new ride, or to plan a shared ride for a potential passenger.
 */

@Entity
@Table(name = "plan_request")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "plan_request_sg", sequenceName = "plan_request_id_seq", allocationSize = 1, initialValue = 50)
public class PlanRequest implements Serializable {
	private static final long serialVersionUID = -4434111737718478002L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "plan_request_sg")
    private Long id;

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
	 * The time it took to complete the plan.  
	 */
    @Column(name = "request_duration", nullable = true)
    private Long requestDuration;

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
     * Numbers of seats required.
     */
    @Positive
    @Column(name = "nr_seats")
    private int nrSeats;
    
	/**
     * The planner reports for creating this plan.
     */
	@OneToMany(mappedBy = "planRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<PlannerReport> plannerReports;

    /**
     * The user requesting the plan. Can be the traveller or the driver.
     */
    @NotNull
    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "requestor", nullable = false, foreignKey = @ForeignKey(name = "plan_request_requestor_fk"))
    private RideshareUser requestor;
    
    /**
     * Maximum walking distance. 
     */
    @Column(name = "max_walk_distance")
    private Integer maxWalkDistance;

    /**
     * The maximum number of itineraries to calculate 
     */
    @Column(name = "max_results")
    private Integer maxResults;

	public PlanRequest() { 
    	this.creationTime = Instant.now();
       	this.requestTime = creationTime;
    }

    public PlanRequest(RideshareUser requestor, GeoLocation from, GeoLocation to, Instant travelTime, boolean useAsArrivalTime, int nrSeats) {
    	this();
    	this.requestor = requestor;
        this.from = from;
        this.to = to;
       	this.travelTime = travelTime;
        this.useAsArrivalTime = useAsArrivalTime;
        this.nrSeats = nrSeats; 
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getCreationTime() {
		return creationTime;
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

	public int getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(int nrSeats) {
		this.nrSeats = nrSeats;
	}

	public RideshareUser getRequestor() {
		return requestor;
	}

	public void setRequestor(RideshareUser requestor) {
		this.requestor = requestor;
	}

	public Integer getMaxWalkDistance() {
		return maxWalkDistance;
	}

	public void setMaxWalkDistance(Integer maxWalkDistance) {
		this.maxWalkDistance = maxWalkDistance;
	}

	public void setPlannerReports(List<PlannerReport> plannerReports) {
		this.plannerReports = plannerReports;
	}

	public Integer getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

	private static String formatTime(Instant instant) {
    	return instant != null ? DateTimeFormatter.ISO_INSTANT.format(instant) : "*";
    }
    
    @Override
	public String toString() {
		return String.format("TripPlan [%s %s (%s %s) from %s to %s \n\t%s]",
				useAsArrivalTime ? "A" : "D", formatTime(travelTime), 
				formatTime(earliestDepartureTime), formatTime(latestArrivalTime), 
				from, to);
	}

	public List<PlannerReport> getPlannerReports() {
		if (plannerReports == null) {
			plannerReports = new ArrayList<>();
		}
		return plannerReports;
	}
	
	public void addPlannerReport(PlannerReport report) {
		report.setPlanRequest(this);
		getPlannerReports().add(report);
	}

	public boolean isOpen() {
		return getRequestDuration() == null;
	}
	
}
