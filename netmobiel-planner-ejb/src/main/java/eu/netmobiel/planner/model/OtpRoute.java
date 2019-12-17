package eu.netmobiel.planner.model;

import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Vetoed
@Table(name = "otp_route")
public class OtpRoute extends OtpBase {
	private static final long serialVersionUID = 5115176824377866798L;

	@Column(name = "short_name", length = 32)
	private String shortName;
	
	@Column(name = "long_name", length = 96)
	private String longName;
	
	@Column(name = "ov_type", nullable = false)
	private Integer type;

	@ManyToMany
	@JoinTable(name = "otp_route_stop", joinColumns = 
		@JoinColumn(name = "route_id", referencedColumnName = "id", foreignKey = 
			@ForeignKey(name = "otp_route_stop_route_fk")
		), inverseJoinColumns =
		@JoinColumn(name = "stop_id", referencedColumnName = "id", foreignKey = 
			@ForeignKey(name = "otp_route_stop_stop_fk")
		)
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<OtpStop> stops;

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getLongName() {
		return longName;
	}

	public void setLongName(String longName) {
		this.longName = longName;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public List<OtpStop> getStops() {
		return stops;
	}

	public void setStops(List<OtpStop> stops) {
		this.stops = stops;
	}

}
