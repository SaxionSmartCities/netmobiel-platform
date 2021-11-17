package eu.netmobiel.rideshare.service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.NoSuchObjectLocalException;
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

import org.hibernate.annotations.QueryHints;
import org.slf4j.Logger;

import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.annotation.Updated;
import eu.netmobiel.commons.event.TripConfirmedByProviderEvent;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.RemoveException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.ConfirmationReasonType;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.here.search.HereSearchClient;
import eu.netmobiel.rideshare.event.BookingFareSettledEvent;
import eu.netmobiel.rideshare.event.RideStateUpdatedEvent;
import eu.netmobiel.rideshare.filter.RideFilter;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideBase;
import eu.netmobiel.rideshare.model.RideMonitorEvent;
import eu.netmobiel.rideshare.model.RideScope;
import eu.netmobiel.rideshare.model.RideState;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.CarDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.RideTemplateDao;
import eu.netmobiel.rideshare.repository.RideshareUserDao;
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
    private RideshareUserDao userDao;
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
    @Inject
    private HereSearchClient hereSearchClient;

    @Resource
    private SessionContext context;

    @Inject @Removed
    private Event<Ride> rideRemovedEvent;

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
		// Get the ride instances that overlap or are beyond the template ride, that are rides with the same driver
		List<Ride> ridesBeyondTemplate  = rideDao.findRidesSameDriverBeyond(template.getDepartureTime());
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
	 * @throws BusinessException 
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public int instantiateRecurrentRides(RideTemplate template, Instant systemHorizon) throws BusinessException {
		// Load template into persistence context
		template = rideTemplateDao.merge(template);
		List<Ride> rides = generateNonOverlappingRides(template, systemHorizon);
		for (Ride ride : rides) {
			rideItineraryHelper.saveNewRide(ride);
			checkRideMonitoring(ride);
		}
		return rides.size();
	}

    protected void completeTheFilter(RideFilter filter) throws BadRequestException, NotFoundException {
    	filter.validate();
    	if (filter.getDriver() == null && filter.getDriverId() != null) {
    		filter.setDriver(userDao.find(filter.getDriverId())
        			.orElseThrow(() -> new NotFoundException("No such user: " + filter.getDriverId())));
    	}
    }
    	
    /**
     * List all rides owned by the calling user (driverId is null) or owned by the specified user. Soft deleted rides are omitted.
     * @return A list of rides owned by the calling user.
     * @throws BadRequestException 
     */
    public PagedResult<Ride> listRides(RideFilter filter, Cursor cursor ) throws NotFoundException, BadRequestException {
    	completeTheFilter(filter);
    	cursor.validate(MAX_RESULTS, 0);
    	// Assure user is in persistence context
    	if (userDao.find(filter.getDriver().getId()).isEmpty()) {
			throw new NotFoundException("No such user: " + filter.getDriver().getId());
    	}
    	List<Ride> results = Collections.emptyList();
        Long totalCount = 0L;
		PagedResult<Long> prs = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && ! cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> rideIds = rideDao.findByDriver(filter, cursor);
    		if (rideIds.getData().size() > 0) {
    			results = rideDao.loadGraphs(rideIds.getData(), Ride.LIST_RIDES_ENTITY_GRAPH, Ride::getId);
    		}
    	}
    	return new PagedResult<>(results, cursor, totalCount);
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
        		results = rideDao.loadGraphs(rideIds.getData(), Ride.SEARCH_RIDES_ENTITY_GRAPH, Ride::getId);
        	}
    	}
    	return new PagedResult<>(results, maxResults, offset, totalCount);
    }

    /**
     * Verify the input from the client.
     * @param ride
     * @throws CreateException
     */
    private static void validateCreateUpdateRide(Ride ride)  throws BadRequestException {
//    	if (ride.getRideTemplate() == null) {
//    		throw new CreateException("Constraint violation: A ride must have a template");
//    	}
    	if (ride.getDepartureTime() == null && ride.getArrivalTime() == null) {
    		throw new BadRequestException("Constraint violation: A ride must have a 'departureTime' and/or an 'arrivalTime'");
    	}
    	if (ride.getArrivalTime() == null && ride.isArrivalTimePinned()) {
    		throw new BadRequestException("Constraint violation: 'arrivalTime' is pinned, but 'arrivalTime' is missing");
    	}
    	if (ride.getFrom() == null || ride.getTo() == null) {
    		throw new BadRequestException("Constraint violation: A ride must have a 'from' and a 'to'");
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
     * @throws BusinessException 
     */
    public Long createRide(Ride ride) throws BusinessException {
    	Car car = carDao.find(UrnHelper.getId(Car.URN_PREFIX, ride.getCarRef()))
    			.orElseThrow(() -> new CreateException("Cannot find car: " + ride.getCarRef()));
    	RideshareUser driverdb = ride.getDriver();
    	if (driverdb == null) {
        	driverdb = identityHelper.resolveUrn(ride.getDriverRef())
        			.orElseThrow(() -> new CreateException("Cannot find driver: " + ride.getDriverRef()));
        	ride.setDriver(driverdb);
    	}
    	if (! car.isOwnedBy(driverdb)) {
    		throw new SecurityException(String.format("Car %s is not owned by %s", car.getLicensePlate(), driverdb.toString()));
    	}
    	ride.setCar(car);
    	if (car.getNrSeats() != null) {
    		// There are never more seats available than specified by the manufacturer minus one seat for the driver. 
    		ride.setNrSeatsAvailable(Math.min(ride.getNrSeatsAvailable(), car.getNrSeats() - 1));
    	}
    	if (ride.getMaxDetourMeters() == null) {
    		ride.setMaxDetourMeters(RideBase.DEFAULT_MAX_DISTANCE_DETOUR_METERS);
    	}
    	validateCreateUpdateRide(ride);
    	if (ride.getBookings() != null && !ride.getBookings().isEmpty()) {
    		throw new BadRequestException("Constraint violation: A new ride cannot contain bookings");
    	}
    	if (ride.getRideTemplate() != null && ride.getRideTemplate().getRecurrence() == null) {
    		log.warn("Inconsistency detected: Template defined without recurrency");
    		ride.setRideTemplate(null);
    	}
    	// If the ride contains no departure then, then the arrival time is important
    	RideTemplate template = ride.getRideTemplate();
    	Instant travelTime = null; 
    	if (ride.getDepartureTime() == null || ride.isArrivalTimePinned()) {
    		travelTime = ride.getArrivalTime();
			ride.setArrivalTimePinned(true);
    	} else {
    		travelTime = ride.getDepartureTime();
    	}
		if (template != null) {
			// Snap the specified time to the first possible time matching the recurrence pattern
			travelTime = template.snapTravelTimeToPattern(travelTime);
		}
    	// Assure both departure and arrival time are set, to avoid database constraint failure.
    	// Temporarily make departure and arrival time the same, this will be adapted once the itinerary is known. 
		ride.setDepartureTime(travelTime);
		ride.setArrivalTime(travelTime);
		// Calculate the ellipse
    	ride.updateShareEligibility();
    	ride.setDeparturePostalCode(hereSearchClient.getPostalCode6(ride.getFrom()));
    	ride.setArrivalPostalCode(hereSearchClient.getPostalCode6(ride.getTo()));
    	// Put the ride into the persistence context, but omit the template for now
    	ride.setRideTemplate(null);
    	ride.setState(RideState.SCHEDULED);
    	rideDao.save(ride);
    	// Update the car itinerary from the route planner
    	rideItineraryHelper.updateRideItinerary(ride);
    	// Now we know the both departure time and arrival time. Is there any overlap?
    	// At this point the ride is completely defined. 
    	if (rideDao.existsTemporalOverlap(ride)) {
    		throw new CreateException("Ride overlaps existing ride");
    	}
    	if (template != null) {
    		// Create the current rides
    		attachTemplateAndInstantiateRides(ride, template);
    	}
    	checkRideMonitoring(ride);
    	return ride.getId();
    }

    protected void attachTemplateAndInstantiateRides(Ride ride, RideTemplate template) throws BusinessException {
		// Create a template from the well-defined ride
		// Copy the (new) ride to the template
		RideBase.copy(ride, template);
		// Perhaps an old persistent template was passed. Remove the id to make it a fresh new one.
		template.setId(null);
		// Copy the geometry obtained from the planner to the template
		// Note: The strategy is currently to calculate the route of a recurrent ride only once (at creation or update).
		//       Alternative is to make the route time-dependent and to calculate the route for each generated ride. 
		//       In the latter case the legGeometry does not need to be on the template.
		template.setLegGeometry(ride.getLegs().get(0).getLegGeometry());
		rideTemplateDao.save(template);
		ride.setRideTemplate(template);
		Instant systemHorizon = getDefaultSystemHorizon();
		// Create rides including the initial simple leg structure.
		// Decision (2021-01-12 Timothy, Jaap): No check on overlapping rides in the sequence, it is too cumbersome.
		// Note that the periodic instantiation of recurrent rides does have the overlap check
//			List<Ride> rides = generateNonOverlappingRides(template, systemHorizon);
		List<Ride> rides = template.generateRides(systemHorizon);

		// At this point is the question: Is the first generated ride the same as the one we already have?
		// The client could issue an invalid first date.
		if (rides.size() == 0) {
			// This is unexpected, no rides at all. The horizon must be too short. Error.
			throw new CreateException("No rides could be created from template setting. Is horizon too close?");
		}
		if (rides.get(0).hasTemporalOverlap(ride)) {
			// The ride that was calculated should have overlap with the first template-generated ride. 
			// That is ok, we snapped it already to the pattern. Skip the first, we have it already in the database.
			rides.remove(0);
		} else {
			// The one we calculated must have (initial) that does not match the recurrence pattern.
			throw new IllegalStateException("First recurrent ride does not match!");
		}
		// None of the rides in the list are in the persistence context
		for (Ride r : rides) {
			rideItineraryHelper.saveNewRide(r);
			checkRideMonitoring(r);
		}
    }
    
    /**
     * Set horizon at current ride, remove future rides, save template. Remove template from ride. 
     * Keep booked rides though
     * @param ride the ride to modify the recurrence of. 
     * @throws BusinessException
     */
    protected void detachTemplateAndRemoveFutureRides(Ride ride, Instant departureTime) throws BusinessException {
    	// Get all future rides beyond this ride, i.e. all rides attached to the same template
    	RideTemplate template = ride.getRideTemplate();
    	List<Long> rideIds = rideDao.findFollowingRideIds(template, departureTime);
    	// Don't remove the reference ride, it could be in the list when moved forward. 
    	rideIds.remove(ride.getId());
    	
    	List<Ride> rides = rideDao.loadGraphs(rideIds, Ride.LIST_RIDES_ENTITY_GRAPH, Ride::getId);
    	for (Ride r : rides) {
			if (! r.hasActiveBooking()) {
				removeRide(r, "Template has changed", true);
			}
		}
    	if (! ride.hasActiveBooking()) {
    		ride.setRideTemplate(null);
    	}
		// At this point all future non-booked rides are removed
		// The current ride is detached from the template
		// If nobody is using the old template anymore, then it is removed.

    	limitTemplateHorizonUpToRide(template, ride);
		checkIfTemplateObsoleted(template); 
    }

    /**
     * Sets the horizon of the given template to exclude the specified ride, i.e., no rides are 
     * generated anymore beyond the specified ride, including that same ride. 
     * @param template the template to modify
     * @param rideToExclude the ride being the horizon of the template.
     */
    private void limitTemplateHorizonUpToRide(RideTemplate template, Ride rideToExclude) {
    	if (!rideTemplateDao.contains(template)) {
    		throw new IllegalStateException("Template should be in persistence context");
    	}
		template.getRecurrence().setHorizon(rideToExclude.getDepartureTime());
    }
    
    /**
     * Checks how many rides are attached to the template. If 0 then delete the template. 
     * @param template
     */
    private void checkIfTemplateObsoleted(RideTemplate template) {
		if (rideTemplateDao.getNrRidesAttached(template) == 0L) {
			rideTemplateDao.remove(template);
		}
    }
    
    /**
     * Retrieves a ride. Anyone can read a ride, given the id. All details are retrieved.
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Ride getRide(Long id) throws NotFoundException {
    	Ride ridedb = rideDao.loadGraph(id, Ride.DETAILS_WITH_LEGS_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such ride"));
    	// Because Hibernate cannot fetch multiple bags in one step, we retrieve the bookings separately
    	// Detach ride to avoid propagation of changes
    	rideDao.detach(ridedb);
    	ridedb.setBookings(bookingDao.findByRide(ridedb, QueryHints.LOADGRAPH, bookingDao.getEntityGraph(Booking.SHALLOW_ENTITY_GRAPH)));
    	return ridedb;
    }

    private void prepareUpdateOfRide(Ride ridedb, Ride ride, RideTemplate newTemplate) throws BusinessException {
    	if (!ridedb.getState().isPreTravelState()) {
    		throw new UpdateException("Ride can not be updated, travelling has already started!");
    	}
    	ride.setDriver(ridedb.getDriver());	// It is not allowed to change driver
		// What if there is already a booking
    	if (ridedb.hasConfirmedBooking()) {
    		throw new UpdateException("The ride has already a booking, an update is not allowed");
    	}
		// What if the booking process is active
    	if (ridedb.hasActiveBookingProcess()) {
    		throw new UpdateException("The ride is involved in a shout-out or a booking is requested, an update is not allowed now");
    	}
    	if (ride.getMaxDetourMeters() == null) {
    		ride.setMaxDetourMeters(RideBase.DEFAULT_MAX_DISTANCE_DETOUR_METERS);
    	}
    	
    	validateCreateUpdateRide(ride);
    	Long carId = UrnHelper.getId(Car.URN_PREFIX, ride.getCarRef());
    	if (carId == null) { 
    		throw new NotFoundException("No Car ID found for Ride " + ride.getId());
    	}
    	Car car = carDao.find(carId)
    			.orElseThrow(() -> new NotFoundException("Cannot find car: " + carId));
    	if (! carDao.isDrivenBy(car, ridedb.getDriver())) {
    		throw new BadRequestException("Constraint violation: The car is not owned by the owner of the ride.");
    	}
    	ride.setCar(car);

    	// Copy non-modifiable attributes to the input object
    	ride.setArrivalPostalCode(ridedb.getArrivalPostalCode());
    	ride.setCancelReason(ridedb.getCancelReason());
    	ride.setConfirmed(ridedb.getConfirmed());
    	ride.setConfirmationReason(ridedb.getConfirmationReason());
    	ride.setDeleted(ridedb.getDeleted());
    	ride.setDeparturePostalCode(ridedb.getDeparturePostalCode());
    	ride.setMonitored(ridedb.isMonitored());
    	ride.setState(ridedb.getState());
    	ride.setVersion(ridedb.getVersion());
    	// If the ride contains no departure then, then the arrival time is important
    	Instant travelTime = null; 
    	if (ride.getDepartureTime() == null || ride.isArrivalTimePinned()) {
    		travelTime = ride.getArrivalTime();
			ride.setArrivalTimePinned(true);
    	} else {
    		travelTime = ride.getDepartureTime();
    	}
		if (newTemplate != null) {
			// Snap the specified time to first possible time matching the recurrence pattern
			travelTime = newTemplate.snapTravelTimeToPattern(travelTime);
		}
    	// Assure both departure and arrival time are set, to avoid database constraint failure.
    	// Temporarily make departure and arrival time the same, this will be adapted once the itinerary is known. 
		ride.setDepartureTime(travelTime);
		ride.setArrivalTime(travelTime);
		// Calculate the ellipse
    	// Recalculate the ellipse for determining the rideshare eligibility
    	ride.updateShareEligibility();
    	if (!ride.getFrom().equals(ridedb.getFrom())) {
    		ride.setDeparturePostalCode(hereSearchClient.getPostalCode6(ride.getFrom()));
    	}
    	if (!ride.getTo().equals(ridedb.getTo())) {
    		ride.setArrivalPostalCode(hereSearchClient.getPostalCode6(ride.getTo()));
    	}
    	
    }
    /**
     * Updates an existing ride. It is not possible to change the driver.
     * A ride (recurrent or not) cannot be update with this call when it has a booking in REQUESTED or CONFIRMED state.
     * Booked ride that are part of een recurrent sequence are not updated, instead they will be dissociated from the ride template
     * and continue as independent rides. The driver has to make explicit decisions about booked rides.    
     * Booked rides should be alterable, but for now this is too complex. 
     * @param ride The ride with the ID already provided
     * @param scope The extent of the update in case of a recurrent ride. Default RideScope.THIS.  
     * @throws BusinessException 
     */
    //FIXME
    public void updateRide(Ride ride, RideScope scope) throws BusinessException {
    	/**
    	 * Attributes that are modifiable:
    	 * allowedLuggage	- Not used now. Could be altered without updating legs. Booked rides could conflict with luggage taken. 
    	 * carRef 			- A different car. Referenced in a passenger leg, i.e. not modifiable in a simple way once booked.
    	 * arrivalTime 		- In theory a shift of travel time. Not modifiable for booked legs.
    	 * departureTime 	- In theory a shift of travel time. Not modifiable for booked legs.
    	 * fromPlace		- Replanning required. Not modifiable for booked legs.
    	 * toPlace			- Replanning required. Not modifiable for booked legs.
    	 * maxDetourMeters	- Can be modified. Booked rides might not satisfy new criteria.
    	 * maxDetourSeconds	- Can be modified. Booked rides might not satisfy new criteria.
    	 * nrSeatsAvailable	- Can be modified. Booked rides might not satisfy new criteria.
    	 * recurrence		- Can be modified. Remove all future effectively non-booked  rides and optionally insert new ones.
    	 * remarks			- Can be modified. Currently not used by the front-end. 
    	 */
    	// Get the ride with booking info
    	Ride ridedb = rideDao.loadGraph(ride.getId(), Ride.UPDATE_DETAILS_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such ride: " + ride.getId()));
    	// Note: the input ride template consists of the recurrence only. 
    	// The input template is by definition not persistent. Use the template from the database.
    	RideTemplate newTemplate = ride.getRideTemplate();
    	RideTemplate oldTemplate = ridedb.getRideTemplate();
    	ride.setRideTemplate(oldTemplate);
    	Instant originalDepartureTime = ridedb.getDepartureTime();
    	prepareUpdateOfRide(ridedb, ride, newTemplate);
    	ridedb = rideDao.merge(ride);
    	if (ridedb.getCar().getNrSeats() != null) {
    		ridedb.setNrSeatsAvailable(Math.min(ridedb.getNrSeatsAvailable(), ridedb.getCar().getNrSeats() - 1));
    	}
    	// ride and ridedb refer to the same object now, 
    	// ridebd bookings, legs and stops appear to be empty now! In the database the object are still there.
    	rideItineraryHelper.updateRideItinerary(ridedb);
    	// At this point the ride is completely defined, except for the template.
    	// If there is recurrence, then the ride matches the first iteration.
    	
    	// Now comes the difficult part with the recurrence. There are 4 possibilities to consider, see below.
    	Recurrence newRecurrence = newTemplate != null ? newTemplate.getRecurrence() : null;  
    	if (oldTemplate == null && newRecurrence == null) {
        	// 1. No recurrence in DB nor update -> no template
    		// Done.
    	} else if (oldTemplate == null && newRecurrence != null) {
    		// 2. Recurrence in update only --> create template and generate rides as with a new ride
    		// scope is ignored, because not applicable
    		attachTemplateAndInstantiateRides(ridedb, newTemplate);
    	} else if (oldTemplate != null && newRecurrence == null) {
	    	// 3. Recurrence in DB only. Remove template and or rideas depending on ride scope. 
    		if (scope == RideScope.THIS_AND_FOLLOWING) {
    	    	// 3.1. THIS_AND_FOLLOWING Set horizon at current ride, remove future rides (starting at the original time), save template. 
    			//      Remove template from ride. Keep booked rides though
    			detachTemplateAndRemoveFutureRides(ridedb, originalDepartureTime);
    		} else {
    	    	// 3.2. THIS Remove template for this ride. Ride is no longer recurrent.
    			ridedb.setRideTemplate(null);
        		checkIfTemplateObsoleted(oldTemplate);
    		}
    	} else {
	    	// 4. Recurrence in DB and update. Ride scope this-and-following is implicit. 
    		// Always replace the template, whether or not the recurrrence is the same.
			detachTemplateAndRemoveFutureRides(ridedb, originalDepartureTime);
    		attachTemplateAndInstantiateRides(ridedb, newTemplate);
    	}
    	// Should we really prevent overlapping rides? Alternative is to let the user cleanup, 
    	// just like the normal use of a planner.
    	if (rideDao.existsTemporalOverlap(ride)) {
    		throw new UpdateException("Ride overlaps existing ride");
    	}
    	checkRideMonitoring(ridedb);
    }
    
    private void removeRideById(Long rideId, final String reason, boolean hard) {
		try {
			Ride ridedb = rideDao.find(rideId)
					.orElseThrow(() -> new NotFoundException("No such ride: " + rideId));
			removeRide(ridedb, reason, hard);
		} catch (BusinessException e) {
			log.warn(String.format("Ride %d not found or not removed, ignoring...", rideId));
		}
    }

    private void removeRide(Ride ridedb, final String reason, boolean hard) throws BusinessException {
    	if (ridedb.getState().isFinalState()) {
    		// Already completed or cancelled. 
    		if (hard) {
	    		// Remove the ride from the listing
	    		ridedb.setDeleted(true);
    		}
    	} else if (! ridedb.getState().isPreTravelState()) {
    		// travelling, validating
    		throw new RemoveException(String.format("Cannot cancel ride %s; state %s forbids", ridedb.getId(), ridedb.getState()));
    	} else {
        	cancelRideTimers(ridedb);
	    	if (ridedb.getBookings().size() > 0) {
	    		updateRideState(ridedb, RideState.CANCELLED);
	    		if (reason != null && !reason.isBlank()) {
	    			ridedb.setCancelReason(reason.trim());
	    		}
	    		// Allow other parties such as the booking manager to do their job too
	    		EventFireWrapper.fire(rideRemovedEvent, ridedb);
	    		if (hard) {
		    		// Remove the ride from the listing
		    		ridedb.setDeleted(true);
	    		}
			} else {
				rideDao.remove(ridedb);
			}
    	}
    }

    /**
     * Removes a ride. If the ride is booked it is soft-deleted. 
     * If a ride is recurring and the scope is set to <code>this-and-following</code> 
     * then all following rides are removed as well. The <code>horizon</code> date of the
     * preceding rides of the same template, if any, is set to the day of the departure 
     * date of the ride being deleted. 
     * @param rideId The ride to remove.
     * @param reason The reason why it was cancelled (optional).
     * @param scope The extent of deletion in case of a recurrent ride. Default RideScope.THIS.  
     * @param hard If set to true then remove the ride from the listing.
     * @throws BusinessException 
     */
    public void removeRide(Long rideId, final String reason, RideScope scope, boolean hard) throws BusinessException {
    	Ride ridedb = rideDao.find(rideId)
    			.orElseThrow(NotFoundException::new);
    	removeRide(ridedb, reason, hard);
    	if (ridedb.getRideTemplate() != null) {
    		// Recurrent ride
    		if (scope == RideScope.THIS_AND_FOLLOWING) {
	    		// Deletes this ride and all that follow
	    		rideDao.findFollowingRideIds(ridedb.getRideTemplate(), ridedb.getDepartureTime())
	    			.forEach(rid -> removeRideById(rid, reason, hard));
	        	limitTemplateHorizonUpToRide(ridedb.getRideTemplate(), ridedb);
    		}
    		checkIfTemplateObsoleted(ridedb.getRideTemplate());
    	}
    }

    public void onStaleItinerary(@Observes(during = TransactionPhase.IN_PROGRESS) @Updated Ride ride) throws BadRequestException {
		if (ride.isDeleted()) {
			log.debug("Ride is already deleted, ignoring update itinerary request: " + ride.getUrn());
		} else {
			rideItineraryHelper.updateRideItinerary(ride);
		}
    }

    /**
     * Sets the confirmation flag on the ride and sends a event to inform that the provider has confirmed the ride.
     * @param rideId the ride to update.
     * @throws BusinessException 
     */
    public void confirmRide(Long rideId, Boolean confirmationValue, ConfirmationReasonType reason) throws BusinessException {
    	Ride ridedb = rideDao.find(rideId)
    			.orElseThrow(() -> new NotFoundException("No such ride: " + rideId));
    	if (confirmationValue == null) {
    		throw new BadRequestException("An empty confirmation value is not allowed");
    	}
    	if (ridedb.getConfirmed() != null) {
    		throw new BadRequestException("Ride has already a confirmation value: " + rideId);
    	}
    	ridedb.setConfirmed(confirmationValue);
    	ridedb.setConfirmationReason(reason);
    	Optional<Booking> optBooking = ridedb.getConfirmedBooking();
    	if (optBooking.isPresent()) {
    		EventFireWrapper.fire(transportProviderConfirmedEvent, 
    				new TripConfirmedByProviderEvent(optBooking.get().getUrn(), 
    						optBooking.get().getPassengerTripRef(), 
    						confirmationValue, reason));
    	}
    }

    /**
     * Flags the ride as complete when in validating state.
     * FIXME This state machine is too sensitive to small changes in the process.
     * @param event the event from the booking manager.
     * @throws BusinessException
     */
    public void onBookingFareSettled(@Observes(during = TransactionPhase.IN_PROGRESS) BookingFareSettledEvent event) throws BusinessException {
    	Ride ride = event.getRide();
    	// Only handle when the ride is being validated, this is the moment where confirmation is requested.
    	// When the event is received in a different ride state, it is probably because the booking was cancelled and the fare released. 
    	if (ride.getState() == RideState.VALIDATING) {
    		// The fare was settled or cancelled, any way, the ride is done
			cancelRideTimers(ride);
			updateRideState(ride, RideState.COMPLETED);
    	}
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
    
    protected void updateRideState(Ride ride, RideState newState) throws BusinessException {
    	RideState previousState = ride.getState();
		ride.setState(newState);
    	log.debug(String.format("updateRideState %s: %s --> %s", ride.toStringCompact(), previousState, ride.getState()));
   		EventFireWrapper.fire(rideStateUpdatedEvent, new RideStateUpdatedEvent(previousState, ride));
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

	protected void handleRideEvent(RideInfo rideInfo) throws BusinessException {
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
			if (ride.hasConfirmedBooking() && ride.getArrivalTime().plus(CONFIRMATION_PERIOD).isAfter(now)) {
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

	@Timeout
	public void onTimeout(Timer timer) {
		try {
			if (! (timer.getInfo() instanceof RideInfo)) {
				log.error("Don't know how to handle timeout: " + timer.getInfo());
				return;
			}
			RideInfo rideInfo = (RideInfo) timer.getInfo();
			handleRideEvent(rideInfo);
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + context.getRollbackOnly()); 
		} catch(NoSuchObjectLocalException ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}

	protected void startMonitoring(Ride ride) {
		if (ride.isDeleted() ) {
			log.warn("Cannot monitor, ride has been deleted: " + ride.getId());
			return;
		}
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

    protected void checkRideMonitoring(Ride ride) {
   		Duration timeLeftToDeparture = Duration.between(Instant.now(), ride.getDepartureTime());
		if (log.isDebugEnabled()) {
			log.debug(String.format("Ride %s is scheduled, time left to departure is %s", ride.getId(), timeLeftToDeparture.toString()));
		}
    	if (! ride.isMonitored() && timeLeftToDeparture.compareTo(DEPARTING_PERIOD.plus(Duration.ofHours(2))) < 0) {
    		if (log.isDebugEnabled()) {
    			log.debug("Start monitoring ride " + ride.getId());
    		}
    		startMonitoring(ride);
    	}
    	// Otherwise leave to the scheduled retrieval of rides
    }	

    /**
     * Kills the timers for the specified Ride.  
     * @param ride the ride for which to cancel all timers.
     */
    protected void cancelRideTimers(Ride ride) {
    	// Find all timers related to this ride and cancel them
    	Collection<Timer> timers = timerService.getTimers();
		for (Timer timer : timers) {
			if ((timer.getInfo() instanceof RideInfo)) {
				RideInfo rideInfo = (RideInfo) timer.getInfo();
				if (rideInfo.rideId.equals(ride.getId())) {
					try {
						if (log.isDebugEnabled()) {
							log.debug("Cancel monitor timer for ride " + ride.getId());
						}
						timer.cancel();
					} catch (Exception ex) {
						log.error("Unable to cancel timer: " + ex.toString());
					}
				}
			}
		}
		ride.setMonitored(false);
    }
	public List<RideInfo> listAllTripMonitorTimers() {
    	// Find all timers related to the trip manager
    	Collection<Timer> timers = timerService.getTimers();
    	return timers.stream()
    			.filter(tm -> tm.getInfo() instanceof RideInfo)
    			.map(tm -> (RideInfo) tm.getInfo())
    			.collect(Collectors.toList());
	}
	
	/**
	 * Create a map to revive the monitor. Use the event that would cause the favourable transition.
	 * Note that only rides that are monitored are considered, e.g. SCHEDULED AND monitored = true. 
	 */
	private static Map<RideState, RideMonitorEvent> rideStateToMonitorRevivalEvent = Map.ofEntries(
			new AbstractMap.SimpleEntry<>(RideState.SCHEDULED, RideMonitorEvent.TIME_TO_PREPARE),
			new AbstractMap.SimpleEntry<>(RideState.DEPARTING, RideMonitorEvent.TIME_TO_PREPARE),
			new AbstractMap.SimpleEntry<>(RideState.IN_TRANSIT, RideMonitorEvent.TIME_TO_DEPART),
			new AbstractMap.SimpleEntry<>(RideState.ARRIVING, RideMonitorEvent.TIME_TO_ARRIVE),
			new AbstractMap.SimpleEntry<>(RideState.VALIDATING, RideMonitorEvent.TIME_TO_VALIDATE),
			new AbstractMap.SimpleEntry<>(RideState.COMPLETED, RideMonitorEvent.TIME_TO_COMPLETE)
		);

	/**
	 * Revive the ride monitors that have been crashed due due to some unrecoverable errors.
	 */
	public void reviveRideMonitors() {
		List<RideInfo> rideInfos = listAllTripMonitorTimers();
		if (rideInfos.isEmpty()) {
			log.info("NO active ride timers");
		} else {
			log.info("Active ride timers:\n" + String.join("\n\t", 
					rideInfos.stream()
					.map(ti -> ti.toString())
					.collect(Collectors.toList()))
			);
		}		

		Set<Long> timedTripIds = rideInfos.stream()
				.map(ti -> ti.rideId)
				.collect(Collectors.toSet());
		List<Ride> monitoredTrips = rideDao.findMonitoredRides();
		monitoredTrips.removeIf(t -> timedTripIds.contains(t.getId()));
		if (monitoredTrips.isEmpty()) {
			log.info("All required ride monitors are in place");
		} else {
			log.warn(String.format("There are %d rides without active monitoring, fixing now...", monitoredTrips.size()));
			for (Ride ride : monitoredTrips) {
				RideMonitorEvent event = rideStateToMonitorRevivalEvent.get(ride.getState());
				if (event == null) {
					log.warn(String.format("Ride %s state is %s, no suitable revival event found", ride.getId(), ride.getState()));
					// First check what is really needed before switching off the monitor 
					// ride.setMonitored(false);
			} else {
					RideInfo ti = new RideInfo(event, ride.getId());
					rideDao.detach(ride);
					try {
						handleRideEvent(ti);
					} catch (BusinessException ex) {
						log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
					} catch (Exception ex) {
						log.error(String.format("Error reviving ride monitor: %s", ex.toString()));
					}
				}
			}
		}
	}

	public Optional<GeoLocation> findNextMissingPostalCode() {
		Optional<Ride> r = rideDao.findFirstRideWithoutPostalCode();
		GeoLocation loc = null;
		if (r.isPresent()) {
			if (r.get().getDeparturePostalCode() == null) {
				loc = r.get().getFrom();
			} else {
				loc = r.get().getTo();
			}
		}
		return Optional.ofNullable(loc);
	}

	public int assignPostalCode(GeoLocation location, String postalCode) {
		int affectedRows = 0;
		// Now assign all rides with same departure location to this postal code
		affectedRows += rideDao.updateDeparturePostalCode(location, postalCode);
		// And assign all rides with same arrival location to same postal code
		affectedRows += rideDao.updateArrivalPostalCode(location, postalCode);
		// Do the same for the templates
		affectedRows += rideTemplateDao.updateDeparturePostalCode(location, postalCode);
		affectedRows += rideTemplateDao.updateArrivalPostalCode(location, postalCode);
		return affectedRows;
	}

}
