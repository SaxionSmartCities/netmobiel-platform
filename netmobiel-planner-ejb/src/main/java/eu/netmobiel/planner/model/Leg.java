package eu.netmobiel.planner.model;


import java.io.Serializable;
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
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.GeometryHelper;

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

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "leg_sg")
    private Long id;

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
    public String agencyId = null;
    
    /**
     * For transit legs, the ID of the trip.
     * For ridesharing it is the ride reference.
     * Otherwise null.
     */
    @Column(name = "trip_id", length = 32)
    public String tripId = null;
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
    @Column(name = "headsign", length = 32)
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
     * The leg's geometry. This one is used only when storing trips into the database. 
     */
    @Column(name = "leg_geometry", nullable = true)
    private MultiPoint legGeometry; 

    /**
     * The leg's geometry as encoded polyline bean. When the domain model is used as decoupling layer for OpenTripPlanner, 
     * the already encode geometry is passed untouched. 
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
    public List<Stop> intermediateStops;

	/**
	 * The state of the leg within a trip. In the itineraries the state is null. 
	 * A leg gets the initial state assigned when persisted as part of a trip.
	 */
    @Column(name = "state", length = 3)
    private TripState state;

    public Leg() {
    }

    public Leg(Leg other) {
		this.distance = other.distance;
		this.traverseMode = other.traverseMode;
		this.agencyId = other.agencyId;
		this.agencyName = other.agencyName;
		this.agencyTimeZoneOffset = other.agencyTimeZoneOffset;
		this.routeType = other.routeType;
		this.routeId = other.routeId;
		this.headsign = other.headsign;
		this.from = other.from.copy();
		this.to = other.to.copy();
		this.legGeometry = other.legGeometry;
		this.routeShortName = other.routeShortName;
		this.routeLongName = other.routeLongName;
		this.driverId = other.driverId;
		this.driverName = other.driverName;
		this.tripId = other.tripId;
		this.vehicleId = other.vehicleId;
		this.vehicleName = other.vehicleName;
		this.vehicleLicensePlate = other.vehicleLicensePlate;
		// Copy by value
		this.guideSteps = new ArrayList<>(other.guideSteps.stream().map(GuideStep::copy).collect(Collectors.toList()));
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

	public MultiPoint getLegGeometry() {
		return legGeometry;
	}

	public void setLegGeometry(MultiPoint  legGeometry) {
		this.legGeometry = legGeometry;
	}

	public EncodedPolylineBean getLegGeometryEncoded() {
		return legGeometryEncoded;
	}

	public void setLegGeometryEncoded(EncodedPolylineBean legGeometryEncoded) {
		this.legGeometryEncoded = legGeometryEncoded;
	}

	public List<GuideStep> getGuideSteps() {
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

	/**
     * Whether this leg is a transit leg or not.
     * @return Boolean true if the leg is a transit leg
     */
    public Boolean isTransitLeg() {
        return traverseMode == null ? null : traverseMode.isTransit();
    }

    public void convertLegGeometry() {
    	if (getLegGeometry() == null && getLegGeometryEncoded() != null) {
    		setLegGeometry(GeometryHelper.createLegGeometry(getLegGeometryEncoded()));
    	}
    }
    
    public int getDestinationStopDistance() {
    	convertLegGeometry();
    	Coordinate lastCoord = getLegGeometry().getCoordinates()[getLegGeometry().getCoordinates().length - 1];
    	GeoLocation lastPoint = new GeoLocation(GeometryHelper.createPoint(lastCoord));
    	return Math.toIntExact(Math.round(to.getLocation().getDistanceFlat(lastPoint) * 1000));
    }
    
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Leg ");
		builder.append(duration).append("s ");
		builder.append(distance).append("m ");
		builder.append(traverseMode).append(" ");
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
		if (guideSteps != null && !guideSteps.isEmpty()) {
			builder.append("\n\t\t\t\t").append(guideSteps.stream().map(p -> p.toString()).collect(Collectors.joining("\n\t\t\t\t"))).append("");
		}
		return builder.toString();
	}
    
}