package eu.netmobiel.rideshare.service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.annotation.Updated;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SoftRemovedException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.model.event.TripConfirmedByProviderEvent;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.event.RideStateUpdatedEvent;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideBase;
import eu.netmobiel.rideshare.model.RideMonitorEvent;
import eu.netmobiel.rideshare.model.RideScope;
import eu.netmobiel.rideshare.model.RideState;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.CarDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.RideTemplateDao;
import eu.netmobiel.rideshare.repository.UserDao;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;
/**
 * The manager for the rides. 
 * 
 * @author Jaap Reitsma
 *
 */
@Stateless
@Logging
public class RideManager {
	public static final Integer MAX_RESULTS = 10; 
	public static final String AGENCY_NAME = "NetMobiel Rideshare Service";
	public static final String AGENCY_ID = "NB:RS";
	public static final Integer MAX_BOOKINGS = 1;
	
	/**
	 * The driver has a vector, the passenger has a vector. 
	 * The difference in direction is limited for a match. Otherwise the driver has to drive in the
	 * wrong direction. The total bearing matching angle is twice the difference 
	 * (driver bearing - diff, driver bearing + diff). 
	 */
	private static final int MAX_BEARING_DIFFERENCE = 60; 	/* degrees */
//	private static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";
	private static final int HORIZON_WEEKS = 8;
	private static final int TEMPLATE_CURSOR_SIZE = 10;

	/**
	 * The duration of the departing state.
	 */
	private static final Duration DEPARTING_PERIOD = Duration.ofMinutes(15);
	/**
	 * The duration of the arriving state.
	 */
	private static final Duration ARRIVING_PERIOD = Duration.ofMinutes(15);
	/**
	 * The delay before sending a invitation for a confirmation.
	 */
	private static final Duration CONFIRMATION_DELAY = Duration.ofMinutes(15);
	/**
	 * The maximum duration of the first confirmation period.
	 */
	private static final Duration CONFIRM_PERIOD_1 = Duration.ofDays(2);
	/**
	 * The period after which to send a confirmation reminder, if necessary.
	 */
	private static final Duration CONFIRM_PERIOD_2 = Duration.ofDays(2);
	/**
	 * The total period after which a confirmation period expires.
	 */
	private static final Duration CONFIRMATION_PERIOD = CONFIRM_PERIOD_1.plus(CONFIRM_PERIOD_2);
	
	@Inject
    private Logger log;

    @Inject
    private UserDao userDao;
    @Inject
    private CarDao carDao;
    @Inject
    private RideDao rideDao;
    @Inject
    private BookingDao bookingDao;
    @Inject
    private RideTemplateDao rideTemplateDao;
    @Inject
    private RideItineraryHelper rideItineraryHelper;
    @Inject
    private IdentityHelper identityHelper;

    @Resource
    private SessionContext context;

    @Inject @Removed
    private Event<Ride> rideRemovedEvent;

    @Inject @Created
    private Event<Ride> rideCreatedEvent;

    @Resource
    private TimerService timerService;

    @Inject
    private Event<RideStateUpdatedEvent> rideStateUpdatedEvent;

	@Inject
    private Event<TripConfirmedByProviderEvent> transportProviderConfirmedEvent;

