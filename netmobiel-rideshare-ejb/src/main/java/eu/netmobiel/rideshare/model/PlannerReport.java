package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
     * The plan request causing the creation of the report.
     */
    @NotNull
    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_request", nullable = false, foreignKey = @ForeignKey(name = "plan_report_plan_request_fk"))
    private PlanRequest planRequest;

    /**  
     * The time and date of the travel. This time can be used in the planner as time of departure or time of arrival, depending
     * on the flag arrivalTimePinned. 
     */
    @Column(name = "travel_time")
    private Instant travelTime;

    /**
     * If true then use the travel time as the arrival time, i.e. close to arrival time is better.
     * Otherwise departure time is more important.
     */
    @Column(name = "use_as_arrival_time", nullable = false)
    private boolean useAsArrivalTime;

    /**
     * The actual departure location of the driver, might deviate a bit from the requested location.
     */
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "from_label", length = GeoLocation.MAX_LABEL_LENGTH)), 
    	@AttributeOverride(name = "point", column = @Column(name = "from_point", nullable = false)), 
   	} )
    private GeoLocation from;
    
    /**
     * The actual arrival location of the driver, might deviate a bit from the requested location.
     */
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "to_label", length = GeoLocation.MAX_LABEL_LENGTH)), 
    	@AttributeOverride(name = "point", column = @Column(name = "to_point", nullable = false)), 
   	} )
    private GeoLocation to;

    /**
     * The additional via points for picking up and dropping-off the passenger.
     */
    @ElementCollection()
    @CollectionTable(name = "report_via", joinColumns = { 
    		@JoinColumn(name = "report_id", foreignKey = @ForeignKey(name = "via_report_fk"))
	})
    @Column(name = "via_location")
    // The following definition is required by OnDelete, just a copy of the same column in @CollectionTable
    @JoinColumn(name = "report_id")
    // Added Cascade for easier integration testing
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<GeoLocation> viaLocations;

    /**
     * Field to show the requested geometry in a single field, as a collection of lines.
     * This allows for convenient visualization in pgAdmin.
     */
	@Basic			// Add this annotation, otherwise no JPA ModelGen attribute is generated.
    @Column(name = "request_geometry", nullable = false)
    private LineString requestGeometry; 

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
     * The number of rides returned 
     */
    @Column(name = "nr_results")
    private Integer nrResults;

    /**
     * Execution time of the request in milliseconds
     */
    @PositiveOrZero
    @Column(name = "execution_time", nullable = false)
    private long executionTime;

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
    
    /**
     * The tool used for the planning
     */
    @Column(name = "tool_type", nullable = false, length = 3)
    private ToolType toolType;

    public PlannerReport() {
    	
    }
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public PlanRequest getPlanRequest() {
		return planRequest;
	}

	public void setPlanRequest(PlanRequest planRequest) {
		this.planRequest = planRequest;
	}

	public Integer getNrResults() {
		return nrResults;
	}

	public void setNrResults(Integer nrResults) {
		this.nrResults = nrResults;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public List<GeoLocation> getViaLocations() {
		return viaLocations;
	}

	public void setViaLocations(List<GeoLocation> viaLocations) {
		this.viaLocations = viaLocations;
	}

	public Integer getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
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

	public ToolType getToolType() {
		return toolType;
	}

	public void setToolType(ToolType toolType) {
		this.toolType = toolType;
	}

    public String shortReport() {
		StringBuilder sb = new StringBuilder();
		sb.append(statusCode).append(" ").append(executionTime).append("ms");
		sb.append(" ").append(from.toString());
		sb.append(" --> ").append(to.toString());
		if (viaLocations != null) {
			sb.append(" Via: ").append(viaLocations.stream().map(p -> p.toString()).collect(Collectors.joining(" ")));
		}
		if (errorVendorCode != null) {
			sb.append(" ").append(errorVendorCode);
		}
		if (errorText != null) {
			sb.append(" ").append(errorText);
		}
		return sb.toString();
    }

}
