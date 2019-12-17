package eu.netmobiel.rideshare.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.json.bind.annotation.JsonbTransient;
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
     * The ride the stop is connected to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride", nullable = true, foreignKey = @ForeignKey(name = "stop_ride_fk"))
    private Ride ride;

	public Stop() {
	}
	
	public Stop(GeoLocation geoloc) {
		this.location = geoloc;
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

	public Ride getRide() {
		return ride;
	}

	public void setRide(Ride ride) {
		this.ride = ride;
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

}
