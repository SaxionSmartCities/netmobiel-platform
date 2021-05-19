package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.enterprise.inject.Vetoed;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
import javax.validation.constraints.NotNull;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride", foreignKey = @ForeignKey(name = "stop_ride_fk"), nullable = false)
	private Ride ride;

	@NotNull
	@Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "label", length = 256)), 
    	@AttributeOverride(name = "point", column = @Column(name = "point", nullable = false)), 
   	} )
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

	public Ride getRide() {
		return ride;
	}

	public void setRide(Ride ride) {
		this.ride = ride;
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

	public boolean isDistanceLessThan(Stop other, int distanceInMeters) {
		return getLocation().getDistanceFlat(other.getLocation()) < distanceInMeters / 1000.0;
	}

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
	@Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
        if (!(o instanceof Stop)) {
            return false;
        }
        Stop other = (Stop) o;
        return id != null && id.equals(other.getId());
    }
	
	@Override
	public int hashCode() {
		return 31;
	}

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atOffset(ZoneOffset.UTC));
    }
    
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Stop ").append(id).append(" ");
		builder.append(location.toString()).append(" ");
		if (arrivalTime != null) {
			builder.append("A ").append(formatTime(arrivalTime)).append(" ");
		}
		if (departureTime != null) {
			builder.append("D ").append(formatTime(departureTime));
		}
		return builder.toString();
	}
}
