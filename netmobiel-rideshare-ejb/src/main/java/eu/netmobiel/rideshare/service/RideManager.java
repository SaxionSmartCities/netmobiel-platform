package eu.netmobiel.rideshare.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SoftRemovedException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.ClosenessFilter;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Leg;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideBase;
import eu.netmobiel.rideshare.model.RideScope;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.Stop;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.CarDao;
import eu.netmobiel.rideshare.repository.LegDao;
import eu.netmobiel.rideshare.repository.OpenTripPlannerDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.RideTemplateDao;
import eu.netmobiel.rideshare.repository.StopDao;
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
@LocalBean
public class RideManager {
	public static final Integer MAX_RESULTS = 10; 
	public static final String AGENCY_NAME = "NetMobiel Rideshare Service";
	public static final String AGENCY_ID = "NB:RS";
	
	/**
	 * The driver has a vector, the passenger has a vector. 
	 * The difference in direction is limited for a match. Otherwise the driver has to drive in the
	 * wrong direction. The total bearing matching angle is twice the difference 
	 * (driver bearing - diff, driver bearing + diff). 
	 */
	private static final int MAX_BEARING_DIFFERENCE = 60; 	/* degrees */
//	private static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";
	private static final int HORIZON_WEEKS = 8;
	private static final int MAX_BOOKING_LOCATION_SHIFT = 100;	// Maximum 100 meter deviation of original pickup/drop-off
	private static final int TEMPLATE_CURSOR_SIZE = 10;
	@Inject
    private Logger log;

    @Inject
    private UserDao userDao;
    @Inject
    private CarDao carDao;
    @Inject
    private LegDao legDao;
    @Inject
    private StopDao stopDao;
    @Inject
    private RideDao rideDao;
    @Inject
    private RideTemplateDao rideTemplateDao;
    @Inject
    private OpenTripPlannerDao otpDao;
    
//    @Inject
//    private UserManager userManager;

    @Resource
    private SessionContext context;

