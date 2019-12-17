package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
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
import javax.persistence.NamedEntityGraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@NamedEntityGraph
@Entity
@Table(name = "trip")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "trip_sg", sequenceName = "trip_id_seq", allocationSize = 1, initialValue = 50)
public class Trip extends Itinerary implements Serializable {

	private static final long serialVersionUID = -3789784762166689720L;

	public static final String URN_PREFIX = PlannerUrnHelper.createUrnPrefix("trip");
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_sg")
    private Long id;

    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "from_label")), 
    	@AttributeOverride(name = "point", column = @Column(name = "from_point")), 
   	} )
    private GeoLocation from;
    
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "to_label")), 
    	@AttributeOverride(name = "point", column = @Column(name = "to_point")), 
   	} )
    private GeoLocation to;

    @NotNull
    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "traveller", nullable = false, foreignKey = @ForeignKey(name = "trip_traveller_fk"))
    private User traveller;

    @Transient
    private String travellerRef;

    @Column(name = "state", length = 3)
    private TripState state;

    @Size(max = 256)
    @Column(name = "cancel_reason", length = 256)
    private String cancelReason;
    
    @Column(name = "deleted")
    private Boolean deleted;


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTravellerRef() {
    	if (traveller != null) {
    		travellerRef = PlannerUrnHelper.createUrn(User.URN_PREFIX, traveller.getId());
    	}
		return travellerRef;
	}


	public void setTravellerRef(String travellerRef) {
		this.travellerRef = travellerRef;
	}

	public User getTraveller() {
		return traveller;
	}

	public void setTraveller(User traveller) {
		this.traveller = traveller;
	}

	public TripState getState() {
		return state;
	}

	public void setState(TripState state) {
		this.state = state;
	}


	public String getCancelReason() {
		return cancelReason;
	}

	public void setCancelReason(String cancelReason) {
		this.cancelReason = cancelReason;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public GeoLocation getFrom() {
		return from;
	}

	public void setFrom(GeoLocation from) {
		this.from = from;
	}

	public GeoLocation getTo() {
		return to;
	}

	public void setTo(GeoLocation to) {
		this.to = to;
	}

	@Override
	public String toString() {
		return String.format("Trip [%s %s D %s A %s from %s to %s]",
				travellerRef, state.name(), 
				DateTimeFormatter.ISO_DATE_TIME.format(getDepartureTime()), DateTimeFormatter.ISO_DATE_TIME.format(getArrivalTime()),
				getFrom().toString(), getTo().toString());
	}


}
