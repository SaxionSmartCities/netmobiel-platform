package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedSubgraph;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

/**
 * The booking is the actual proposal to join a ride. The pickup and drop-off are already negotiated. These stops are 
 * not necessarily the actual departure and destination location of the passenger.
 * 
 * @author Jaap Reitsma
 *
 */

@NamedNativeQueries({
	@NamedNativeQuery(
		name = "ListBookingConfirmedCount",
		query = "select u.managed_identity as managed_identity, "
        		+ "date_part('year', m.created_time) as year, " 
        		+ "date_part('month', m.created_time) as month, "
        		+ "count(*) as count "
        		+ "from booking b "
        		+ "join rs_user u on u.id = b.passenger "
        		+ "where b.departure_time >= ? and b.departure_time < ? and b.state = 'CFM' "
        		+ "group by u.managed_identity, year, month "
        		+ "order by u.managed_identity, year, month",
        resultSetMapping = "ListBookingCountMapping"),
	@NamedNativeQuery(
			name = "ListBookingCancelledByPassengerCount",
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', m.created_time) as year, " 
	        		+ "date_part('month', m.created_time) as month, "
	        		+ "count(*) as count "
	        		+ "from booking b "
	        		+ "join rs_user u on u.id = b.passenger "
	        		+ "where b.departure_time >= ? and b.departure_time < ? and b.state = 'CNC' " 
	        		+ " and (b.cancelled_by_driver = false or b.cancelled_by_driver is null)"
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = "ListBookingCountMapping"),
	@NamedNativeQuery(
			name = "ListBookingCancelledByDriverCount",
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', m.created_time) as year, " 
	        		+ "date_part('month', m.created_time) as month, "
	        		+ "count(*) as count "
	        		+ "from booking b "
	        		+ "join rs_user u on u.id = b.passenger "
	        		+ "where b.departure_time >= ? and b.departure_time < ? and b.state = 'CNC' " 
	        		+ " and b.cancelled_by_driver = true "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = "ListBookingCountMapping"),
})
@SqlResultSetMapping(
	name = "ListBookingCountMapping", 
	classes = @ConstructorResult(
		targetClass = NumericReportValue.class, 
		columns = {
				@ColumnResult(name = "managed_identity", type = String.class),
				@ColumnResult(name = "year", type = int.class),
				@ColumnResult(name = "month", type = int.class),
				@ColumnResult(name = "count", type = int.class)
		}
	)
)

@NamedEntityGraph(
		name = Booking.SHALLOW_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "state"),		
				@NamedAttributeNode(value = "nrSeats"),		
				@NamedAttributeNode(value = "passenger", subgraph = "passenger-details"),		
				@NamedAttributeNode(value = "departureTime"),		
				@NamedAttributeNode(value = "arrivalTime"),		
				@NamedAttributeNode(value = "pickup"),		
				@NamedAttributeNode(value = "dropOff"),		
				@NamedAttributeNode(value = "cancelReason"),		
				@NamedAttributeNode(value = "cancelledByDriver"),		
				@NamedAttributeNode(value = "legs", subgraph = "leg-details"),		
		}, subgraphs = {
				@NamedSubgraph(
						name = "leg-details",
						attributeNodes = {
								@NamedAttributeNode(value = "id")
						}
					),
				@NamedSubgraph(
						name = "passenger-details",
						attributeNodes = {
								@NamedAttributeNode(value = "id"),
								@NamedAttributeNode(value = "email"),
								@NamedAttributeNode(value = "familyName"),
								@NamedAttributeNode(value = "givenName"),
								@NamedAttributeNode(value = "managedIdentity")
						}
					)
		}
	)
@NamedEntityGraph(
		name = Booking.DEEP_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "state"),		
				@NamedAttributeNode(value = "nrSeats"),		
				@NamedAttributeNode(value = "passenger"),		
				@NamedAttributeNode(value = "departureTime"),		
				@NamedAttributeNode(value = "arrivalTime"),		
				@NamedAttributeNode(value = "pickup"),		
				@NamedAttributeNode(value = "dropOff"),		
				@NamedAttributeNode(value = "cancelReason"),		
				@NamedAttributeNode(value = "cancelledByDriver"),		
				@NamedAttributeNode(value = "legs", subgraph = "leg-details"),		
				@NamedAttributeNode(value = "ride", subgraph = "ride-details"),		
		}, subgraphs = {
				@NamedSubgraph(
						name = "leg-details",
						attributeNodes = {
								@NamedAttributeNode(value = "id"),
								@NamedAttributeNode(value = "distance"),
								@NamedAttributeNode(value = "duration"),
								@NamedAttributeNode(value = "from"),
								@NamedAttributeNode(value = "to"),
								@NamedAttributeNode(value = "legGeometry"),
						}
					),
				@NamedSubgraph(
						name = "ride-details",
						attributeNodes = {
								@NamedAttributeNode(value = "id"),
								@NamedAttributeNode(value = "driver"),
								@NamedAttributeNode(value = "car")
						}
					)
		}
	)