    /**
     * Updates all recurrent rides by moving the system horizon to the next day.
     * The state of the ride generation is saved in each template. Updating the tempalte and saving the generated rides
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
			log.info(String.format("DB maintenance: Move horizon to %s, check starts at %s", DateTimeFormatter.ISO_INSTANT.format(systemHorizon)));
			do {
				List<RideTemplate> templates = rideTemplateDao.findOpenTemplates(systemHorizon, offset, TEMPLATE_CURSOR_SIZE);
				count = templates.size();
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
			} while (count < TEMPLATE_CURSOR_SIZE);
			log.info(String.format("DB maintenance: %d rides inserted in total", totalCount));
		} catch (Exception ex) {
			log.error("Error fetching open ride templates: " + ex.toString());
		}
	}

	protected List<Ride> generateNonOverlappingRides(RideTemplate template, Instant systemHorizon) {
		// Get the ride instances that overlap or are beyond the template ride
		List<Ride> ridesBeyondTemplate  = rideDao.findRidesBeyondTemplate(template);
		List<Ride> rides = template.generateRides(systemHorizon);
		// Remove rides that overlap with existing future rides
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
		List<Ride> rides = generateNonOverlappingRides(template, systemHorizon);
		for (Ride ride : rides) {
        	rideDao.save(ride);
		}
		return rides.size();
	}

    /**
     * List all rides owned by the calling user (driverId is null) or owned by the specified user. Soft deleted rides are omitted.
     * @return A list of rides owned by the calling user.
     * @throws BadRequestException 
     */
    public PagedResult<Ride> listRides(Long driverId, Instant since, Instant until, Boolean deletedToo, Integer maxResults, Integer offset) throws NotFoundException, BadRequestException {
    	if (since == null) {
    		since = Instant.now();
    	}
    	if (until != null && since != null && ! until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: The 'until' date must be greater than the 'since' date.");
    	}
    	if (maxResults != null && maxResults > 100) {
    		throw new BadRequestException("Constraint violation: 'maxResults' <= 100.");
    	}
    	if (maxResults != null && maxResults <= 0) {
    		throw new BadRequestException("Constraint violation: 'maxResults' > 0.");
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
    		throw new BadRequestException("Constraint violation: 'driverId' is manadatory.");
    	}
    	driver = userDao.find(driverId)
    				.orElseThrow(() -> new NotFoundException("No such user: " + driverId));
    	List<Ride> results = Collections.emptyList();
        Long totalCount = 0L;
		PagedResult<Long> prs = rideDao.findByDriver(driver, since, until, deletedToo, 0, 0);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> rideIds = rideDao.findByDriver(driver, since, until, deletedToo, maxResults, offset);
    		if (rideIds.getData().size() > 0) {
    			results = rideDao.fetch(rideIds.getData(), Ride.BOOKINGS_ENTITY_GRAPH);
    		}
    	}
    	return new PagedResult<Ride>(results, maxResults, offset, totalCount);
    }
    
    public PagedResult<Ride> search(GeoLocation fromPlace, GeoLocation toPlace, Instant earliestDeparture, Instant latestDeparture, Integer nrSeats, Integer maxResults, Integer offset) {
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
        PagedResult<Long> prs = rideDao.search(fromPlace, toPlace, MAX_BEARING_DIFFERENCE, earliestDeparture, latestDeparture, nrSeats, 0, 0);
        Long totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		PagedResult<Long> rideIds = rideDao.search(fromPlace, toPlace, MAX_BEARING_DIFFERENCE, earliestDeparture, latestDeparture, nrSeats, maxResults, offset);
        	if (! rideIds.getData().isEmpty()) {
        		results = rideDao.fetch(rideIds.getData(), Ride.SEARCH_RIDES_ENTITY_GRAPH);
        	}
    	}
    	return new PagedResult<Ride>(results, maxResults, offset, totalCount);
    }
 
    private void validateCreateUpdateRide(Ride ride)  throws CreateException {
//    	if (ride.getRideTemplate() == null) {
//    		throw new CreateException("Constraint violation: A ride must have a template");
//    	}
    	if (ride.getDepartureTime() == null && ride.getArrivalTime() == null) {
    		throw new CreateException("Constraint violation: A ride must have a 'departureTime' and/or an 'arrivalTime'");
    	}
    	if (ride.getFrom() == null || ride.getTo() == null) {
    		throw new CreateException("Constraint violation: A new ride must have a 'from' and a 'to'");
    	}
    	if (ride.getBookings() != null && !ride.getBookings().isEmpty()) {
    		throw new CreateException("Constraint violation: A new ride cannot contain bookings");
    	}
    	if (ride.getCarRef() == null) {
    		throw new CreateException("Constraint violation: A ride must have a car defined");
    	}
    	if (ride.getMaxDetourMeters() != null && ride.getMaxDetourMeters() <= 0) {
    		throw new CreateException("Constraint violation: The maximum detour in meters must be greater than 0");
    	}
    	if (ride.getMaxDetourSeconds() != null && ride.getMaxDetourSeconds() <= 0) {
    		throw new CreateException("Constraint violation: The maximum detour in seconds must be greater than 0");
    	}
    }

    private void updateRideItinerary(Ride ride) throws CreateException {
    	// Create the route to drive and create the leg graph
    	List<Leg> newLegs = null;
    	try {
			newLegs = Arrays.asList(otpDao.createItinerary(ride));
	    	// Set the sequence of the legs
	    	AtomicInteger index = new AtomicInteger(0);
	    	newLegs.forEach(leg -> leg.setLegIx(index.getAndIncrement()));
		} catch (NotFoundException | BadRequestException e) {
			throw new CreateException("Cannot compute itinerary", e);
		}
    	// Update the ride leg structure with the results from the planner. 
    	// FIXME Allow transfer time for pickup and drop-off of the passenger
    	// Remove all booking info from old legs
    	ride.getLegs().forEach(leg -> leg.getBookings().clear());
    	
    	// Get the stops of the graph: The first departure and then all arrivals
    	List<Stop> newStops = new ArrayList<>();
    	newStops.add(newLegs.get(0).getFrom());
    	newLegs.forEach(leg -> newStops.add(leg.getTo()));

    	if (ride.getId() == null) {
    		// A fresh new ride not yet stored in  the database. Cascading persist will do the job.
    		newStops.forEach(s -> ride.addStop(s));
    		newLegs.forEach(leg -> ride.addLeg(leg));
    	} else {
	    	// Replace the old stop structure
	    	List<Stop> oldStops = ride.getStops(); 
	    	int i;
	        for (i = 0; i < oldStops.size() && i < newStops.size(); i++) {
	        	Stop oldStop = oldStops.get(i);
	        	Stop newStop = newStops.get(i);
	        	// Overwrite the old stop with the attributes from the new stop, first set the keys
	        	newStop.setId(oldStop.getId());
	        	newStop.setRide(ride);
	    		stopDao.merge(newStops.get(i));
			}
	        // New list is shorter than old list
	        // Decrease the length of the list
	    	for (; i < oldStops.size(); i++) {
	    		ride.removeStop(oldStops.get(i));
	    		// Orphan removal will remove the stop
			}
	        // New list is longer than old list, add remaining stops
	    	// Increase the length of the list
	    	for (i = oldStops.size(); i < newStops.size(); i++) {
	    		Stop newStop = newStops.get(i);
	    		ride.addStop(newStop);
	   			stopDao.save(newStop);
			}
	
	    	// Replace the old leg structure
	    	List<Leg> oldLegs = ride.getLegs(); 
	    	for (i = 0; i < oldLegs.size() && i < newLegs.size(); i++) {
	        	// Overwrite the old leg with the attributes from the new leg, first set the keys
	        	Leg oldLeg = oldLegs.get(i);
	        	Leg newLeg = newLegs.get(i);
	    		newLeg.setId(oldLeg.getId());
	    		newLeg.setRide(ride);
	    		legDao.merge(newLeg);
			}
	    	for (; i < oldLegs.size(); i++) {
	    		ride.removeLeg(oldLegs.get(i));
	    		// Orphan removal will remove the stop
			}
	    	for (i = oldLegs.size(); i < newLegs.size(); i++) {
	    		Leg newLeg = newLegs.get(i);
        		ride.addLeg(newLeg);
       			legDao.save(newLeg);
			}
    	}
    	// Compute the leg - booking relationship
    	// For each booking: Determine the first leg and the last leg, then add intermediate legs.
    	// In case of a single booking there is always just one leg. 
    	ClosenessFilter closenessFilter = new ClosenessFilter(MAX_BOOKING_LOCATION_SHIFT);    	
    	for (Booking booking : ride.getBookings()) {
    		if (booking.isDeleted()) {
    			continue;
    		}
    		Leg start = ride.getLegs().stream()
    				.filter(leg -> closenessFilter.test(leg.getFrom().getLocation(), booking.getPickup()))
    				.findFirst()
    				.orElseThrow(() -> new IllegalStateException("Cannot find first leg for booking"));
    		Leg last = ride.getLegs().stream()
    				.filter(leg -> closenessFilter.test(leg.getTo().getLocation(), booking.getDropOff()))
    				.findFirst()
    				.orElseThrow(() -> new IllegalStateException("Cannot find last leg for booking"));
    		// Get the start, the end and everything in between ans them to the booking 
			booking.getLegs().addAll(ride.getLegs().subList(ride.getLegs().indexOf(start), ride.getLegs().indexOf(last) + 1));
		}
    	int distance = ride.getLegs().stream().collect(Collectors.summingInt(Leg::getDistance));
    	ride.setDistance(distance);
    	// Old cars don't have the CO2 emission specification
    	if (ride.getCar().getCo2Emission() != null) {
    		ride.setCO2Emission(Math.toIntExact(Math.round(ride.getDistance() * ride.getCar().getCo2Emission() / 1000.0)));
    	}
    	if (ride.isArrivalTimePinned()) {
    		Stop departureStop = ride.getLegs().get(0).getFrom();
    		ride.setDepartureTime(departureStop.getDepartureTime());
    	} else {
    		Stop arrivalStop = ride.getLegs().get(ride.getLegs().size() - 1).getTo();
    		ride.setArrivalTime(arrivalStop.getArrivalTime());
    	}
    }

    /**
     * Creates a ride. In case recurrence is set, all following rides are created as well, up to 8 weeks in advance.
     * A ride has a template only for recurrent rides.
     * The owner of the ride is determined by the car. |Becasue of that, the driver does already exist in the local database.  
     * @param ride The input from the application.
     * @return The ID of the ride just created.
     * @throws CreateException In case of trouble like wrong parameter values.
     * @throws NotFoundException If the car is not found.
     */
    public Long createRide(Ride ride) throws CreateException, NotFoundException {
    	Car car = carDao.find(RideshareUrnHelper.getId(Car.URN_PREFIX, ride.getCarRef()))
    			.orElseThrow(() -> new CreateException("Cannot find car: " + ride.getCarRef()));
    	ride.setCar(car);
    	ride.setDriver(car.getDriver());
    	validateCreateUpdateRide(ride);
    	if (! RideshareUrnHelper.getId(User.URN_PREFIX, car.getDriverRef()).equals(ride.getDriver().getId())) {
    		throw new CreateException("Constraint violation: The car is not owned by the owner of the ride.");
    	}
    	// If the ride contains no departure then, then the arrival time is important
    	if (ride.getDepartureTime() == null) {
    		ride.setArrivalTimePinned(true);
    	}
    	ride.updateShareEligibility();

    	// Update the car itinerary from the route planner
    	updateRideItinerary(ride);
    	
    	Long rideId = null;
    	if (ride.getRideTemplate() != null && ride.getRideTemplate().getRecurrence() == null) {
    		log.warn("Inconsistence detected: Template defined without recurrency");
    		ride.setRideTemplate(null);
    	}
    	if (ride.getRideTemplate() == null) {
        	rideDao.save(ride);
        	rideId = ride.getId();
    	} else {
    		RideTemplate template = ride.getRideTemplate();
    		// Copy the (new) ride to the template
    		RideBase.copy(ride, template);
    		// Copy the geometry obtained from the planner to the template
    		// Note: The strategy is here to calculate the route of a recurrent ride only once.
    		//       Alternative is to make the route time-dependent and to calculate the route for each generated ride.
    		//       In the latter case the legGeometry does not need to be on the template.
    		template.setLegGeometry(ride.getLegs().get(0).getLegGeometry());
    		rideTemplateDao.save(template);
			Instant systemHorizon = getDefaultSystemHorizon();
			// Create rides including the initial simple leg structure.
			List<Ride> rides = generateNonOverlappingRides(template, systemHorizon);
			for (Ride r : rides) {
	        	rideDao.save(r);
			}
    		rideId = rides.get(0).getId();
    	}
    	return rideId;
    }

    
    /**
     * Retrieves a ride. Anyone can read a ride, given the id. All details are retrieved.
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Ride getRide(Long id) throws NotFoundException {
    	Ride ridedb = rideDao.find(id, rideDao.createLoadHint(Ride.BOOKINGS_ENTITY_GRAPH))
    			.orElseThrow(NotFoundException::new);
    	return ridedb;
    }

    /**
     * Updates an existing ride. If a booking is involved, it might change. The booking details themselves cannot be changed trough this call. 
     * It is not possible to change the driver.
     * @param ride The ride with the ID already provided
     * @throws CreateException
     * @throws NotFoundException
     */
    //FIXME
    public void updateRide(Ride ride) throws CreateException, NotFoundException {
    	Ride ridedb = rideDao.find(ride.getId())
    			.orElseThrow(NotFoundException::new);
    	ride.setDriver(ridedb.getDriver());
    	if (ridedb.getBookings().stream().filter(b -> ! b.isDeleted()).collect(Collectors.counting()) > 0) {
    		// What if there is already a booking
    		throw new CreateException("The ride has already bookings, an update is not allowed");
    	}
    	validateCreateUpdateRide(ride);
    	Long carId = ride.getCar() != null ? ride.getCar().getId() :  RideshareUrnHelper.getId(Car.URN_PREFIX, ride.getCarRef());
    	if (carId == null) {
    		new NotFoundException("No Car ID found for Ride " + ride.getId());
    	}
    	Car car = carDao.find(carId)
    			.orElseThrow(() -> new CreateException("Cannot find car: " + carId));
    	if (! car.getDriver().equals(ridedb.getDriver())) {
    		throw new CreateException("Constraint violation: The car is not owned by the owner of the ride.");
    	}
    	Recurrence recc = ride.getRideTemplate() != null ? ride.getRideTemplate().getRecurrence() : null;  
    	// Set all relations
    	ride.setCar(ridedb.getCar());
    	ride.setRideTemplate(ridedb.getRideTemplate());
    	rideDao.merge(ride);
    	updateRideItinerary(ride);
    	
    	// Now comes the difficult part with the recurrence
    	// 1. No recurrence in DB nor update -> no template
    	// 2. Recurrence in update only --> create template and generate rides as with a new ride
    	// 3. Recurrence in DB only. 
    	// 3.1. THIS Remove template for this ride. Ride is no longer recurrent.
    	// 3.2. THIS_AND_FOLLOWING Set horizon at current ride, remove future ride, save template. Remove template from ride.
    	// 4. Recurrence in DB and update
    	// 4.1 Recurrence is same
    	// 4.1.1. THIS Create a new template for this ride, generate rides
    	// 4.1.2. THIS_AND_FOLLOWING Set horizon at current template, remove all future rides and generate rides
    	// 4.2. Recurrence has changed - Remove all future unbooked rides
    	// 4.2.1. THIS Create a new template for this ride, generate rides
    	// 4.2.2. THIS_AND_FOLLOWING Set horizon at current template, remove all future rides (or reuse them) and generate rides
    	throw new UnsupportedOperationException("Update of a ride is not allowed");
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
    	if (ridedb.getBookings().size() > 0) {
    		// Perform a soft delete
    		ridedb.setDeleted(true);
    		ridedb.getBookings().stream()
	    		.filter(b -> ! b.isDeleted())
	    		.forEach(b -> b.markAsCancelled(reason, true));
    		// FIXME send message to passengers
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

    /**
     * Observes changes on rides and updates the legs of the ride.
     * @param ride the ride to update.
     */
    public void onUpdateRideItinerary(@Observes Ride ride) throws Exception {
//    	try {
        	Ride ridedb = rideDao.find(ride.getId())
        			.orElseThrow(NotFoundException::new);
    		updateRideItinerary(ridedb);
//    	} catch (Exception ex) {
//    		log.error(String.format("Error updating ride %d", ride.getId()), ex);
//    	}
    }
}
