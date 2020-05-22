package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.Instant;
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
	
	public Stop(GeoLocation geoloc) {
		this(geoloc, null, null);
	}
	
	public Stop(GeoLocation geoloc, Instant anDepartureTime, Instant anArrivalTime) {
		this.location = geoloc;
		this.departureTime = anDepartureTime;
		this.arrivalTime = anArrivalTime;
	}

	public Stop(Stop other) {
    	this.location = new GeoLocation(other.location);
    	this.arrivalTime = other.arrivalTime;
    	this.departureTime = other.departureTime;
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

	@Override
	public int hashCode() {
		return Objects.hash(arrivalTime, departureTime, location);
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
				&& Objects.equals(location, other.location);
	}

	@Override
	public String toString() {
		return location.toString();
	}
}
