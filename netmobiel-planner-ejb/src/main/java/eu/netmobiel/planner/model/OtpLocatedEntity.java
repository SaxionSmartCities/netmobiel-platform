package eu.netmobiel.planner.model;

import javax.enterprise.inject.Vetoed;
import javax.json.bind.annotation.JsonbProperty;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import eu.netmobiel.commons.model.GeoLocation;

@MappedSuperclass
@Vetoed
@Table(name = "cluster")
public class OtpLocatedEntity extends OtpBase {
	private static final long serialVersionUID = -8837056996502612302L;

	@Embedded
    private GeoLocation location;

	public OtpLocatedEntity() {
	}
	
	public GeoLocation getLocation() {
		return location;
	}

	public void setLocation(GeoLocation location) {
		this.location = location;
	}

	private GeoLocation setLocationThen() {
		if (this.location == null) {
			this.location = new GeoLocation();
		}
		return this.location;
	}
	
	public Double getLatitude() {
		return location.getLatitude();
	}

	@JsonbProperty("lat")
	public void setLatitude(Double latitude) {
		setLocationThen().setLatitude(latitude);
	}

	public Double getLongitude() {
		return location.getLongitude();
	}

	@JsonbProperty("lon")
	public void setLongitude(Double longitude) {
		setLocationThen().setLongitude(longitude);
	}

	public String getLabel() {
		return location.getLabel();
	}

	@JsonbProperty("name")
	public void setLabel(String label) {
		setLocationThen().setLabel(label);
	}
	
}
