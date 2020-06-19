package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
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
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.util.PlannerUrnHelper;

/**
 * Model of a trip. A trip has an itinerary with legs and stops. A trip is executed by the traveller.
 * In the beginning, a trip may have a plan only, no itinerary. An itinerary is by definition calculated by the 
 * NetMobiel planner. In the very beginning there may be an idea of a trip (spatial displacement), but not yet 
 * how (modality) and when (temporal). This is why a trip has a departure and arrival location itself.
 * A leg is a precise definition of how, when and where. An itinerary is a collection of legs a summarises a few
 * characteristics like duration, distance, waiting time etc. 
 * 
 * Should a traveller be allowed to plan individual legs? It gets complicated when allowing so, a better design is to create 
 * partial trips, i.e., a trip consists then of one or more partial trips. Each partial trip is planned by the traveller.
 * With that model all model elements are reusable. For now we stick to a single trip in one piece.
 * 
 * TODO: The trip state 'Cancelled' is incorrect with regard to analysis. Better is to introduce a separate cancel flag.
 * With the flag the last trip state is kept, so that we can analyse in which state a trip is cancelled.
 * Alternative: Maintain a log of the trip state change.
 * 
 * A trip is created with a reference to an itinerary as calculated earlier by the planner. When a user does not get 
 * satisfactory results, he can issue a shout-out. A shout-out creates a trip, copies a reference plan as sets the 
 * modality to rideshare and posts then all potential drivers with the request. A driver can respond by offering a ride. 
 * The ride will be added as a itinerary in the shout-out plan. At some point in time the traveller will decide which 
 * ride (that is: itinerary) to select. From that on the process continues as with a ordinary planning: In case of rideshare 
 * a booking process is started. The driver confirms and then the trip is scheduled.  
 *  
 *  Once it is time, the trip will become in transit. When the traveller arrives, the trip is completed.  
 * 
 */
@Entity
@Table(name = "trip")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "trip_sg", sequenceName = "trip_id_seq", allocationSize = 1, initialValue = 50)
public class Trip implements Serializable {

	private static final long serialVersionUID = -3789784762166689720L;

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


    @Column(name = "state", length = 3)
    private TripState state;

    @OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "itinerary", nullable = true, foreignKey = @ForeignKey(name = "trip_itinerary_fk"))
    private Itinerary itinerary;

    @Transient
    private String itineraryRef;
    
    @OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan", nullable = true, foreignKey = @ForeignKey(name = "trip_plan_fk"))
    private TripPlan tripPlan;

    @Transient
    private String tripPlanRef;

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

    public int getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(int nrSeats) {
		this.nrSeats = nrSeats;
	}

	public Itinerary getItinerary() {
		return itinerary;
	}

	public void setItinerary(Itinerary itinerary) {
		this.itinerary = itinerary;
	}

	public String getItineraryRef() {
		if (itineraryRef == null) {
			itineraryRef = itinerary.getItineraryRef();
		}
		return itineraryRef;
	}

	public void setItineraryRef(String itineraryRef) {
		this.itineraryRef = itineraryRef;
		this.itinerary = null;
	}

	public TripPlan getTripPlan() {
		return tripPlan;
	}

	public void setTripPlan(TripPlan tripPlan) {
		this.tripPlan = tripPlan;
		this.tripPlanRef = null;
	}

	public String getTripPlanRef() {
		// Better not pull the plan if it is not there.
//		if (tripPlanRef == null) {
//			tripPlanRef = tripPlan.getTripPlanRef();
//		}
		return tripPlanRef;
	}

	public void setTripPlanRef(String tripPlanRef) {
		this.tripPlanRef = tripPlanRef;
		this.tripPlan = null;
	}

	private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

	@Override
	public String toString() {
		return String.format("Trip %d %s %s D %s A %s from %s to %s\n\t%s",
				getId(), traveller.getEmail(), state.name(), 
				formatTime(itinerary.getDepartureTime()), formatTime(itinerary.getArrivalTime()),
				getFrom().toString(), getTo().toString(),
				itinerary.toStringCompact());
	}


}
