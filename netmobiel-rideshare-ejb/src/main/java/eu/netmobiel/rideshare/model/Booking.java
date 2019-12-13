package eu.netmobiel.rideshare.model;

import java.io.Serializable;

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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

/**
 * The booking is the actual proposal to join a ride. The pickup and drop-off are already negotiated. These stops are 
 * not necessarily the actual departure and destination location of the passenger.
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Vetoed
@Table(name = "booking")
@SequenceGenerator(name = "booking_sg", sequenceName = "booking_id_seq", allocationSize = 1, initialValue = 50)
public class Booking implements Serializable {
	private static final long serialVersionUID = 3727019200633708992L;
	public static final String URN_PREFIX = RideshareUrnHelper.createUrnPrefix("booking");

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_sg")
    private Long id;

    @Column(name = "state", length = 3)
    private BookingState state;

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
    private User passenger;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride", nullable = false, foreignKey = @ForeignKey(name = "booking_ride_fk"))
    private Ride ride;

    @NotNull
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	@JoinColumn(name = "pickup", nullable = false, foreignKey = @ForeignKey(name = "booking_pickup_stop_fk"))
    private Stop pickup;

    @NotNull
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	@JoinColumn(name = "drop_off", nullable = false, foreignKey = @ForeignKey(name = "booking_drop_off_stop_fk"))
    private Stop dropOff;


    @Size(max = 256)
    @Column(name = "cancel_reason", length = 256)
    private String cancelReason;
    
    @Column(name = "cancelled_by_driver")
    private Boolean cancelledByDriver;


    @Transient
    private String passengerRef;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Stop getPickup() {
		return pickup;
	}

	public void setPickup(Stop pickup) {
		this.pickup = pickup;
	}

	public Stop getDropOff() {
		return dropOff;
	}

	public void setDropOff(Stop dropOff) {
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

	public User getPassenger() {
		return passenger;
	}

	public void setPassenger(User passenger) {
		this.passenger = passenger;
		this.passengerRef = null;
	}

	public String getPassengerRef() {
    	if (passenger != null) {
    		passengerRef = UrnHelper.createUrn(User.URN_PREFIX, passenger.getId());
    	}
		return passengerRef;
	}

	public void markAsCancelled(String reason, boolean byDriver) {
		this.state = BookingState.CANCELLED;
		this.cancelReason = reason;
		this.cancelledByDriver = byDriver;
	}
}
