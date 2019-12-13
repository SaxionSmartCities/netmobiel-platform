package eu.netmobiel.rideshare.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Basic;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import com.vividsolutions.jts.geom.Geometry;

import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@Entity
@Vetoed
@Table(name = "ride_template")
@SequenceGenerator(name = "ride_template_sg", sequenceName = "ride_template_id_seq", allocationSize = 1, initialValue = 50)
public class RideTemplate implements Serializable {
	private static final long serialVersionUID = -6389728915295371839L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ride_template_sg")
    private Long id;

    @NotNull
    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "driver", nullable = false, foreignKey = @ForeignKey(name = "ride_template_driver_fk"))
    private User driver;

    @Transient
    private String driverRef;

    @NotNull
    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "car", nullable = false, foreignKey = @ForeignKey(name = "ride_template_car_fk"))
    private Car car;

    @Transient
    private String carRef;

    /**
     * Both the from and to stop belong to a dedicated instance of a ride. It could also have been embedded objects as
     * the life-time of a stop is exactly the same as the life-time of a ride.
     * From a normalisation perspective the stops have their own table. Not sure what the best solution is. 
     */
    @NotNull
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "from_place", nullable = false, foreignKey = @ForeignKey(name = "ride_template_from_stop_fk"))
    private Stop fromPlace;

    @NotNull
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "to_place", nullable = false, foreignKey = @ForeignKey(name = "ride_template_to_stop_fk"))
    private Stop toPlace;

    @PositiveOrZero
    @Max(99)
    @Column(name = "nr_seats_available")
    private int nrSeatsAvailable;

    @Size(max = 256)
    @Column(length = 256)
    private String remarks;

    /**
     * Estimated distance in [meter].
     */
    @PositiveOrZero
    @Column(name = "estimated_distance")
    private Integer estimatedDistance;
    
    /**
     * Estimated driving time in [second].
     */
    @PositiveOrZero
    @Column(name = "estimated_driving_time")
    private Integer estimatedDrivingTime;
    
    /**
     * Estimated emission of CO2 [g].
     */
    @PositiveOrZero
    @Column(name = "estimated_co2_emission")
    private Integer estimatedCO2Emission;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "share_eligibility")
    private Geometry shareEligibility; 
    
    @Positive
    @Column(name = "max_detour_meters")
    private Integer maxDetourMeters;
    
    @Positive
    @Column(name = "max_detour_seconds")
    private Integer maxDetourSeconds;
    
    @Positive
    @Column(name = "carthesian_distance")
    private Integer carthesianDistance;

    @Column(name = "carthesian_bearing")
    private Integer carthesianBearing;

    @Embedded
    private Recurrence recurrence;

    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public User getDriver() {
		return driver;
	}

	public void setDriver(User driver) {
		this.driver = driver;
		this.driverRef = null;
	}

	public Car getCar() {
		return car;
	}

	public void setCar(Car car) {
		this.car = car;
		this.carRef = null;
	}

	public String getCarRef() {
    	if (car != null) {
    		carRef = RideshareUrnHelper.createUrn(Car.URN_PREFIX, car.getId());
    	}
		return carRef;
	}

	public void setCarRef(String carRef) {
		this.carRef = carRef;
	}

	public Stop getFromPlace() {
		return fromPlace;
	}

	public void setFromPlace(Stop fromPlace) {
		this.fromPlace = fromPlace;
	}

	public Stop getToPlace() {
		return toPlace;
	}

	public void setToPlace(Stop toPlace) {
		this.toPlace = toPlace;
	}

	public String getDriverRef() {
    	if (driver != null) {
    		driverRef = RideshareUrnHelper.createUrn(User.URN_PREFIX, driver.getId());
    	}
		return driverRef;
	}


	public void setDriverRef(String driverRef) {
		this.driverRef = driverRef;
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

	public Integer getEstimatedDistance() {
		return estimatedDistance;
	}

	public void setEstimatedDistance(Integer estimatedDistance) {
		this.estimatedDistance = estimatedDistance;
	}

	public Integer getEstimatedDrivingTime() {
		return estimatedDrivingTime;
	}

	public void setEstimatedDrivingTime(Integer estimatedDrivingTime) {
		this.estimatedDrivingTime = estimatedDrivingTime;
	}

	public Integer getEstimatedCO2Emission() {
		return estimatedCO2Emission;
	}

	public void setEstimatedCO2Emission(Integer estimatedCO2Emission) {
		this.estimatedCO2Emission = estimatedCO2Emission;
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
	public Recurrence getRecurrence() {
		return recurrence;
	}
	public void setRecurrence(Recurrence recurrence) {
		this.recurrence = recurrence;
	}


}
