package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.Vetoed;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.rideshare.util.RideshareUrnHelper;

/**
 * A Ride is simple or recurrent. A recurrent Ride is copied from a template. A simple Ride has no template.
 *  
 * @author Jaap Reitsma
 *
 */
@NamedEntityGraph(
	name = Ride.SEARCH_RIDES_ENTITY_GRAPH, 
	attributeNodes = { 
			@NamedAttributeNode(value = "car"),		
			@NamedAttributeNode(value = "driver")
	}
)
@NamedEntityGraph(
		name = Ride.LIST_RIDES_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "bookings", subgraph = "booking-details"),		
				@NamedAttributeNode(value = "car"),		
				@NamedAttributeNode(value = "driver"),
				@NamedAttributeNode(value = "rideTemplate", subgraph = "template-details")		
		}, subgraphs = {
				@NamedSubgraph(
						name = "template-details",
						attributeNodes = {
								@NamedAttributeNode(value = "recurrence")
						}
					),
				@NamedSubgraph(
						name = "booking-details",
						attributeNodes = {
								@NamedAttributeNode(value = "passenger")
						}
					)
		}
	)
// Get the details of a ride: template recurrence, car, driver, legs. The legs do not specify bookings. 
@NamedEntityGraph(
	name = Ride.DETAILS_WITH_LEGS_ENTITY_GRAPH, 
	attributeNodes = { 
		@NamedAttributeNode(value = "car"),		
		@NamedAttributeNode(value = "driver"),		
		@NamedAttributeNode(value = "legs"),		
		@NamedAttributeNode(value = "rideTemplate", subgraph = "template-details")		
	}, subgraphs = {
			@NamedSubgraph(
					name = "template-details",
					attributeNodes = {
							@NamedAttributeNode(value = "recurrence")
					}
				)
	}
)
@NamedEntityGraph(
		name = Ride.UPDATE_DETAILS_ENTITY_GRAPH, 
		attributeNodes = { 
				// Do not include bookings too, that gives an error message: Cannot fetch multiple bags.
			@NamedAttributeNode(value = "bookings"),		
			@NamedAttributeNode(value = "car"),		
			@NamedAttributeNode(value = "driver"),		
			@NamedAttributeNode(value = "rideTemplate")		
		}
	)
