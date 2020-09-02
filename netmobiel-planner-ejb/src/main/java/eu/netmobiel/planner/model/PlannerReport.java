package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
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
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import com.vividsolutions.jts.geom.LineString;

import eu.netmobiel.commons.model.GeoLocation;

/**
 * Planner report class used to document the calls to external planners. 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "planner_report")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "planner_report_sg", sequenceName = "planner_report_id_seq", allocationSize = 1, initialValue = 50)
public class PlannerReport implements Serializable {
	private static final long serialVersionUID = -3429586582741265876L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "planner_report_sg")
    private Long id;

	/**
	 * The creation time of the report. This field reflects actual point of time.  
	 */
    @NotNull
    @Column(name = "creation_time", nullable = false)
    private Instant creationTime;

    /**
	 * The time of the request of the plan. The request time is the reference point for validating other request parameters. For 
	 * the planner the request time is the 'now'.  
	 */
    @NotNull
    @Column(name = "request_time", nullable = false)
    private Instant requestTime;


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
     * The additional via points.
     */
    @ElementCollection()
    @CollectionTable(name = "report_via", joinColumns = { 
    		@JoinColumn(name = "report_id", foreignKey = @ForeignKey(foreignKeyDefinition = "via_report_fk"))
	})
    @Column(name = "via_location")
    private List<GeoLocation> viaLocations;

    /**
     * Field to show the requested geometry in a single field, as a collection of lines.
     * This allows for convenient visualization in pgAdmin.
     */
	@Basic			// Add this annotation, otherwise no JPA ModelGen attribute is generated.
    @Column(name = "request_geometry", nullable = false)
    private LineString requestGeometry; 

    /**
     * The eligible traverse modes
     */
    @ElementCollection()
    @CollectionTable(name = "report_traverse_mode", joinColumns = { 
    	@JoinColumn(name = "report_id", foreignKey = @ForeignKey(foreignKeyDefinition = "traverse_mode_report_fk")) 
    })
    @Column(name = "traverse_mode", length = 2)
    @OrderBy("ASC")
    private Set<TraverseMode> traverseModes;

    /**
     * Maximum walking distance. 
     */
    @Column(name = "max_walk_distance")
    private Integer maxWalkDistance;

    /**
     * The error status code, following the http status codes.
     */
    @Column(name = "status_code")
    private int statusCode;

    /**
     * The vendor specific error code, if available.
     */
    @Size(max = 64)
    @Column(name = "error_vendor_code", length = 64)
    private String errorVendorCode;

    /**
     * The error text returned by the planner.
     */
    @Size(max = 512)
    @Column(name = "error_text", length = 512)
    private String errorText;
    
    /**
     * The maximum number of itineraries to calculate 
     */
    @Column(name = "max_results")
    private Integer maxResults;

    /**
     * The offset in the result set (when applicable)
     */
    @Column(name = "start_position")
    private Integer startPosition;

    /**
     * The number of itineraries returned 
     */
    @Column(name = "nr_itineraries")
    private Integer nrItineraries;

    /**
     * Execution time of the request in milliseconds
     */
    @Column(name = "execution_time")
    private long executionTime;

    /**
     * In case of rideshare (or any bookable transport): The number of seats requested.
     */
    @Positive
    @Column(name = "nr_seats")
    private Integer nrSeats;

    /**
     * The tool used for the planning
     */
    @Column(name = "tool_type", nullable = false, length = 3)
    private ToolType toolType;

    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan", foreignKey = @ForeignKey(name = "planner_report_plan_fk"), nullable = false)
	private TripPlan plan;
    
	/**
	 * if true then also retrieve rides that partly overlap the passenger's travel window. Otherwise it must be fully inside the passenger's travel window. 
	 */
    @Column(name = "lenient_search")
    private Boolean lenientSearch;

    /**
     * The detour the driver is willing to make in meters. 
     */
    @Positive
    @Column(name = "max_detour_meters")
    private Integer maxDetourMeters;
    
    /**
     * The detour the driver is willing to make in seconds. 
     */
    @Positive
    @Column(name = "max_detour_seconds")
    private Integer maxDetourSeconds;

    /**
     * Itinerary proposal rejected?
     */
    @Column(name = "rejected")
    private Boolean rejected;
    
    /**
     * Textual reason for rejection
     */
    @Size(max = 256)
    @Column(name = "rejection_reason", length = 256)
    private String rejectionReason;
    
    @PrePersist
    protected void onCreate() {
    	creationTime = Instant.now();
        if (requestTime == null) { 
        	requestTime = creationTime;
        }
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

	public LineString getRequestGeometry() {
		return requestGeometry;
	}

	public void setRequestGeometry(LineString requestGeometry) {
		this.requestGeometry = requestGeometry;
	}

	public Integer getMaxWalkDistance() {
		return maxWalkDistance;
	}

	public void setMaxWalkDistance(Integer maxWalkDistance) {
		this.maxWalkDistance = maxWalkDistance;
	}

	public String getErrorVendorCode() {
		return errorVendorCode;
	}

	public void setErrorVendorCode(String errorVendorCode) {
		this.errorVendorCode = errorVendorCode;
	}

	public String getErrorText() {
		return errorText;
	}

	public void setErrorText(String error) {
		this.errorText = error;
	}

	public Integer getNrItineraries() {
		return nrItineraries;
	}

	public void setNrItineraries(Integer nrItineraries) {
		this.nrItineraries = nrItineraries;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public Integer getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(Integer nrSeats) {
		this.nrSeats = nrSeats;
	}

	public List<GeoLocation> getViaLocations() {
		return viaLocations;
	}

	public void setViaLocations(List<GeoLocation> viaLocations) {
		this.viaLocations = viaLocations;
	}

	public TripPlan getPlan() {
		return plan;
	}

	public void setPlan(TripPlan plan) {
		this.plan = plan;
	}

	public Set<TraverseMode> getTraverseModes() {
		return traverseModes;
	}

	public void setTraverseModes(Set<TraverseMode> traverseModes) {
		this.traverseModes = traverseModes;
	}

	public ToolType getToolType() {
		return toolType;
	}

	public void setToolType(ToolType toolType) {
		this.toolType = toolType;
	}

	public Integer getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

	public Integer getStartPosition() {
		return startPosition;
	}

	public void setStartPosition(Integer startPosition) {
		this.startPosition = startPosition;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
    
    public Boolean getLenientSearch() {
		return lenientSearch;
	}

	public void setLenientSearch(Boolean lenientSearch) {
		this.lenientSearch = lenientSearch;
	}

	public Integer getMaxDetourMeters() {
		return maxDetourMeters;
	}

	public void setMaxDetourMeters(Integer maxDetourMeters) {
		this.maxDetourMeters = maxDetourMeters;
	}

	public Integer getMaxDetourSeconds() {
		return maxDetourSeconds;
	}

	public void setMaxDetourSeconds(Integer maxDetourSeconds) {
		this.maxDetourSeconds = maxDetourSeconds;
	}

	public Boolean getRejected() {
		return rejected;
	}

	public void setRejected(Boolean rejected) {
		this.rejected = rejected;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public void setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
	}

	private String formatDateTime(Instant instant) {
    	return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    public String shortReport() {
		StringBuilder sb = new StringBuilder();
		sb.append(statusCode).append(" ").append(executionTime).append("ms");
		sb.append(useAsArrivalTime ? " A " : " D ");
		sb.append(formatDateTime(travelTime));
		sb.append(" ").append(from.toString());
		sb.append(" --> ").append(to.toString());
		if (viaLocations != null) {
			sb.append(" Via: ").append(viaLocations.stream().map(p -> p.toString()).collect(Collectors.joining(" ")));
		}
		sb.append(" By: ").append(traverseModes.stream().map(m -> m.name()).collect(Collectors.joining(", ")));
		if (errorVendorCode != null) {
			sb.append(" ").append(errorVendorCode);
		}
		if (errorText != null) {
			sb.append(" ").append(errorText);
		}
		return sb.toString();
    }

}
