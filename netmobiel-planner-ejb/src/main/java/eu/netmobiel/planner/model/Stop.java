package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

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
     * The ID of the stop. This is often something that users don't care about.
     */
	@Column(name = "stop_id", length = 32)
    private String stopId;

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

	public Stop(GeoLocation geoloc, Instant departureTime, Instant arrivalTime) {
		this.location = geoloc;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
	}
	
	public Stop(GeoLocation geoloc) {
		this(geoloc, null, null);
	}
	
    public Stop(Stop other) {
    	this.location = new GeoLocation(other.location);
    	this.arrivalTime = other.arrivalTime;
    	this.departureTime = other.departureTime;
    	this.platformCode = other.platformCode;
    	this.stopCode = other.stopCode;
    	this.stopId = other.stopId;
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

	public String getStopId() {
		return stopId;
	}

	public void setStopId(String stopId) {
		this.stopId = stopId;
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

    private static String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atOffset(ZoneOffset.UTC));
    }
    
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Stop ");
		builder.append(location.toString()).append(" ");
		if (arrivalTime != null) {
			builder.append("A ").append(formatTime(arrivalTime)).append(" ");
		}
		if (departureTime != null) {
			builder.append("D ").append(formatTime(departureTime));
		}
		if (stopId != null) {
			builder.append(" ").append(stopId);
		}
		if (stopCode != null) {
			builder.append(" ").append(stopCode);
		}
		if (platformCode != null) {
			builder.append(" ").append(platformCode);
		}
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(arrivalTime, departureTime, location, platformCode, stopCode, stopId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Stop other = (Stop) obj;
		return Objects.equals(arrivalTime, other.arrivalTime) && Objects.equals(departureTime, other.departureTime)
				&& Objects.equals(location, other.location) && Objects.equals(platformCode, other.platformCode)
				&& Objects.equals(stopCode, other.stopCode) && Objects.equals(stopId, other.stopId);
	}


	public int getWaitingTime() {
		return departureTime == null || arrivalTime == null ? 0 : Math.toIntExact(Duration.between(arrivalTime, departureTime).getSeconds());
	}

	public void shiftLinear(Duration delta) {
		if (departureTime != null) {
			departureTime = departureTime.plus(delta);
		}
		if (arrivalTime != null) {
			arrivalTime = arrivalTime.plus(delta);
		}
	}
}
