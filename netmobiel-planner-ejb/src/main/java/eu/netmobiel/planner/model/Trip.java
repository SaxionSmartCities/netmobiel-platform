package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@NamedEntityGraph(
		name = Trip.LIST_TRIPS_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "stops"),		
				@NamedAttributeNode(value = "legs", subgraph = "leg-details")		
		}, subgraphs = {
				// Without this subgraph no leg details are retrieved
				@NamedSubgraph(
						name = "leg-details",
						attributeNodes = {
								@NamedAttributeNode(value = "guideSteps")
						}
					)
				}

	)
@NamedEntityGraph(
		name = Trip.LIST_TRIP_DETAIL_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "stops"),		
				@NamedAttributeNode(value = "legs", subgraph = "leg-details"),		
				@NamedAttributeNode(value = "traveller"),		
		}, subgraphs = {
				// Without this subgraph no leg details are retrieved
				@NamedSubgraph(
						name = "leg-details",
						attributeNodes = {
								@NamedAttributeNode(value = "guideSteps")
						}
					)
				}

	)
@Entity
@Table(name = "trip")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "trip_sg", sequenceName = "trip_id_seq", allocationSize = 1, initialValue = 50)
public class Trip extends Itinerary implements Serializable {

	private static final long serialVersionUID = -3789784762166689720L;
	public static final String LIST_TRIPS_ENTITY_GRAPH = "list-trips-graph";
	public static final String LIST_TRIP_DETAIL_ENTITY_GRAPH = "list-trip-detail-graph";

	public static final String URN_PREFIX = PlannerUrnHelper.createUrnPrefix(Trip.class);
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_sg")
    private Long id;

    @Transient
    private String tripRef;
    
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

    /**
     * In case of rideshare: The number of seats requested.
     */
    @Positive
    @Column(name = "nr_seats")
    private int nrSeats = 1;



	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTripRef() {
    	if (tripRef == null) {
    		tripRef = PlannerUrnHelper.createUrn(Trip.URN_PREFIX, getId());
    	}
		return tripRef;
	}

	public String getTravellerRef() {
    	if (travellerRef == null) {
    		travellerRef = PlannerUrnHelper.createUrn(User.URN_PREFIX, traveller.getId());
    	}
		return travellerRef;
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

	/**
	 * Searches through the legs of this trip for a leg with a specific tripId. The trip id is a reference from the transport provider
	 * and refers to a specific ride of a vehicle, both in rideshare and in public transport. In public transport the tripId refers 
	 * to a specific instance of a ride over a route in a day, but not necessarily unique over time. In rideshare the tripId is an unique id.  
	 * @param tripId the trip id to find
	 * @return An Optional with the leg containing the trip id or null if not found.  
	 */
	public Optional<Leg> findLegByTripId(String tripId) {
		Leg leg = null;
		if (getLegs() != null) {
			leg = getLegs().stream().filter(lg -> tripId.equals(lg.getTripId())).findFirst().orElse(null);
		}
		return Optional.ofNullable(leg);
	}

	/**
	 * Searches through the legs of this trip for a leg with a specific bookingId.  
	 * @param bookingId the booking id to find
	 * @return An Optional with the leg containing the booking id or null if not found.  
	 */
	public Optional<Leg> findLegByBookingId(String bookingId) {
		Leg leg = null;
		if (getLegs() != null) {
			leg = getLegs().stream().filter(lg -> bookingId.equals(lg.getBookingId())).findFirst().orElse(null);
		}
		return Optional.ofNullable(leg);
	}

	//    private String formatTime(Instant instant) {
//    	return DateTimeFormatter.ISO_DATE_TIME.format(instant.atOffset(ZoneOffset.UTC).toLocalDateTime());
//    }

    public int getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(int nrSeats) {
		this.nrSeats = nrSeats;
	}

	private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

	@Override
	public String toString() {
		return String.format("Trip %s %s D %s A %s from %s to %s\n\t%s",
				getTravellerRef(), state.name(), 
				formatTime(getDepartureTime()), formatTime(getArrivalTime()),
				getFrom().toString(), getTo().toString(),
				super.toStringCompact());
	}


}
