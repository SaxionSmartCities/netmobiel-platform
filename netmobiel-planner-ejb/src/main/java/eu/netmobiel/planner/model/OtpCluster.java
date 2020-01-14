package eu.netmobiel.planner.model;

import java.util.BitSet;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import eu.netmobiel.opentripplanner.api.model.TransportationType;

@Entity
@Vetoed
@Table(name = "otp_cluster",
	uniqueConstraints = @UniqueConstraint(name = "otp_cluster_gtfs_id_uc", columnNames= { "gtfs_id" } )
)
@Access(AccessType.FIELD)
public class OtpCluster extends OtpLocatedEntity {
	private static final long serialVersionUID = -8837056996502612302L;

    @OneToMany(mappedBy = "cluster", fetch = FetchType.LAZY )
    private List<OtpStop> stops;

	/**
	 * Post-processing: number of stops in this cluster
	 */
	@Column(name = "nr_stops")
	private Integer nrStops;

	/**
	 * Post-processing: number of routes in this cluster
	 */
	@Column(name = "nr_routes")
	private Integer nrRoutes;

	/**
	 * Post-processing: transportation types used on this stop
	 */
	@Transient
	private BitSet transportationTypes = new BitSet(8);

	public OtpCluster() {
	}

	public List<OtpStop> getStops() {
		return stops;
	}

	public void setStops(List<OtpStop> stops) {
		this.stops = stops;
	}

	public Integer getNrStops() {
		return nrStops;
	}

	public void setNrStops(Integer nrStops) {
		this.nrStops = nrStops;
	}

	public Integer getNrRoutes() {
		return nrRoutes;
	}

	public void setNrRoutes(Integer nrRoutes) {
		this.nrRoutes = nrRoutes;
	}

	@Access(AccessType.PROPERTY)
    @Column(name = "transportation_types")
	protected int getTransportationTypeValues() {
		byte [] bytes = this.transportationTypes.toByteArray();
        return bytes.length > 0 ? bytes[0] & 0xFF : 0;
    }

	protected void setTransportationTypeValues(int types) {
        this.transportationTypes = BitSet.valueOf(new byte[] { (byte) (types & 0xFF) });
    }

	public BitSet getTransportationTypes() {
		return transportationTypes;
	}

	public void setTransportationTypes(BitSet transportationTypes) {
		this.transportationTypes = transportationTypes;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OtpCluster [");
		if (getLabel() != null) {
			builder.append(getLabel()).append(" ");
		}
		if (nrStops != null) {
			builder.append("#Stops ").append(nrStops).append(", ");
		}
		if (nrRoutes != null) {
			builder.append("#Routes ").append(nrRoutes).append(", ");
		}
		builder.append(TransportationType.listNames(getTransportationTypeValues()));
		builder.append("]");
		return builder.toString();
	}
    
}
