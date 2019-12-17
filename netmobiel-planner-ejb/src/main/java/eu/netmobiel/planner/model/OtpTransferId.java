package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.util.Objects;

public class OtpTransferId implements Serializable {
	private static final long serialVersionUID = -468978946588126947L;
	/**
	 * The attributes names should match the parent. The type should the type of the PK of the parent (!).
	 */
	private String fromStop; 
    private String toStop; 

    public OtpTransferId() {
	}

    public OtpTransferId(String from, String to) {
    	this.fromStop = from;
    	this.toStop = to;
	}

    public String getFromStop() {
		return fromStop;
	}

	public String getToStop() {
		return toStop;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fromStop, toStop);
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
		OtpTransferId other = (OtpTransferId) obj;
		return Objects.equals(fromStop, other.fromStop) && Objects.equals(toStop, other.toStop);
	}
	
}