    /**
     * Updates all recurrent rides by advancing the system horizon to a predefined offset with reference to the calling time.
     * The state of the ride generation is saved in each template. Updating the template and saving the generated rides from that template
     * are part of the same transaction. 
     */
	@Schedule(info = "Ride Maintenance", hour = "2", minute = "15", second = "0", persistent = false /* non-critical job */)
	public void instantiateRecurrentRides() {
		try {
			// Retrieve a paged list of templates to update
			int offset = 0;
			int totalCount = 0;
			int count = 0;
			int errorCount = 0;
			Instant systemHorizon = getDefaultSystemHorizon();
			log.info(String.format("DB maintenance: Move horizon to %s", DateTimeFormatter.ISO_INSTANT.format(systemHorizon)));
			do {
				List<RideTemplate> templates = rideTemplateDao.findOpenTemplates(systemHorizon, offset, TEMPLATE_CURSOR_SIZE);
				count = templates.size();
				log.debug(String.format("Found %d open templates", count));
				for (RideTemplate template : templates) {
					try {
						// Force transaction demarcation
						totalCount += context.getBusinessObject(RideManager.class).instantiateRecurrentRides(template, systemHorizon);
					} catch (Exception ex) {
						log.error("Error generating rides", ex);
						errorCount++;
						if (errorCount >= 3) {
							log.error("Too many errors, aborting generation of ride instances");
							break;
						}
					}
				}
			} while (count >= TEMPLATE_CURSOR_SIZE);
			log.info(String.format("DB maintenance: %d rides inserted in total", totalCount));
		} catch (Exception ex) {
			log.error("Error fetching open ride templates: " + ex.toString());
		}
	}

	protected List<Ride> generateNonOverlappingRides(RideTemplate template, Instant systemHorizon) {
		// Get the ride instances that overlap or are beyond the template ride
		List<Ride> ridesBeyondTemplate  = rideDao.findRidesBeyondTemplate(template);
		List<Ride> rides = template.generateRides(systemHorizon);
		// Remove generated rides that overlap with existing future rides
		rides.removeIf(ride -> ridesBeyondTemplate.stream().filter(r -> r.hasTemporalOverlap(ride)).findFirst().isPresent());
		return rides;
	}

