package eu.netmobiel.rideshare.filter;

import java.time.Instant;
import java.time.OffsetDateTime;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.RideState;
import eu.netmobiel.rideshare.model.RideshareUser;

public class RideFilter extends PeriodFilter {
	/** ==============================
	 * Selection of a specific user.
	 */
	private Long driverId;
	private RideshareUser driver;
	
	/** ==============================
	 * Selection by a state
	 */
	private RideState rideState;
	private BookingState bookingState;
	
	
	/** ==============================
	 * Sorting
	 */
	private SortDirection sortDir;

	
	/**
	 * Should we ignore the anonymous flag? Only when a user requests his own
	 * donations, or when an admin requests the overview In report queries the
	 * anonymous donations are *always* excluded from the reports.
	 */
	private boolean deletedToo;

	public RideFilter() {
	}

	public RideFilter(Long driverId, OffsetDateTime since, OffsetDateTime until, String rideState, String bookingState, String sortDir, boolean deletedToo) {
		this.driverId = driverId;
		setSince(since);
		setUntil(until);
		setRideState(rideState);
		setBookingState(bookingState);
		setSortDir(sortDir);
		this.deletedToo = deletedToo;
	}
	
	public RideFilter(RideshareUser driver, Instant since, Instant until) {
		this.driver = driver;
		setSince(since);
		setUntil(until);
	}

	public RideFilter(RideshareUser driver, Instant since, Instant until, RideState aRideState, BookingState aBookingState) {
		this(driver, since, until);
		this.rideState = aRideState;
		this.bookingState = aBookingState;
	}

	public Long getDriverId() {
		return driverId;
	}

	public void setDriverId(Long driverId) {
		this.driverId = driverId;
	}

	public final void setDriverId(String driverId) {
		this.driverId = UrnHelper.getId(RideshareUser.URN_PREFIX, driverId);
	}

	public RideshareUser getDriver() {
		return driver;
	}

	public void setDriver(RideshareUser driver) {
		this.driver = driver;
	}

	public boolean isDeletedToo() {
		return deletedToo;
	}

	public void setDeletedToo(boolean deletedToo) {
		this.deletedToo = deletedToo;
	}

	public RideState getRideState() {
		return rideState;
	}

	public void setRideState(RideState rideState) {
		this.rideState = rideState;
	}

	public final void setRideState(String rideState) {
		if (rideState != null) {
			this.rideState = RideState.valueOf(rideState);
		}
	}

	public BookingState getBookingState() {
		return bookingState;
	}

	public void setBookingState(BookingState bookingState) {
		this.bookingState = bookingState;
	}

	public final void setBookingState(String bookingState) {
		if (bookingState != null) {
			this.bookingState = BookingState.valueOf(bookingState);
		}
	}

	public SortDirection getSortDir() {
		return sortDir;
	}

	public void setSortDir(SortDirection sortDir) {
		this.sortDir = sortDir;
	}

	public final void setSortDir(String sortDir) {
		if (sortDir != null) {
			this.sortDir = SortDirection.valueOf(sortDir);
		}
	}

	public void validate() throws BadRequestException {
		super.validate();
    	if (this.sortDir == null) {
    		this.sortDir = SortDirection.ASC;
    	}
	}
}