@Entity
@Vetoed
@Table(name = "booking")
@SequenceGenerator(name = "booking_sg", sequenceName = "booking_id_seq", allocationSize = 1, initialValue = 50)
public class Booking implements Serializable {
	private static final long serialVersionUID = 3727019200633708992L;
	public static final String URN_PREFIX = RideshareUrnHelper.createUrnPrefix("booking");
	public static final String SHALLOW_ENTITY_GRAPH = "booking-shallow-details-graph";
	public static final String DEEP_ENTITY_GRAPH = "booking-deep-details-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_sg")
    private Long id;

    @Transient
    private String bookingRef;

	/**
	 * The state of the booking.
	 */
    @Column(name = "state", length = 3)
    private BookingState state;

    /**
     * Number of seats occupied by this booking 
     */
    @Positive
    @Max(99)
    @Column(name = "nr_seats")
    private int nrSeats;

    /**
     * The passenger for whom the booking is made. 
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "passenger", nullable = false, foreignKey = @ForeignKey(name = "booking_passenger_fk"))
    private RideshareUser passenger;

    /**
     * The ride carrying the booking.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride", nullable = false, foreignKey = @ForeignKey(name = "booking_ride_fk"))
    private Ride ride;

    /**
     * The intended pickup time.
     */
    @NotNull
    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;
    
    /**
     * The intended drop-off time
     */
    @NotNull
    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    /**
     * The pickup location of the passenger.  
     */
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "from_label", length = 256)), 
    	@AttributeOverride(name = "point", column = @Column(name = "from_point")), 
   	} )
    private GeoLocation pickup;
    
    /**
     * The drop-off location of the passenger.  
     */
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "to_label", length = 256)), 
    	@AttributeOverride(name = "point", column = @Column(name = "to_point")), 
   	} )
    private GeoLocation dropOff;


    /**
     * If the booking is cancelled, the reason for canceling.
     */
    @Size(max = 256)
    @Column(name = "cancel_reason", length = 256)
    private String cancelReason;
    
    /**
     * If true then the booking was canceled by the driver.
     */
    @Column(name = "cancelled_by_driver")
    private Boolean cancelledByDriver;


    /**
     * The reference to the passenger as known in the rideshare
     */
    @Transient
    private String passengerRef;

    /**
     * The legs this booking is involved in.
     */
    @ManyToMany(mappedBy = "bookings", fetch = FetchType.LAZY)
    @OrderBy("legIx asc")
    private List<Leg> legs;

	/**
	 * The reference to the passenger's trip (a planner URN). 
	 */
    @Column(name = "passenger_trip_ref", length = 32, nullable = false)
    private String passengerTripRef;

    /**
     * No-args constructor.
     */
    public Booking() {
    	
    }
    
    public Booking(Ride ride, RideshareUser passenger, GeoLocation pickup, GeoLocation dropOff, Integer nrSeats) {
    	this.dropOff = new GeoLocation(dropOff);
    	this.nrSeats = nrSeats;
    	this.passenger = passenger;
    	this.pickup = new GeoLocation(pickup);
    	this.ride = ride;
    	this.state = BookingState.NEW;
    }
    
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBookingRef() {
		if (bookingRef == null) {
			bookingRef = RideshareUrnHelper.createUrn(Booking.URN_PREFIX, getId());
		}
		return bookingRef;
	}

	public BookingState getState() {
		return state;
	}

	public void setState(BookingState state) {
		this.state = state;
	}

	public int getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(int nrSeats) {
		this.nrSeats = nrSeats;
	}

	public Instant getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(Instant departureTime) {
		this.departureTime = departureTime;
	}

	public Instant getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Instant arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public GeoLocation getPickup() {
		return pickup;
	}

	public void setPickup(GeoLocation pickup) {
		this.pickup = pickup;
	}

	public GeoLocation getDropOff() {
		return dropOff;
	}

	public void setDropOff(GeoLocation dropOff) {
		this.dropOff = dropOff;
	}

	public Ride getRide() {
		return ride;
	}

	public void setRide(Ride ride) {
		this.ride = ride;
	}

	public String getCancelReason() {
		return cancelReason;
	}

	public void setCancelReason(String cancelReason) {
		this.cancelReason = cancelReason;
	}

	public Boolean getCancelledByDriver() {
		return cancelledByDriver;
	}

	public void setCancelledByDriver(Boolean cancelledByDriver) {
		this.cancelledByDriver = cancelledByDriver;
	}

	public RideshareUser getPassenger() {
		return passenger;
	}

	public void setPassenger(RideshareUser passenger) {
		this.passenger = passenger;
		this.passengerRef = null;
	}

	public String getPassengerRef() {
    	if (passenger != null) {
    		passengerRef = UrnHelper.createUrn(RideshareUser.URN_PREFIX, passenger.getId());
    	}
		return passengerRef;
	}

	public boolean isDeleted() {
		return getState() == BookingState.CANCELLED;
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

	public String getPassengerTripRef() {
		return passengerTripRef;
	}

	public void setPassengerTripRef(String passengerTripRef) {
		this.passengerTripRef = passengerTripRef;
	}

	public void markAsCancelled(String reason, boolean byDriver) {
		this.state = BookingState.CANCELLED;
		this.cancelReason = reason;
		this.cancelledByDriver = byDriver;
	}

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
	@Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
         if (!(o instanceof Booking)) {
            return false;
        }
        Booking other = (Booking) o;
        return id != null && id.equals(other.getId());
    }

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
    @Override
    public int hashCode() {
        return 31;
    }

	@Override
	public String toString() {
		return String.format("Booking %d on Ride %d %s %s D %s A %s from %s to %s #%d seat(s)",
				getId(),
				getRide() != null ? getRide().getId() : "<null>",
				getPassenger().getManagedIdentity(), 
				getState().name(), 
				DateTimeFormatter.ISO_INSTANT.format(getDepartureTime()), 
				DateTimeFormatter.ISO_INSTANT.format(getArrivalTime()),
				getPickup().toString(), 
				getDropOff().toString(),
				getNrSeats());
	}

}
