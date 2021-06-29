package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import com.vividsolutions.jts.geom.Geometry;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.ReferableObject;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.commons.util.EllipseHelper.EligibleArea;
import eu.netmobiel.commons.util.UrnHelper;

/**
 * This class captures the common attributes of a Ride and a Ride Template. The template parameter are constant, with the exception of the 
 * departure and arrival time. For a template these are state parameter used for generating rides out of the template.
 * 
 * @author Jaap Reitsma
 *
 */
@MappedSuperclass
public abstract class RideBase extends ReferableObject implements Serializable {
	private static final long serialVersionUID = 7659815346376185257L;
	public static final float DEFAULT_RELATIVE_MAX_DETOUR = 0.30f;
	public static final float DEFAULT_NOMINAL_SPEED = 25 * 1000 / 3600f; 	/* km/h --> m/s */


    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "driver", nullable = false, foreignKey = @ForeignKey(name = "ride_base_driver_fk"))
    private RideshareUser driver;

    @Transient
    private String driverRef;

    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "car", nullable = false, foreignKey = @ForeignKey(name = "ride_base_car_fk"))
    private Car car;

    @Transient
    private String carRef;

    @PositiveOrZero
    @Max(99)
    @Column(name = "nr_seats_available")
    private int nrSeatsAvailable;

    @Size(max = 256)
    @Column(length = 256)
    private String remarks;

    /**
     * Total distance in [meter]. This parameter is added for convenience, it should be the sum of the distance of the legs.
     */
    @PositiveOrZero
    @Column(name = "distance")
    private Integer distance;
    
    /**
     * Estimated emission of CO2 [g].
     */
    @PositiveOrZero
    @Column(name = "co2_emission")
    private Integer CO2Emission;

    /**
     * The geometry eligible for picking up and dropping off passengers.  
     */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "share_eligibility")
    private Geometry shareEligibility; 

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
     * The distance from departure to arrival location as the bird flies. 
     */
    @Positive
    @Column(name = "carthesian_distance")
    private Integer carthesianDistance;

    /**
     * The compass direction from departure to arrival location (halfway) as the bird flies. 
     *  
     */
    @Column(name = "carthesian_bearing")
    private Integer carthesianBearing;

    /**
     * Template: The departure time of the last generated ride.  
     * Ride: The actual departure time. This might differ (a bit) from the template after some negotiation with a passenger.
     * It will also change because of picking up and dropping off a passenger.
     * This value must be equal to the departure time of the first stop. 
     */
    @NotNull
    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;
    
    /**
     * Template: The arrival time of the last generated ride.  
     * Ride: The actual arrival time. This might differ (a bit) from the template after some negotiation with a passenger.
     * It will also change because of picking up and dropping off a passenger. 
     * This value must be equal to the arrival time of the last stop. 
     */
    @NotNull
    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    /**
     * If true then the arrival time is pinned, i.e. when a booking is added the arrival time is kept at the same time, departure time shifts.
     * Otherwise departure time is pinned.
     */
    @Column(name = "arrival_time_pinned")
    private boolean arrivalTimePinned;

    /**
     * Template: The (standard) departure location of the driver.
     * Ride: Possibly updated  departure location of the driver.  
     */
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "from_label", length = 256)), 
    	@AttributeOverride(name = "point", column = @Column(name = "from_point", nullable = false)), 
   	} )
    private GeoLocation from;
    
    /**
     * Template The (standard) arrival location of the driver.
     * Ride: Possibly updated arrival location of the driver.  
     */
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "to_label", length = 256)), 
    	@AttributeOverride(name = "point", column = @Column(name = "to_point", nullable = false)), 
   	} )
    private GeoLocation to;

    /**
     * The postal code 6 of the departure location.
     */
    @Size(max = 6)
    @Column(name = "departure_postal_code")
    private String departurePostalCode;

    /**
     * The postal code 6 of the arrival location.
     */
    @Size(max = 6)
    @Column(name = "arrival_postal_code")
    private String arrivalPostalCode;

    public RideshareUser getDriver() {
		return driver;
	}

	public void setDriver(RideshareUser driver) {
		this.driver = driver;
		this.driverRef = null;
	}

	public String getDriverRef() {
		if (driverRef == null) {
    		driverRef = UrnHelper.createUrn(RideshareUser.URN_PREFIX, driver.getId());
		}
		return driverRef;
	}

	public void setDriverRef(String driverRef) {
		this.driverRef = driverRef;
		this.driver = null;
	}

	public Car getCar() {
		return car;
	}

	public void setCar(Car car) {
		this.car = car;
		this.carRef = null;
	}

	public String getCarRef() {
		if (carRef == null) {
    		carRef = UrnHelper.createUrn(Car.URN_PREFIX, car.getId());
		}
		return carRef;
	}

	public void setCarRef(String carRef) {
		this.car = null;
		this.carRef = carRef;
	}

	public int getNrSeatsAvailable() {
		return nrSeatsAvailable;
	}

	public void setNrSeatsAvailable(int nrSeatsAvailable) {
		this.nrSeatsAvailable = nrSeatsAvailable;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public Integer getDistance() {
		return distance;
	}

	public void setDistance(Integer distance) {
		this.distance = distance;
	}

	public Integer getCO2Emission() {
		return CO2Emission;
	}

	public void setCO2Emission(Integer cO2Emission) {
		CO2Emission = cO2Emission;
	}

	public Geometry getShareEligibility() {
		return shareEligibility;
	}

	public void setShareEligibility(Geometry shareEligibility) {
		this.shareEligibility = shareEligibility;
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

	public Integer getCarthesianDistance() {
		return carthesianDistance;
	}

	public void setCarthesianDistance(Integer carthesianDistance) {
		this.carthesianDistance = carthesianDistance;
	}

	public Integer getCarthesianBearing() {
		return carthesianBearing;
	}

	public void setCarthesianBearing(Integer carthesianBearing) {
		this.carthesianBearing = carthesianBearing;
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

	public boolean isArrivalTimePinned() {
		return arrivalTimePinned;
	}

	public void setArrivalTimePinned(boolean arrivalTimePinned) {
		this.arrivalTimePinned = arrivalTimePinned;
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

    public String getDeparturePostalCode() {
		return departurePostalCode;
	}

	public void setDeparturePostalCode(String departurePostalCode) {
		this.departurePostalCode = departurePostalCode;
	}

	public String getArrivalPostalCode() {
		return arrivalPostalCode;
	}

	public void setArrivalPostalCode(String arrivalPostalCode) {
		this.arrivalPostalCode = arrivalPostalCode;
	}

	public Integer getDuration() {
    	return departureTime != null && arrivalTime != null ? Math.toIntExact(arrivalTime.getEpochSecond() - departureTime.getEpochSecond()) : null;
    }
    
    /**
     * Calculates an ellipse with the property that the distance from one focal point (departure stop) to
     * the border of the ellipse and then to the other focal point (arrival) is equal to the maximum detour distance.
     * The distance is calculated from the nominal speed (m/s). 
     * @param r the ride.
     */
    public void updateShareEligibility() {
    	// See https://en.wikipedia.org/wiki/Ellipse
    	Integer maxDetourDistance = null;
    	if (getMaxDetourMeters() != null) {
    		maxDetourDistance = getMaxDetourMeters();
    	} else if (getMaxDetourSeconds() != null) {
    		maxDetourDistance = Math.round(getMaxDetourSeconds() * DEFAULT_NOMINAL_SPEED);    		
    	}
    	EligibleArea ea = EllipseHelper.calculateEllipse(getFrom().getPoint(), getTo().getPoint(),  
    			maxDetourDistance != null ? maxDetourDistance / 2.0 : null, DEFAULT_RELATIVE_MAX_DETOUR / 2);
    	setShareEligibility(ea.eligibleAreaGeometry);
    	setCarthesianDistance(Math.toIntExact(Math.round(ea.carthesianDistance)));
    	setCarthesianBearing(Math.toIntExact(Math.round(ea.carthesianBearing)));
    }

	/**
     * Instantiates a new ride from an existing template. 
     * @return The new ride. 
     */
    public static void copy(RideBase src, RideBase dst) {
		dst.arrivalPostalCode = src.arrivalPostalCode;
		dst.arrivalTime = src.arrivalTime;
		dst.arrivalTimePinned = src.arrivalTimePinned;
		dst.car = src.car;
		dst.carthesianBearing = src.carthesianBearing;
		dst.carthesianDistance = src.carthesianDistance;
		dst.CO2Emission = src.CO2Emission;
		dst.departurePostalCode = src.departurePostalCode;
		dst.departureTime = src.departureTime;
		dst.distance = src.distance;
		dst.driver = src.driver;
		dst.from = new GeoLocation(src.from);
		dst.maxDetourMeters = src.maxDetourMeters;
		dst.maxDetourSeconds = src.maxDetourSeconds;
		dst.nrSeatsAvailable = src.nrSeatsAvailable;
		dst.remarks = src.remarks;
		dst.shareEligibility = src.shareEligibility;
		dst.to = new GeoLocation(src.to);
	}
}	
