package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import eu.netmobiel.commons.model.GeoLocation;

@Entity
@Vetoed
@Table(name = "stop")
@SequenceGenerator(name = "stop_sg", sequenceName = "stop_id_seq", allocationSize = 1, initialValue = 50)
public class Stop implements Serializable {
	private static final long serialVersionUID = -8837056996502612302L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stop_sg")
    private Long id;

    @Embedded
    private GeoLocation location;

	/** 
     * The "code" of the stop. Depending on the transit agency, this is often
     * something that users care about.
     */
	@Column(name = "stop_code", length = 32)
    private String stopCode;

    /**
     * The code or name identifying the quay/platform the vehicle will arrive at or depart from
     */
	@Column(name = "platform_code", length = 32)
    private String platformCode;

    /**
     * The time the traveller will arrive at the place.
     */
	@Column(name = "arrival_time")
    private Instant arrivalTime;

    /**
     * The time the traveller will depart from the place.
     */
	@Column(name = "departure_time")
    private Instant departureTime;

    public Stop() {
    }

//    public Stop(Double lon, Double lat, String name) {
//    	super(lat, lon, name);
//    }

//    public Stop(Double lon, Double lat, String name, Instant arrival, Instant departure) {
//        this(lon, lat, name);
//        this.arrivalTime = arrival;
//        this.departureTime = departure;
//    }
	
	public Stop(GeoLocation geoloc) {
		this.location = geoloc;
	}
	
    public Stop(Stop other) {
    	this.location = new GeoLocation(other.location);
    	this.arrivalTime = other.arrivalTime;
    	this.departureTime = other.departureTime;
    	this.platformCode = other.platformCode;
    	this.stopCode = other.stopCode;
    }
    
    public Stop copy() {
    	return new Stop(this);
    }
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public GeoLocation getLocation() {
		return location;
	}

	public void setLocation(GeoLocation location) {
		this.location = location;
	}

	public Double getLatitude() {
		return location.getLatitude();
	}

	private GeoLocation setLocationThen() {
		if (this.location == null) {
			this.location = new GeoLocation();
		}
		return this.location;
	}
	
	public void setLatitude(Double latitude) {
		setLocationThen().setLatitude(latitude);
	}

	public Double getLongitude() {
		return location.getLongitude();
	}

	public void setLongitude(Double longitude) {
		setLocationThen().setLongitude(longitude);
	}

	public String getLabel() {
		return location.getLabel();
	}

	public void setLabel(String label) {
		setLocationThen().setLabel(label);
	}

	public String getStopCode() {
		return stopCode;
	}

	public void setStopCode(String stopCode) {
		this.stopCode = stopCode;
	}

	public String getPlatformCode() {
		return platformCode;
	}

	public void setPlatformCode(String platformCode) {
		this.platformCode = platformCode;
	}

	public Instant getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Instant arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public Instant getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(Instant departureTime) {
		this.departureTime = departureTime;
	}

}
