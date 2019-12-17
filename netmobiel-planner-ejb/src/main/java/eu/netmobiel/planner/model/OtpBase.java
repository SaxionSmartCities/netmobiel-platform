package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Vetoed
public class OtpBase implements Serializable {
	private static final long serialVersionUID = -8837056996502612302L;

	@Id
    @Column(name = "id", length = 64)
    private String id;
    
    @Column(name = "gtfs_id", length = 64, unique = true)
    private String gtfsId;
    

	public OtpBase() {
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getGtfsId() {
		return gtfsId;
	}

	public void setGtfsId(String gtfsId) {
		this.gtfsId = gtfsId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
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
		OtpBase other = (OtpBase) obj;
		return Objects.equals(id, other.id);
	}

}
