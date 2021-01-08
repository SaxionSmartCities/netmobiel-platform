package eu.netmobiel.planner.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.json.bind.annotation.JsonbProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@IdClass(OtpTransferId.class)
@Vetoed
@Table(name = "otp_transfer")
public class OtpTransfer implements Serializable {
	private static final long serialVersionUID = 9200358705937130102L;

	/**
	 * Distance between two stops
	 */
	@Column(name = "distance", nullable = false)
	private int distance;

	@Id
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_stop", nullable = false, foreignKey = @ForeignKey(name = "otp_transfer_from_stop_fk"))
    private OtpStop fromStop; 

	@JsonbProperty("stop")
	@Id
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_stop", nullable = false, foreignKey = @ForeignKey(name = "otp_transfer_to_stop_fk"))
    private OtpStop toStop; 

    public OtpTransfer() {
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public OtpStop getFromStop() {
		return fromStop;
	}

	public void setFromStop(OtpStop fromStop) {
		this.fromStop = fromStop;
	}

	public OtpStop getToStop() {
		return toStop;
	}

	public void setToStop(OtpStop toStop) {
		this.toStop = toStop;
	}

}
