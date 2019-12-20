package eu.netmobiel.planner.model;


import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.vividsolutions.jts.geom.MultiPoint;

import eu.netmobiel.commons.api.EncodedPolylineBean;

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
     * The date and time this leg begins.
     */
    @Column(name = "start_time")
    private Instant startTime;
    
    /**
     * The date and time this leg ends.
     */
    @Column(name = "end_time")
    private Instant endTime;
    
    /**
     * The duration of the leg in seconds (in general endTime - startTime).
     */
    @Basic
    private Integer duration;

    /**
    * The Place where the leg originates. Note: 'from' is a reserved keyword in Postgres.
    */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "from_stop", foreignKey = @ForeignKey(name = "leg_stop_from_fk"))
    private Stop from;
   
   /**
    * The Place where the leg begins.
    */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "to_stop", foreignKey = @ForeignKey(name = "leg_stop_to_fk"))
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
     * The name of the agency or transport service provider
     */
    @Column(name = "agency_name", length = 32)
    private String agencyName;

    /**
     * For transit legs, the type of the route. Non transit -1
     * When 0-7: 0 Tram, 1 Subway, 2 Train, 3 Bus, 4 Ferry, 5 Cable Car, 6 Gondola, 7 Funicular
     * When equal or highter than 100, it is coded using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard
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
		name = "walk_step",
		joinColumns = @JoinColumn(name = "leg_id", referencedColumnName = "id", 
			foreignKey = @ForeignKey(name = "step_leg_fk")) 
	)
	@OrderColumn(name = "step_ix")
    private List<WalkStep> walkSteps;

    /**
     * For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends.
     * For non-transit legs, null.
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
		this.startTime = other.startTime;
		this.endTime = other.endTime;
		this.distance = other.distance;
		this.traverseMode = other.traverseMode;
		this.agencyName = other.agencyName;
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
		this.vehicleId = other.vehicleId;
		this.vehicleName = other.vehicleName;
		this.vehicleLicensePlate = other.vehicleLicensePlate;
		// Copy by value
		this.walkSteps = new ArrayList<>(other.walkSteps.stream().map(WalkStep::copy).collect(Collectors.toList()));
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
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
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

	public String getAgencyName() {
		return agencyName;
	}

	public void setAgencyName(String agencyName) {
		this.agencyName = agencyName;
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

	public List<WalkStep> getWalkSteps() {
		return walkSteps;
	}

	public void setWalkSteps(List<WalkStep> walkSteps) {
		this.walkSteps = walkSteps;
	}

	public List<Stop> getIntermediateStops() {
		return intermediateStops;
	}

	public void setIntermediateStops(List<Stop> intermediateStops) {
		this.intermediateStops = intermediateStops;
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
    
    /** 
     * The leg's duration in seconds
     */
    public double getDuration() {
        return Duration.between(startTime, endTime).getSeconds();
    }

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
    
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Leg [");
		builder.append(formatTime(startTime)).append(" ");
		builder.append(formatTime(endTime)).append(" ");
		builder.append(getDuration()).append("s");
		
		builder.append("\n\t\t\tFrom ").append(from).append(" ");
		builder.append("To ").append(to).append(" ");
		builder.append(Math.round(distance)).append("m").append(" ");
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
		
		if (intermediateStops != null && !intermediateStops.isEmpty()) {
			builder.append("\n\t\t\t").append(intermediateStops.stream().map(p -> p.toString()).collect(Collectors.joining("\n\t\t\t"))).append("");
		}
		
		builder.append("]");
		return builder.toString();
	}
    
}