@Entity
@Table(name = "ride")
@Vetoed
@SequenceGenerator(name = "ride_sg", sequenceName = "ride_id_seq", allocationSize = 1, initialValue = 50)
public class Ride extends RideBase implements Serializable {
	private static final long serialVersionUID = 4342765799358026502L;
	public static final String URN_PREFIX = RideshareUrnHelper.createUrnPrefix("ride");
	public static final String SEARCH_RIDES_ENTITY_GRAPH = "search-rides-graph";
	public static final String LIST_RIDES_ENTITY_GRAPH = "list-rides-graph";
	public static final String DETAILS_WITH_LEGS_ENTITY_GRAPH = "ride-details-graph";
	public static final String UPDATE_DETAILS_ENTITY_GRAPH = "ride-update-details-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ride_sg")
    private Long id;

	@Version
	@Column(name = "version")
	private int version;
	
    @Transient
    private String rideRef;

    /**
     * A recurrent ride has a ride template, shared by multiple rides. The pattern is used to instantiate new rides from the template(s), but also 
     * the recognize the instances that were derived from the same template.
     * A new template is saved before the ride is saved. Existing templates are not touched.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	@JoinColumn(name = "ride_template", nullable = true, foreignKey = @ForeignKey(name = "ride_ride_template_fk"))
    private RideTemplate rideTemplate;


    /**
     * The reason for cancelling a ride.
     */
    @Size(max = 256)
    @Column(name = "cancel_reason", length = 256)
    private String cancelReason;
    
    /**
     * If true the ride is soft-deleted. If a ride was only planned (no bookings), it will be hard deleted.
     */
    @Column(name = "deleted")
    private Boolean deleted;

    /**
     * The bookings on this ride. Currently at most one non-deleted (cancelled) booking.
     * Once a booking is present, a ride is no longer hard removed.
     */
    @OneToMany (mappedBy = "ride", fetch = FetchType.LAZY)
    private List<Booking> bookings;

    /**
     * The stops (vertices) in this ride. The stops are ordered.
     * The life cycle of a stop differs from a ride. The stops are updated with each change in the itinerary.
     * Stops cannot exists outside a ride. 
     */
	@OneToMany(mappedBy="ride", cascade = { CascadeType.REMOVE }, fetch = FetchType.LAZY)
	@OrderColumn(name = "stop_ix")
	private List<Stop> stops;

	/**
     * The legs (edges) in this ride. The legs are ordered.
     * The life cycle of a leg differs from a ride. The legs are updated with each change in the itinerary.
     * Legs cannot exists outside a ride. 
     */
	@OneToMany(mappedBy="ride", cascade = { CascadeType.REMOVE }, fetch = FetchType.LAZY)
	@OrderBy("legIx asc")
	private List<Leg> legs;

	/**
	 * The state of the ride. 
	 */
	@NotNull
    @Column(name = "state", length = 3)
    private RideState state;

    /**
     * If true then the ride is being monitored.
     */
    @Column(name = "monitored", nullable = false)
    private boolean monitored;

    /**
     * If true then the ride is confirmed by the driver.
     */
    @Column(name = "confirmed")
    private Boolean confirmed;
    
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public RideTemplate getRideTemplate() {
		return rideTemplate;
	}

	public void setRideTemplate(RideTemplate rideTemplate) {
		this.rideTemplate = rideTemplate;
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

	public boolean isDeleted() {
		return getDeleted() != null ? getDeleted() : false;
	}
	
	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public List<Booking> getBookings() {
		if (bookings == null) {
			bookings = new ArrayList<>();
		}
		return bookings;
	}

	public void setBookings(List<Booking> bookings) {
		this.bookings = bookings;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public List<Stop> getStops() {
		if (stops == null) {
			stops = new ArrayList<>();
		}
		return stops;
	}

	public void setStops(List<Stop> stops) {
		this.stops = stops;
	}

	public List<Leg> getLegs() {
		if (legs == null) {
			legs = new ArrayList<>();
		}
		return legs;
	}

	public void setLegs(List<Leg> legs) {
		this.legs = legs;
	}

	public RideState getState() {
		return state;
	}

	public void setState(RideState state) {
		this.state = state;
	}

	public boolean isMonitored() {
		return monitored;
	}

	public void setMonitored(boolean monitored) {
		this.monitored = monitored;
	}

	public Boolean getConfirmed() {
		return confirmed;
	}

	public void setConfirmed(Boolean confirmed) {
		this.confirmed = confirmed;
	}

	public String getRideRef() {
		if (rideRef == null) {
    		rideRef = RideshareUrnHelper.createUrn(Ride.URN_PREFIX, getId());
		}
		return rideRef;
	}

	/**
	 * Returns true if the specified ride overlaps in time with this ride.
	 * @param r the ride to compare.
	 * @return true if there is an overlap in trip time
	 */
	public boolean hasTemporalOverlap(Ride r) {
		return r.getDepartureTime().isBefore(getArrivalTime()) && r.getArrivalTime().isAfter(getDepartureTime());
	}

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    public void addStop(Stop stop) {
        getStops().add(stop);
        stop.setRide(this);
    }
 
    public void removeStop(Stop stop) {
        getStops().remove(stop);
        stop.setRide(null);
    }

    public void addLeg(Leg leg) {
        getLegs().add(leg);
        leg.setRide(this);
    }
 
    public void removeLeg(Leg leg) {
        getLegs().remove(leg);
        leg.setRide(null);
    }

    public void addBooking(Booking booking) {
        getBookings().add(booking);
        booking.setRide(this);
    }
 
    public void removeBooking(Booking booking) {
        getBookings().remove(booking);
        booking.setRide(null);
    }
    
    public boolean hasActiveBookingProcess() {
    	return getBookings().stream()
    			.filter(b -> b.getState() == BookingState.PROPOSED || b.getState() == BookingState.REQUESTED)
    			.findAny().isPresent();
    }

    public boolean hasActiveBooking() {
    	return getBookings().stream()
    			.filter(b -> b.getState() == BookingState.CONFIRMED)
    			.findAny().isPresent();
    }

    public Optional<Booking> getActiveBooking() {
    	return getBookings().stream()
    			.filter(b -> b.getState() == BookingState.CONFIRMED)
    			.findFirst();
    }

	public String toStringCompact() {
		return String.format("Ride %d %s %s D %s A %s %s %dm from %s to %s",
				getId(), getDriver().getEmail(), state == null ? "<init>" : state.name(), 
				formatTime(getDepartureTime()), formatTime(getArrivalTime()),
				Duration.ofSeconds(getDuration()).toString(), getDistance(),
				getFrom().toString(), getTo().toString());
	}

    @Override
    public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(toStringCompact());
		Stop previous = null;
		for (Leg leg : getLegs()) {
			if (previous == null) {
				builder.append("\n\t\t").append(leg.getFrom());
			} else if (! previous.equals(leg.getFrom())) {
				builder.append("\n\t\t").append(leg.getFrom());
			}
			builder.append("\n\t\t\t").append(leg);
			builder.append("\n\t\t").append(leg.getTo());
			previous = leg.getTo();
		}
		return builder.toString();
    }
}	
