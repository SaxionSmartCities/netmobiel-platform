package eu.netmobiel.planner.model;


import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;

import eu.netmobiel.commons.api.EncodedPolylineBean;
import eu.netmobiel.commons.model.ConfirmationReasonType;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PaymentState;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.commons.util.PolylineEncoder;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.util.PlannerUrnHelper;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle (or on foot).
 */

@Entity
@Table(name = "leg")
@Vetoed
@SequenceGenerator(name = "leg_sg", sequenceName = "leg_id_seq", allocationSize = 1, initialValue = 50)
public class Leg implements Serializable {
	private static final long serialVersionUID = -3789784762166689720L;
	public static final String URN_PREFIX = PlannerUrnHelper.createUrnPrefix(Leg.class);

	/**
	 * The duration of the departing state.
	 */
	public static final Duration DEPARTING_PERIOD = Duration.ofMinutes(15);
	/**
	 * The duration of the arriving state.
	 */
	public static final Duration ARRIVING_PERIOD = Duration.ofMinutes(15);

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "leg_sg")
    private Long id;

	/**
	 * Reference urn to the leg. Assumption: The leg id is stable.
	 */
    @Transient
    private String legRef;
    
    /**
     * The duration of the leg in seconds (in general endTime - startTime).
     */
    @Basic
    private Integer duration;

    /**
    * The Place where the leg originates. Note: 'from' is a reserved keyword in Postgres.
    */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_stop", foreignKey = @ForeignKey(name = "leg_from_stop_fk"), nullable = false)
    private Stop from;
   
   /**
    * The Place where the leg begins.
    */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_stop", foreignKey = @ForeignKey(name = "leg_to_stop_fk"), nullable = false)
    private Stop to;

    /**
     * The distance travelled while traversing the leg in meters.
     */
    @Basic
    private Integer distance;
    
    /**
     * The mode (e.g., <code>Walk</code>) used when traversing this leg.
     */
    @Column(name = "traverse_mode", length = 2)
    private TraverseMode traverseMode;

    /**
     * For transit legs, the ID of the transit agency that operates the service used for this leg.
     * For ridesharing the reference to the rideshare service.
     */
    @Column(name = "agency_id", length = 32)
    private String agencyId = null;
    
    /**
     * For transit legs, the ID of the trip.
     * For ridesharing it is the ride reference, i.e. the ride URN!.
     * Otherwise null.
     */
    @Column(name = "trip_id", length = 32)
    private String tripId = null;
    /**
     * The name of the agency or transport service provider
     */
    @Column(name = "agency_name", length = 32)
    private String agencyName;

    /**
     * The timezone offset of the agency in milliseconds.
     */
    @Column(name = "agency_time_zone_offset")
    private Integer agencyTimeZoneOffset;
    /**
     * For transit legs, the type of the route. Non transit -1
     * When 0-7: 0 Tram, 1 Subway, 2 Train, 3 Bus, 4 Ferry, 5 Cable Car, 6 Gondola, 7 Funicular
     * When equal or higher than 100, it is coded using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard
     * Also see http://groups.google.com/group/gtfs-changes/msg/ed917a69cf8c5bef
     */
    @Column(name = "route_type")
    private Integer routeType;
    
    /**
     * For transit legs, the ID of the route.
     * For non-transit legs, null.
     */
    @Column(name = "route_id", length = 32)
    private String routeId;

    /**
     * The short name of the route, e.g. "51", "Sprinter"
     */
    @Column(name = "route_short_name", length = 32)
    private String routeShortName;

    /**
     * The long name of the toute, e.g. "Zutphen <-> Winterwijk"
     */
    @Column(name = "route_long_name", length = 96)
    private String routeLongName;

    /**
     * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
     */
    @Column(name = "headsign", length = 48)
    private String headsign;
    
    /**
     * In case of rideshare: the ID of the driver (a urn)
     */
    @Column(name = "driver_id", length = 32)
    private String driverId;
    
    /**
     * In case of rideshare: the name of the Driver
     */
    @Column(name = "driver_name", length = 64)
    private String driverName;
    
    /**
     * In case of rideshare: The ID of the car (a urn)
     */
    @Column(name = "vehicle_id", length = 32)
    private String vehicleId;
    
    /**
     * In case of rideshare: The brand and model of the car
     */
    @Column(name = "vehicle_name", length = 64)
    private String vehicleName;
    
    /**
     * In case of rideshare: The license plate of the car.
     */
    @Column(name = "vehicle_license_plate", length = 16)
    private String vehicleLicensePlate;
    
    /**
     * For bookable legs: the reference to the booking.
     */
    @Column(name = "booking_id", length = 32)
    private String bookingId;

    /**
     * For bookable legs: Is booking required?
     */
    @Column(name = "booking_required")
    private Boolean bookingRequired;

    /**
     * For bookable legs: the provider has confirmed the booking request of the passenger
     */
    @Column(name = "booking_confirmed")
    private Boolean bookingConfirmed;

    /**
     * The leg's geometry. This one is used only when storing trips into the database. 
     */
	@Basic			// Add this annotation, otherwise no JPA ModelGen attribute is generated.
    @Column(name = "leg_geometry", nullable = true)
    private MultiPoint legGeometry; 

    /**
     * The leg's geometry as encoded polyline bean. When the domain model is used as decoupling layer for OpenTripPlanner, 
     * the already encoded geometry is passed untouched. 
     */
    @Transient
    private EncodedPolylineBean legGeometryEncoded; 
    
    /**
     * A series of turn by turn instructions used for walking, biking and driving. 
     */
	@ElementCollection()
	@CollectionTable(
		name = "guide_step",
		joinColumns = @JoinColumn(name = "leg_id", referencedColumnName = "id", 
			foreignKey = @ForeignKey(name = "step_leg_fk")) 
	)
	@OrderColumn(name = "step_ix")
    private List<GuideStep> guideSteps;

    /**
     * For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends.
     * For non-transit legs, null.
     * In the model we do not save the intermediate stops.
     */
	@Transient
    private List<Stop> intermediateStops;

	/**
	 * The state of the leg within a trip. In the itineraries the state is null. 
	 * A leg gets the initial state assigned when persisted as part of a trip.
	 */
    @Column(name = "state", length = 3)
    private TripState state;

    /**
     * Reference to the report of the planner that created the leg.
     * In some cases the report may be absent, e.g., when legs are created manually.  
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report", foreignKey = @ForeignKey(name = "leg_report_fk"))
    private PlannerReport plannerReport;
    
    /**
     * The costs of travelling over this leg in NetMobiel credits.
     */
    @Column(name = "fare_credits")
    private Integer fareInCredits;

    /**
     * The state of the payment. If null it is undefined.
     */
    @Column(name = "payment_state", length = 1)
    private PaymentState paymentState;

    /**
     * The urn of the payment. Can refer to the reservation, the release or the final transfer. 
     */
    @Column(name = "payment_id", length = 32)
    private String paymentId = null;

    /**
     * Is a confirmation by the transport provider requested?
     */
    @Column(name = "confirmation_prov_req", nullable = false)
    private boolean confirmationByProviderRequested;

    /**
     * Is a confirmation requested from the traveller?
     */
    @Column(name = "confirmation_req", nullable = false)
    private boolean confirmationRequested;

    /**
     * Has the leg been confirmed by the transport provider?
     * A null value means no response.
     */
    @Column(name = "confirmed_prov")
    private Boolean confirmedByProvider;

    /**
     * Has the leg been confirmed by the traveller?
     */
    @Column(name = "confirmed")
    private Boolean confirmed;

    /**
     * Explanation for the (negative) confirmation value of the transport provider.
     */
    @Column(name = "conf_reason_prov")
    private ConfirmationReasonType confirmationReasonByProvider;
    
    /**
     * Explanation for the (negative) confirmation value of the traveller.
     */
    @Column(name = "conf_reason")
    private ConfirmationReasonType confirmationReason;

    /**
     * If true then the leg (in fact the booking) was cancelled by the mobility provider.
     */
    @Column(name = "cancelled_by_provider")
    private Boolean cancelledByProvider;

    /**
     * If set the leg is intended to resolves a shout-out of a passenger.
     * A shout-out reference refers to a trip plan (shout-out) created by a traveller. 
     */
    @Column(name = "shout_out_ref")
    private String shoutOutRef;
    
    public Leg() {
    	this.state = TripState.PLANNING;
    }

    public Leg(Stop from, Stop to) {
    	this();
    	this.from = from;
    	this.to = to;
    	// Other parameters are still unknown.
    }

    public Leg(Leg other) {
    	this();
		this.agencyId = other.agencyId;
		this.agencyName = other.agencyName;
		this.agencyTimeZoneOffset = other.agencyTimeZoneOffset;
		this.bookingConfirmed = other.bookingConfirmed;
		this.bookingId = other.bookingId;
		this.bookingRequired = other.bookingRequired;
		this.cancelledByProvider = other.cancelledByProvider;
		this.confirmationRequested = other.confirmationRequested;
		this.confirmationByProviderRequested = other.confirmationByProviderRequested;
		this.confirmed = other.confirmed;
		this.confirmedByProvider = other.confirmedByProvider;
		this.confirmationReason = other.confirmationReason;
		this.confirmationReasonByProvider = other.confirmationReasonByProvider;
		this.distance = other.distance;
		this.driverId = other.driverId;
		this.driverName = other.driverName;
		this.duration = other.duration;
		this.fareInCredits = other.fareInCredits;
		this.from = other.from.copy();
		// Copy by value
		this.guideSteps = new ArrayList<>(other.getGuideSteps().stream().map(GuideStep::copy).collect(Collectors.toList()));
		this.headsign = other.headsign;
		this.legGeometry = other.legGeometry;
		this.paymentId = other.paymentId;
		this.paymentState = other.paymentState;
		this.plannerReport = other.plannerReport; 
		this.routeType = other.routeType;
		this.routeId = other.routeId;
		this.routeLongName = other.routeLongName;
		this.routeShortName = other.routeShortName;
		this.routeType = other.routeType;
		this.shoutOutRef = other.shoutOutRef;
		this.state = other.state;
		this.to = other.to.copy();
		this.traverseMode = other.traverseMode;
		this.tripId = other.tripId;
		this.vehicleId = other.vehicleId;
		this.vehicleLicensePlate = other.vehicleLicensePlate;
		this.vehicleName = other.vehicleName;
	}

    public Leg copy() {
    	return new Leg(this);
    }
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLegRef() {
    	if (legRef == null) {
    		legRef = UrnHelper.createUrn(Leg.URN_PREFIX, getId());
    	}
		return legRef;
	}

	public Instant getStartTime() {
		return getFrom().getDepartureTime();
	}

	public Instant getEndTime() {
		return getTo().getArrivalTime();
	}

	public Stop getFrom() {
		return from;
	}

	public void setFrom(Stop from) {
		this.from = from;
	}

	public Stop getTo() {
		return to;
	}

	public void setTo(Stop to) {
		this.to = to;
	}

	public Integer getDistance() {
		return distance;
	}

	public void setDistance(Integer distance) {
		this.distance = distance;
	}

	public TraverseMode getTraverseMode() {
		return traverseMode;
	}

	public void setTraverseMode(TraverseMode mode) {
		this.traverseMode = mode;
	}

	public String getAgencyId() {
		return agencyId;
	}

	public void setAgencyId(String agencyId) {
		this.agencyId = agencyId;
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public String getAgencyName() {
		return agencyName;
	}

	public void setAgencyName(String agencyName) {
		this.agencyName = agencyName;
	}

	public Integer getAgencyTimeZoneOffset() {
		return agencyTimeZoneOffset;
	}

	public void setAgencyTimeZoneOffset(Integer agencyTimeZoneOffset) {
		this.agencyTimeZoneOffset = agencyTimeZoneOffset;
	}

	public Integer getRouteType() {
		return routeType;
	}

	public void setRouteType(Integer routeType) {
		this.routeType = routeType;
	}

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public String getRouteShortName() {
		return routeShortName;
	}

	public void setRouteShortName(String routeShortName) {
		this.routeShortName = routeShortName;
	}

	public String getRouteLongName() {
		return routeLongName;
	}

	public void setRouteLongName(String routeLongName) {
		this.routeLongName = routeLongName;
	}

	public String getHeadsign() {
		return headsign;
	}

	public void setHeadsign(String headsign) {
		this.headsign = headsign;
	}

	public String getDriverId() {
		return driverId;
	}

	public void setDriverId(String driverId) {
		this.driverId = driverId;
	}

	public String getDriverName() {
		return driverName;
	}

	public void setDriverName(String driverName) {
		this.driverName = driverName;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public String getVehicleName() {
		return vehicleName;
	}

	public void setVehicleName(String vehicleName) {
		this.vehicleName = vehicleName;
	}

	public String getVehicleLicensePlate() {
		return vehicleLicensePlate;
	}

	public void setVehicleLicensePlate(String vehicleLicensePlate) {
		this.vehicleLicensePlate = vehicleLicensePlate;
	}

	public String getBookingId() {
		return bookingId;
	}

	public void setBookingId(String bookingId) {
		this.bookingId = bookingId;
	}

	public Boolean getBookingRequired() {
		return bookingRequired;
	}

	public void setBookingRequired(Boolean bookingRequired) {
		this.bookingRequired = bookingRequired;
	}

	public boolean isBookingRequired() {
		return bookingRequired == Boolean.TRUE;
	}
	
	public Boolean getBookingConfirmed() {
		return bookingConfirmed;
	}
    public boolean isBookingConfirmed() {
    	return bookingConfirmed == Boolean.TRUE;
    }
    
    public void setBookingConfirmed(Boolean confirmed) {
    	this.bookingConfirmed = confirmed;
    }

	public MultiPoint getLegGeometry() {
		return legGeometry;
	}

	public void setLegGeometry(MultiPoint  legGeometry) {
		this.legGeometry = legGeometry;
		this.legGeometryEncoded = null;
	}

	public EncodedPolylineBean getLegGeometryEncoded() {
    	if (legGeometry != null && legGeometryEncoded == null) {
    		legGeometryEncoded = PolylineEncoder.createEncodings(legGeometry);
    	}
		return legGeometryEncoded;
	}

	public void setLegGeometryEncoded(EncodedPolylineBean legGeometryEncoded) {
		this.legGeometryEncoded = legGeometryEncoded;
    	if (this.legGeometryEncoded != null) {
    		this.legGeometry = GeometryHelper.createLegGeometry(this.legGeometryEncoded);
    	}
	}

	public List<GuideStep> getGuideSteps() {
		if (guideSteps == null) {
			guideSteps = new ArrayList<>();
		}
		return guideSteps;
	}

	public void setGuideSteps(List<GuideStep> walkSteps) {
		this.guideSteps = walkSteps;
	}

	public List<Stop> getIntermediateStops() {
		return intermediateStops;
	}

	public void setIntermediateStops(List<Stop> intermediateStops) {
		this.intermediateStops = intermediateStops;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public TripState getState() {
		return state;
	}

	public void setState(TripState state) {
		this.state = state;
	}

	public PlannerReport getPlannerReport() {
		return plannerReport;
	}

	public void setPlannerReport(PlannerReport plannerReport) {
		this.plannerReport = plannerReport;
	}

	public Integer getFareInCredits() {
		return fareInCredits;
	}

	public void setFareInCredits(Integer fareInCredits) {
		this.fareInCredits = fareInCredits;
	}

	public boolean hasFareInCredits() {
		return getFareInCredits() != null && getFareInCredits() > 0;
	}
	
	public PaymentState getPaymentState() {
		return paymentState;
	}

	public void setPaymentState(PaymentState paymentState) {
		this.paymentState = paymentState;
	}

	public boolean isPaymentDue() {
		return PaymentState.RESERVED == paymentState;
	}
	
	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public boolean isConfirmationByProviderRequested() {
		return confirmationByProviderRequested;
	}

	public void setConfirmationByProviderRequested(boolean confirmationByProviderRequested) {
		this.confirmationByProviderRequested = confirmationByProviderRequested;
	}

	public boolean isConfirmationRequested() {
		return confirmationRequested;
	}

	public void setConfirmationRequested(boolean confirmationRequested) {
		this.confirmationRequested = confirmationRequested;
	}

	public Boolean getConfirmedByProvider() {
		return confirmedByProvider;
	}

	public void setConfirmedByProvider(Boolean confirmedByProvider) {
		this.confirmedByProvider = confirmedByProvider;
	}

	public boolean isConfirmedByProvider() {
		return Boolean.TRUE.equals(confirmedByProvider);
	}

	public boolean isDeniedByProvider() {
		return Boolean.FALSE.equals(confirmedByProvider);
	}

	public Boolean getConfirmed() {
		return confirmed;
	}

	public void setConfirmed(Boolean confirmed) {
		this.confirmed = confirmed;
	}

	public boolean isConfirmed() {
		return Boolean.TRUE.equals(confirmed);
	}

	public boolean isDenied() {
		return Boolean.FALSE.equals(confirmed);
	}

	public boolean isDisputed() {
		return isConfirmedByProvider() && isDenied();
	}
	
	public ConfirmationReasonType getConfirmationReasonByProvider() {
		return confirmationReasonByProvider;
	}

	public void setConfirmationReasonByProvider(ConfirmationReasonType confirmationReasonByProvider) {
		this.confirmationReasonByProvider = confirmationReasonByProvider;
	}

	public ConfirmationReasonType getConfirmationReason() {
		return confirmationReason;
	}

	public void setConfirmationReason(ConfirmationReasonType confirmationReason) {
		this.confirmationReason = confirmationReason;
	}

	public Boolean getCancelledByProvider() {
		return cancelledByProvider;
	}

	public void setCancelledByProvider(Boolean cancelledByProvider) {
		this.cancelledByProvider = cancelledByProvider;
	}

	public String getShoutOutRef() {
		return shoutOutRef;
	}

	public void setShoutOutRef(String shoutOutRef) {
		this.shoutOutRef = shoutOutRef;
	}

	/**
     * Whether this leg is a transit leg or not.
     * @return Boolean true if the leg is a transit leg
     */
    public Boolean isTransitLeg() {
        return traverseMode == null ? null : traverseMode.isTransit();
    }

    /**
     * Evaluates the current state parameters and determines the leg state accordingly.
     * This method determines the state without considering the current state. This might result in 'unexpected' transitions,
     * caused by. e.g., not checking often enough.
     * @param referenceTime The time the check occurs. Ordinarily 'now', but for testing might be different.   
     */
    public TripState nextState(Instant referenceTime) {
    	TripState next = state;
    	if (state == null) {
    		state = TripState.PLANNING;
    	}
    	if (state == TripState.CANCELLED ) {
    		// The cancel state is set explicitly. Once cancelled stays cancelled forever. 
    		// The completed is not so final as it looks.
    		return state;
    	}
    	next = state;
    	if (isBookingRequired() && (getBookingId() == null || !isBookingConfirmed())) {
    		next = TripState.BOOKING;
    	} else if (!getEndTime().plus(ARRIVING_PERIOD).isAfter(referenceTime)) {
        	if (isPaymentDue()) {
        		next = TripState.VALIDATING;
        	} else {
        		next = TripState.COMPLETED;
        	}
       	} else if (!getEndTime().isAfter(referenceTime)) {
    		next = TripState.ARRIVING;
    	} else if (!getStartTime().isAfter(referenceTime)) {
    		next = TripState.IN_TRANSIT;
    	} else if (!getStartTime().minus(DEPARTING_PERIOD).isAfter(referenceTime)) {
    		next = TripState.DEPARTING;
    	} else {
    		next = TripState.SCHEDULED;
    	}
    	return next;
    }
    
    public int getDestinationStopDistance() {
    	Coordinate lastCoord = getLegGeometry().getCoordinates()[getLegGeometry().getCoordinates().length - 1];
    	GeoLocation lastPoint = new GeoLocation(GeometryHelper.createPoint(lastCoord));
    	return Math.toIntExact(Math.round(to.getLocation().getDistanceFlat(lastPoint) * 1000));
    }
    
	public String toStringCompact() {
		StringBuilder builder = new StringBuilder();
		builder.append("Leg ");
		builder.append(id).append(" ");
		builder.append(state).append(" ");
		builder.append(duration).append("s ");
		builder.append(distance).append("m ");
		builder.append(traverseMode).append(" ");
		builder.append(from).append(" ");
		builder.append(to).append(" ");
		return builder.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Leg ");
		builder.append(state).append(" ");
		builder.append(duration).append("s ");
		builder.append(distance).append("m ");
		builder.append(traverseMode).append(" ");
		if (fareInCredits != null) {
			builder.append("Fare (cr) ").append(fareInCredits).append(" ");
		}
		if (routeShortName != null) {
			builder.append(routeShortName).append(" ");
		}
		if (routeLongName != null) {
			builder.append(routeLongName).append(" ");
		}
		if (vehicleName != null) {
			builder.append("Car ").append(vehicleName).append(" ");
		}
		if (driverName != null) {
			builder.append("Driver ").append(driverName).append(" ");
		}
		if (agencyId != null) {
			builder.append("AgencyId ").append(agencyId).append(" ");
		}
		if (tripId != null) {
			builder.append("Trip ").append(tripId).append(" ");
		}
		
		if (intermediateStops != null && !intermediateStops.isEmpty()) {
			builder.append("\n\t\t\t").append(intermediateStops.stream().map(p -> p.toString()).collect(Collectors.joining("\n\t\t\t"))).append("");
		}
		return builder.toString();
	}
    
	public String toStringFull() {
		StringBuilder builder = new StringBuilder();
		builder.append(toString());
		if (guideSteps != null && !guideSteps.isEmpty()) {
			builder.append("\n\t\t\t\t").append(guideSteps.stream().map(p -> p.toString()).collect(Collectors.joining("\n\t\t\t\t"))).append("");
		}
		return builder.toString();
	}
}