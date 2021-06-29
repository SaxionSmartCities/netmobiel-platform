package eu.netmobiel.planner.model;

import java.util.BitSet;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Vetoed
@Table(name = "otp_stop", 
	indexes = @Index(name = "stop_cluster_ix", columnList = "cluster"),
	uniqueConstraints = @UniqueConstraint(name = "otp_stop_gtfs_id_uc", columnNames= { "gtfs_id" } )
)
@Access(AccessType.FIELD)
public class OtpStop extends OtpLocatedEntity {
	private static final long serialVersionUID = -8837056996502612302L;

    @Column(name = "platform_code", length = 32)
    private String platformCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cluster", nullable = true, foreignKey = @ForeignKey(name = "otp_stop_cluster_fk"))
    private OtpCluster cluster; 
    
	@ManyToMany(mappedBy = "stops")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<OtpRoute> routes;

	@OneToMany(mappedBy = "fromStop", fetch = FetchType.LAZY)
	private List<OtpTransfer> transfers;
	
	/**
	 * Post-processing: number of routes in this stop is part of
	 */
	@Column(name = "nr_routes")
	private Integer nrRoutes;

	/**
	 * Post-processing: transportation types used on this stop
	 */
	@Transient
	private BitSet transportationTypes = new BitSet(8);
	
	public String getPlatformCode() {
		return platformCode;
	}

	public void setPlatformCode(String platformCode) {
		this.platformCode = platformCode;
	}

	public OtpCluster getCluster() {
		return cluster;
	}

	public void setCluster(OtpCluster cluster) {
		this.cluster = cluster;
	}

	public List<OtpRoute> getRoutes() {
		return routes;
	}

	public void setRoutes(List<OtpRoute> routes) {
		this.routes = routes;
	}

	public List<OtpTransfer> getTransfers() {
		return transfers;
	}

	public void setTransfers(List<OtpTransfer> transfers) {
		this.transfers = transfers;
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

}