	protected Instant getDefaultSystemHorizon() {
		return ZonedDateTime.now(ZoneOffset.UTC).plusWeeks(HORIZON_WEEKS).toInstant();
	}
	/**
	 * Extends the recurrent rides, starting at the state date. A new transaction is used for each template.
     * @param template the template to generate the ride for, if required.
     * @param systemHorizon the system horizon, do not create past the horizon
     * @return the  number of rides created.  
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public int instantiateRecurrentRides(RideTemplate template, Instant systemHorizon) {
		// Load template into persistence context
		template = rideTemplateDao.merge(template);
		List<Ride> rides = generateNonOverlappingRides(template, systemHorizon);
		rides.forEach(r -> rideItineraryHelper.saveNewRide(r));
		return rides.size();
	}

    /**
     * List all rides owned by the calling user (driverId is null) or owned by the specified user. Soft deleted rides are omitted.
     * @return A list of rides owned by the calling user.
     * @throws BadRequestException 
     */
    public PagedResult<Ride> listRides(Long driverId, Instant since, Instant until, Boolean deletedToo, 
    		SortDirection sortDirection, Integer maxResults, Integer offset) throws NotFoundException, BadRequestException {
    	if (until != null && since != null && ! until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: The 'until' date must be greater than the 'since' date.");
    	}
    	if (maxResults != null && maxResults > 100) {
    		throw new BadRequestException("Constraint violation: 'maxResults' <= 100.");
    	}
    	if (maxResults != null && maxResults < 0) {
    		throw new BadRequestException("Constraint violation: 'maxResults' >= 0.");
    	}
    	if (offset != null && offset < 0) {
    		throw new BadRequestException("Constraint violation: 'offset' >= 0.");
    	}
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	User driver = null;
    	if (driverId == null) {
    		throw new BadRequestException("Constraint violation: 'driverId' is mandatory.");
    	}
    	driver = userDao.find(driverId)
    				.orElseThrow(() -> new NotFoundException("No such user: " + driverId));
    	List<Ride> results = Collections.emptyList();
        Long totalCount = 0L;
		PagedResult<Long> prs = rideDao.findByDriver(driver, since, until, deletedToo, sortDirection, 0, 0);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> rideIds = rideDao.findByDriver(driver, since, until, deletedToo, sortDirection, maxResults, offset);
    		if (rideIds.getData().size() > 0) {
    			results = rideDao.fetch(rideIds.getData(), Ride.LIST_RIDES_ENTITY_GRAPH);
    		}
    	}
    	return new PagedResult<Ride>(results, maxResults, offset, totalCount);
    }

    /**
     * Searches for matching rides. The following rules apply:<br/>
     * 1. Pickup and drop-off are within eligibility area of the ride (calculated on creation of the ride);
     * 2.1 lenient = false: The ride departs after <code>earliestDeparture</code> and arrives before <code>latestArrival</code>;
     * 2.2 lenient = true: The ride arrives after <code>earliestDeparture</code> and departs before <code>latestArrival</code>;
     * 3. The car has enough seats available
     * 4. The ride has not been deleted;
     * 5. The passenger and driver should travel in more or less the same direction. 
     * 6. The ride has less than <code>maxBookings</code> active bookings.
     * 7. earliestDeparture is before latestArrival.  
     * @param fromPlace The location for pickup
     * @param toPlace The location for drop-off
     * @param maxBearingDifference The maximum difference in bearing direction between driver and passenger vectors.
     * @param earliestDeparture The date and time to depart earliest
     * @param latestArrival The date and time to arrive latest 
     * @param nrSeatsRequested the number of seats required
     * @param lenient if true then also retrieve rides that partly overlap the passenger's travel window. Otherwise it must be fully inside the passenger's travel window. 
     * @param maxResults pagination: maximum number of results
     * @param offset pagination: The offset to start (start at 0)
     * @param graphName the graph name of the entity graph to use.
     * @return A list of potential matches.
     */

    public PagedResult<Ride> search(@NotNull GeoLocation fromPlace, @NotNull GeoLocation toPlace, Instant earliestDeparture, 
    		Instant latestArrival, Integer nrSeats, boolean lenient, Integer maxResults, Integer offset) throws BadRequestException {
    	if (earliestDeparture != null && latestArrival != null && earliestDeparture.isAfter(latestArrival)) {
    		throw new BadRequestException("Departure time must be before arrival time");
    	}
    	if (nrSeats == null) {
    		nrSeats = 1;
    	}
    	if (maxResults == null) {
    		maxResults = MAX_RESULTS;
    	}
    	if (offset == null) {
    		offset = 0;
    	}
    	List<Ride> results = Collections.emptyList();
        PagedResult<Long> prs = rideDao.search(fromPlace, toPlace, MAX_BEARING_DIFFERENCE, earliestDeparture, latestArrival, nrSeats, lenient, MAX_BOOKINGS, 0, 0);
        Long totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		PagedResult<Long> rideIds = rideDao.search(fromPlace, toPlace, MAX_BEARING_DIFFERENCE, earliestDeparture, latestArrival, nrSeats, lenient, MAX_BOOKINGS, maxResults, offset);
        	if (! rideIds.getData().isEmpty()) {
        		results = rideDao.fetch(rideIds.getData(), Ride.SEARCH_RIDES_ENTITY_GRAPH);
        	}
    	}
    	return new PagedResult<Ride>(results, maxResults, offset, totalCount);
    }

    /**
     * Verify the input from the client.
     * @param ride
     * @throws CreateException
     */
    private void validateCreateUpdateRide(Ride ride)  throws BadRequestException {
//    	if (ride.getRideTemplate() == null) {
//    		throw new CreateException("Constraint violation: A ride must have a template");
//    	}
    	if (ride.getDepartureTime() == null && ride.getArrivalTime() == null) {
    		throw new BadRequestException("Constraint violation: A ride must have a 'departureTime' and/or an 'arrivalTime'");
    	}
    	if (ride.getFrom() == null || ride.getTo() == null) {
    		throw new BadRequestException("Constraint violation: A new ride must have a 'from' and a 'to'");
    	}
    	if (ride.getBookings() != null && !ride.getBookings().isEmpty()) {
    		throw new BadRequestException("Constraint violation: A new ride cannot contain bookings");
    	}
    	if (ride.getCarRef() == null) {
    		throw new BadRequestException("Constraint violation: A ride must have a car defined");
    	}
    	if (ride.getMaxDetourMeters() != null && ride.getMaxDetourMeters() <= 0) {
    		throw new BadRequestException("Constraint violation: The maximum detour in meters must be greater than 0");
    	}
    	if (ride.getMaxDetourSeconds() != null && ride.getMaxDetourSeconds() <= 0) {
    		throw new BadRequestException("Constraint violation: The maximum detour in seconds must be greater than 0");
    	}
    }

    /**
     * Creates a ride. In case recurrence is set, all following rides are created as well, up to 8 weeks in advance.
     * A ride has a template only for recurrent rides. The driver of driver reference must be set. 
     * The car must exist and be owned by the driver.
     * @param ride The input from the application.  
     * @return The ID of the ride just created.
     * @throws CreateException In case of trouble like wrong parameter values.
     * @throws NotFoundException If the car is not found.
     */
    public Long createRide(Ride ride) throws CreateException, NotFoundException, BadRequestException {
    	Car car = carDao.find(RideshareUrnHelper.getId(Car.URN_PREFIX, ride.getCarRef()))
    			.orElseThrow(() -> new CreateException("Cannot find car: " + ride.getCarRef()));
    	User driverdb = ride.getDriver();
    	if (driverdb == null) {
        	driverdb = identityHelper.resolveUrn(ride.getDriverRef())
        			.orElseThrow(() -> new CreateException("Cannot find driver: " + ride.getDriverRef()));
        	ride.setDriver(driverdb);
    	}
    	if (! car.isOwnedBy(driverdb)) {
    		throw new SecurityException(String.format("Car %s is not owned by %s", car.getLicensePlate(), driverdb.toString()));
    	}
    	ride.setCar(car);
    	validateCreateUpdateRide(ride);
//    	if (! RideshareUrnHelper.getId(User.URN_PREFIX, car.getDriverRef()).equals(ride.getDriver().getId())) {
//    		throw new CreateException("Constraint violation: The car is not owned by the owner of the ride.");
//    	}
    	// If the ride contains no departure then, then the arrival time is important
    	// Assure both departure and arrival time are set, to avoid database constraint failure. 
    	if (ride.getDepartureTime() == null || ride.isArrivalTimePinned()) {
    		ride.setArrivalTimePinned(true);
    		ride.setDepartureTime(ride.getArrivalTime());
    	} else {
    		ride.setArrivalTime(ride.getDepartureTime());
    	}
    	ride.updateShareEligibility();
    	// Put the ride into the persistence context, but omit the template for now
    	RideTemplate template = ride.getRideTemplate();
    	ride.setRideTemplate(null);
    	ride.setState(RideState.SCHEDULED);
    	rideDao.save(ride);
    	// Update the car itinerary from the route planner
    	rideItineraryHelper.updateRideItinerary(ride);
    	// At this point the ride is completely defined. 
    	if (ride.getRideTemplate() != null && ride.getRideTemplate().getRecurrence() == null) {
    		log.warn("Inconsistence detected: Template defined without recurrency");
    		ride.setRideTemplate(null);
    	}
    	rideCreatedEvent.fire(ride);
    	Ride firstRide = ride;
    	if (template != null) {
    		// Create a template from the well-defined ride
    		// Copy the (new) ride to the template
    		RideBase.copy(ride, template);
    		// Copy the geometry obtained from the planner to the template
    		// Note: The strategy is currently to calculate the route of a recurrent ride only once (at creation or update).
    		//       Alternative is to make the route time-dependent and to calculate the route for each generated ride. 
    		//       In the latter case the legGeometry does not need to be on the template.
    		template.setLegGeometry(ride.getLegs().get(0).getLegGeometry());
    		rideTemplateDao.save(template);
    		ride.setRideTemplate(template);
			Instant systemHorizon = getDefaultSystemHorizon();
			// Create rides including the initial simple leg structure.
//			List<Ride> rides = generateNonOverlappingRides(template, systemHorizon);
			List<Ride> rides = template.generateRides(systemHorizon);

			// At this point is the question: Is the first generated ride the same as the one we already have?
			// The client could issue an invalid first date.
			if (rides.size() == 0) {
				// This is unexpected, no rides at all. The horizon must be too short. Error.
				throw new CreateException("No rides could be created from template setting. Is horizon too close?");
			} else {
				Ride firstGeneratedRide = rides.get(0);
				if (firstGeneratedRide.hasTemporalOverlap(ride)) {
					// The ride that was calculated has overlap with the first template-generated ride. 
					// That is ok, skip the first, we have it already in the database
					rides.remove(0);
				} else {
					// The one we calculated must have (initial) that does not match the recurrence pattern.
					// Remove it from the database
					rideDao.remove(ride);
					firstRide = firstGeneratedRide;
				}
				// No rides in the list are in the persistence context 
				rides.forEach(r -> rideItineraryHelper.saveNewRide(r));
			}
    	}
    	return firstRide.getId();
    }

    /**
     * Retrieves a ride. Anyone can read a ride, given the id. All details are retrieved.
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Ride getRide(Long id) throws NotFoundException {
    	Ride ridedb = rideDao.find(id, rideDao.createLoadHint(Ride.DETAILS_WITH_LEGS_ENTITY_GRAPH))
    			.orElseThrow(() -> new NotFoundException("No such ride"));
    	// Because Hibernate cannot fetch multiple bags in one step, we retrieve the bookings separately
    	// Detach ride to avoid propagation of changes
    	rideDao.detach(ridedb);
    	ridedb.setBookings(bookingDao.findByRide(ridedb, BookingDao.JPA_HINT_FETCH, bookingDao.getEntityGraph(Booking.SHALLOW_ENTITY_GRAPH)));
    	return ridedb;
    }

    /**
     * Updates an existing ride. If a booking is involved, it might change. The booking details themselves cannot be changed trough this call. 
     * It is not possible to change the driver.
     * @param ride The ride with the ID already provided
     * @param scope The extent of the update in case of a recurrent ride. Default RideScope.THIS.  
     * @throws CreateException
     * @throws NotFoundException
     */
    //FIXME
    public void updateRide(Ride ride, RideScope scope) throws UpdateException, NotFoundException, BadRequestException {
    	Ride ridedb = rideDao.find(ride.getId())
    			.orElseThrow(NotFoundException::new);
    	ride.setDriver(ridedb.getDriver());	// It is not allowed to change driver
//    	if (ridedb.getBookings().stream().filter(b -> ! b.isDeleted()).collect(Collectors.counting()) > 0) {
//    		// What if there is already a booking
//    		throw new CreateException("The ride has already bookings, an update is not allowed");
//    	}
    	validateCreateUpdateRide(ride);
    	Long carId = RideshareUrnHelper.getId(Car.URN_PREFIX, ride.getCarRef());
    	if (carId == null) {
    		new NotFoundException("No Car ID found for Ride " + ride.getId());
    	}
    	Car car = carDao.find(carId)
    			.orElseThrow(() -> new NotFoundException("Cannot find car: " + carId));
    	if (! carDao.isDrivenBy(car, ridedb.getDriver())) {
    		throw new BadRequestException("Constraint violation: The car is not owned by the owner of the ride.");
    	}
    	Recurrence newRecurrence = ride.getRideTemplate() != null ? ride.getRideTemplate().getRecurrence() : null;  
    	// Set all relations
    	ride.setCar(ridedb.getCar());
    	ride.setRideTemplate(ridedb.getRideTemplate());
    	// If the ride contains no departure then, then the arrival time is important
    	// Assure both departure and arrival time are set, to avoid database constraint failure. 
    	if (ride.getDepartureTime() == null) {
    		ride.setArrivalTimePinned(true);
    		ride.setDepartureTime(ride.getArrivalTime());
    	} else {
    		ride.setArrivalTime(ride.getDepartureTime());
    	}
    	ride.updateShareEligibility();
    	ridedb = rideDao.merge(ride);
    	// ride and ridedb refer to the same object now
    	rideItineraryHelper.updateRideItinerary(ridedb);

    	// Now comes the difficult part with the recurrence
    	if (ridedb.getRideTemplate() == null && newRecurrence == null) {
        	// 1. No recurrence in DB nor update -> no template
    		// Done.
    	} else if (ridedb.getRideTemplate() == null && newRecurrence != null) {
    		// 2. Recurrence in update only --> create template and generate rides as with a new ride
    		throw new UnsupportedOperationException("Update with recurrence is not yet supported");
    	} else if (ridedb.getRideTemplate() != null && newRecurrence == null) {
	    	// 3. Recurrence in DB only. 
	    	// 3.1. THIS Remove template for this ride. Ride is no longer recurrent.
	    	// 3.2. THIS_AND_FOLLOWING Set horizon at current ride, remove future rides, save template. Remove template from ride.
    		//		Keep booked rides though
    		throw new UnsupportedOperationException("Update with removal of recurrence is not yet supported");
    	} else {
	    	// 4. Recurrence in DB and update
    		if (newRecurrence.equals(ridedb.getRideTemplate().getRecurrence())) {
    	    	// 4.1 Recurrence is same
    	    	// 4.1.1. THIS Create a new template for this ride, generate rides
    	    	// 4.1.2. THIS_AND_FOLLOWING Set horizon at current template, remove all future rides and generate rides
        		//  	  Keep booked rides though
        		throw new UnsupportedOperationException("Update of recurrent rides is not yet supported");
    		} else {
    	    	// 4.2. Recurrence has changed - Remove all future unbooked rides
    	    	// 4.2.1. THIS Create a new template for this ride, generate rides
    	    	// 4.2.2. THIS_AND_FOLLOWING Set horizon at current template, remove all future rides (or reuse them) and generate rides
        		//		  Keep booked rides though
        		throw new UnsupportedOperationException("Update with modified recurrence is not yet supported");
    		}
    	}
    }
    
    private void removeRide(Long rideId, final String reason) {
		try {
			Ride ridedb = rideDao.find(rideId)
					.orElseThrow(NotFoundException::new);
			removeRide(ridedb, reason);
		} catch (NotFoundException e) {
			log.warn(String.format("Ride %d not found, ignoring...", rideId));
		}
    }

    private void removeRide(Ride ridedb, final String reason) {
    	cancelRideTimers(ridedb);
		ridedb.setMonitored(false);
    	if (ridedb.getBookings().size() > 0) {
    		// Perform a soft delete
    		ridedb.setDeleted(true);
    		updateRideState(ridedb, RideState.CANCELLED);
    		ridedb.setCancelReason(reason);
    		// Allow other parties such as the booking manager to do their job too
    		rideRemovedEvent.fire(ridedb);
		} else {
			rideDao.remove(ridedb);
		}
    }

    /**
     * Removes a ride. If the ride is booked it is soft-deleted. 
     * If a ride is recurring and the scope is set to <code>this-and-following</code> 
     * then all following rides are removed as well. The <code>horizon</code> date of the
     * preceding rides of the same template, if any, is set to the day of the departure 
     * date of the ride being deleted. 
     * If the ride is already deleted the  
     * @param rideId The ride to remove.
     * @param reason The reason why it was cancelled (optional).
     * @param scope The extent of deletion in case of a recurrent ride. Default RideScope.THIS.  
     * @throws NotFoundException In case the rideId is not found
     */
    public void removeRide(Long rideId, final String reason, RideScope scope) throws NotFoundException, SoftRemovedException {
    	Ride ridedb = rideDao.find(rideId)
    			.orElseThrow(NotFoundException::new);
    	if (ridedb.isDeleted()) {
    		throw new SoftRemovedException();
    	}
    	removeRide(ridedb, reason);
    	if (ridedb.getRideTemplate() != null) {
    		// Recurrent ride
    		if (scope == RideScope.THIS_AND_FOLLOWING) {
	    		// Deletes this ride and all that follow
	    		rideDao.findFollowingRideIds(ridedb.getRideTemplate(), ridedb.getDepartureTime())
	    			.forEach(rid -> removeRide(rid, reason));
	    		// Set the horizon of the template to the departure date of this ride.
	    		ridedb.getRideTemplate().getRecurrence().setHorizon(ridedb.getDepartureTime());
    		}
	    	// Check how many rides are attached to the template. If 0 then delete the template too.
	    	if (rideTemplateDao.getNrRidesAttached(ridedb.getRideTemplate()) == 0L) {
	    		rideTemplateDao.remove(ridedb.getRideTemplate());
	    	}
    	}
    }

    public void onStaleItinerary(@Observes(during = TransactionPhase.IN_PROGRESS) @Updated Ride ride) {
    	try {
    		if (ride.isDeleted()) {
    			log.debug("Ride is already deleted, ignoring update itinerary request: " + ride.getRideRef());
    		} else {
    			rideItineraryHelper.updateRideItinerary(ride);
    		}
		} catch (BadRequestException e ) {
			log.error("Error updating itinerary: " + e.toString());
			context.setRollbackOnly();
		} catch (Exception e) {
			context.setRollbackOnly();
			log.error("Error updating itinerary: ", e);
		}
    }

    /**
     * Sets the confirmation flag on the ride and sends a event to inform the provider has confirmed the ride.
     * @param rideId the ride to update.
     * @throws NotFoundException If the ride was not found.
     */
    public void confirmRide(Long rideId, Boolean confirmationValue) throws NotFoundException, BadRequestException {
    	Ride ridedb = rideDao.find(rideId)
    			.orElseThrow(() -> new NotFoundException("No such ride: " + rideId));
    	if (confirmationValue == null) {
    		throw new BadRequestException("An empty confirmation value is not allowed");
    	}
    	if (ridedb.getConfirmed() != null) {
    		throw new BadRequestException("Ride has already a confirmation value: " + rideId);
    	}
    	ridedb.setConfirmed(confirmationValue);
    	ridedb.getActiveBooking()
    		.ifPresent(b -> transportProviderConfirmedEvent.fire(new TripConfirmedByProviderEvent(b.getBookingRef(),  b.getPassengerTripRef(), confirmationValue)));
    }

    public static class RideInfo implements Serializable {
		private static final long serialVersionUID = -2715209888482006490L;
		public RideMonitorEvent event;
    	public Long rideId;
    	public RideInfo(RideMonitorEvent anEvent, Long aRideId) {
    		this.event = anEvent;
    		this.rideId = aRideId;
    	}
    	
		@Override
		public String toString() {
			return String.format("RideInfo [%s %s]", event, rideId);
		}
    }
    
    protected void updateRideState(Ride ride, RideState newState) {
    	RideState previousState = ride.getState();
		ride.setState(newState);
    	log.debug(String.format("updateRideState %s: %s --> %s", ride.toStringCompact(), previousState, ride.getState()));
   		rideStateUpdatedEvent.fire(new RideStateUpdatedEvent(previousState, ride));
    }

	@Schedule(info = "Collect due rides", hour = "*/1", minute = "0", second = "0", persistent = false /* non-critical job */)
	public void checkForDueRides() {
//		log.debug("CollectDueRides");
		// Get all rides that have a departure time within a certain window (and not monitored)
		List<Ride> rides = rideDao.findMonitorableRides(Instant.now().plus(Duration.ofHours(2).plus(DEPARTING_PERIOD)));
		for (Ride ride : rides) {
			startMonitoring(ride);
		}
	}

	@Timeout
	public void onTimeout(Timer timer) {
		if (! (timer.getInfo() instanceof RideInfo)) {
			log.error("Don't know how to handle timeout: " + timer.getInfo());
			return;
		}
		RideInfo rideInfo = (RideInfo) timer.getInfo();
		if (log.isDebugEnabled()) {
			log.debug("Received ride event: " + rideInfo.toString());
		}
		Ride ride = rideDao.fetchGraph(rideInfo.rideId, Ride.DETAILS_WITH_LEGS_ENTITY_GRAPH)
				.orElseThrow(() -> new IllegalArgumentException("No such ride: " + rideInfo.rideId));
		Instant now = Instant.now();
		switch (rideInfo.event) {
		case TIME_TO_PREPARE:
			updateRideState(ride, RideState.DEPARTING);
			timerService.createTimer(Date.from(ride.getDepartureTime()), 
					new RideInfo(RideMonitorEvent.TIME_TO_DEPART, ride.getId()));
			break;
		case TIME_TO_DEPART:
			updateRideState(ride, RideState.IN_TRANSIT);
			timerService.createTimer(Date.from(ride.getArrivalTime()), 
					new RideInfo(RideMonitorEvent.TIME_TO_ARRIVE, ride.getId()));
			break;
		case TIME_TO_ARRIVE:
			updateRideState(ride, RideState.ARRIVING);
			if (ride.hasActiveBooking() && ride.getArrivalTime().plus(CONFIRMATION_PERIOD).isAfter(now)) {
				timerService.createTimer(Date.from(ride.getArrivalTime().plus(CONFIRMATION_DELAY)), 
						new RideInfo(RideMonitorEvent.TIME_TO_VALIDATE, ride.getId()));
			} else {
				timerService.createTimer(Date.from(ride.getArrivalTime().plus(ARRIVING_PERIOD)), 
						new RideInfo(RideMonitorEvent.TIME_TO_COMPLETE, ride.getId()));
			}
			break;
		case TIME_TO_VALIDATE:
			updateRideState(ride, RideState.VALIDATING);
			timerService.createTimer(Date.from(ride.getArrivalTime().plus(CONFIRM_PERIOD_1)), 
					new RideInfo(RideMonitorEvent.TIME_TO_CONFIRM_REMINDER, ride.getId()));
			break;
		case TIME_TO_CONFIRM_REMINDER:
			updateRideState(ride, RideState.VALIDATING);
			timerService.createTimer(Date.from(ride.getArrivalTime().plus(CONFIRM_PERIOD_2)), 
					new RideInfo(RideMonitorEvent.TIME_TO_COMPLETE, ride.getId()));
			break;
		case TIME_TO_COMPLETE:
			updateRideState(ride, RideState.COMPLETED);
			ride.setMonitored(false);
			break;
		default:
			log.warn("Don't know how to handle event: " + rideInfo.event);
			break;
		}
	}

	protected void startMonitoring(Ride ride) {
		if (ride.getState() == RideState.CANCELLED) {
			log.warn("Cannot monitor, ride has been canceled: " + ride.getId());
			return;
		}
		if (ride.isMonitored()) {
			log.warn("Ride already monitored: " + ride.getId());
			return;
		}
		ride.setMonitored(true);
		// Should we always generate timer events and let the state machine decide what to do?
		// Tested. Result: Timer events are received in random order.
		// Workaround: Set the next timer in each event handler
		timerService.createTimer(Date.from(ride.getDepartureTime().minus(DEPARTING_PERIOD)), 
				new RideInfo(RideMonitorEvent.TIME_TO_PREPARE, ride.getId()));
	}

    public void onRideCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Created Ride ride) {
    	Ride ridedb = ride;
    	if (! rideDao.contains(ride)) {
			ridedb = rideDao.find(ride.getId())
					.orElseThrow(() -> new IllegalArgumentException("No such ride: " + ride.getId()));
    	}
    	if (ridedb.getDepartureTime().minus(DEPARTING_PERIOD.plus(Duration.ofHours(2))).isAfter(Instant.now())) {
    		startMonitoring(ridedb);
    	}
    	// Otherwise leave to the scheduled retrieval of rides
    }	

    protected void cancelRideTimers(Ride ride) {
    	// Find all timers related to this ride and cancel them
    	Collection<Timer> timers = timerService.getTimers();
		for (Timer timer : timers) {
			if ((timer.getInfo() instanceof RideInfo)) {
				RideInfo rideInfo = (RideInfo) timer.getInfo();
				if (rideInfo.rideId.equals(ride.getId())) {
					try {
						timer.cancel();
					} catch (Exception ex) {
						log.error("Unable to cancel timer: " + ex.toString());
					}
				}
			}
		}
    }
}
