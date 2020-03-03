package eu.netmobiel.rideshare.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.commons.util.EllipseHelper.EligibleArea;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.opentripplanner.api.model.Leg;
import eu.netmobiel.opentripplanner.api.model.PlanResponse;
import eu.netmobiel.opentripplanner.api.model.TraverseMode;
import eu.netmobiel.opentripplanner.client.OpenTripPlannerClient;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.RecurrenceIterator;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideScope;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.CarDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.RideTemplateDao;
import eu.netmobiel.rideshare.repository.StopDao;
import eu.netmobiel.rideshare.repository.UserDao;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@Stateless
@Logging
public class RideManager {
	public static final String AGENCY_NAME = "NetMobiel Rideshare Service";
	public static final String AGENCY_ID = "NB:RS";
	
	private static final float DEFAULT_RELATIVE_MAX_DETOUR = 0.30f;
	private static final float DEFAULT_NOMINAL_SPEED = 25 * 1000 / 3600; 	/* km/h --> m/s */
	/**
	 * The driver has a vector, the passenger has a vector. 
	 * The difference in direction is limited for a match. Otherwise the driver has to drive in the
	 * wrong direction. The total bearing matching angle is twice the difference 
	 * (driver bearing - diff, driver bearing + diff). 
	 */
	private static final int MAX_BEARING_DIFFERENCE = 60; 	/* degrees */
	private static final Integer OTP_MAX_WALK_DISTANCE = 500;
	private static final int HORIZON_WEEKS = 8;
	@Inject
    private Logger log;

    @Inject
    private UserDao userDao;
    @Inject
    private CarDao carDao;
    @Inject
    private RideDao rideDao;
    @Inject
    private RideTemplateDao rideTemplateDao;
    @Inject
    private StopDao stopDao;
    @Inject
    private OpenTripPlannerClient otpClient;
    
    @Inject
    private UserManager userManager;

    /**
     * Updates all recurrent rides by moving the system horizon to the next day.
     * To prevent to re-insert removed recurrent rides we have to limit the window to the previous horizon, i.e. yesterday  
     */
	@Schedule(info = "Ride Maintenance", hour = "2", minute = "15", second = "0", persistent = false /* non-critical job */)
	public void instantiateRecurrentRides() {
		try {
			instantiateRecurrentRides(null);
		} catch (Exception ex) {
			log.error("Error updating recurrence horizons: " + ex.toString());
		}
	}
	
	/**
	 * Extends the recurrent rides, starting at the start date.
     * @param startDate the first date to start the iteration.  
	 */
	public void instantiateRecurrentRides(LocalDate startDate) {
		int count = 0;
		LocalDate horizon = LocalDate.now().plusWeeks(HORIZON_WEEKS);
		if (startDate == null) {
			startDate = horizon.minusDays(1);
		}
		log.info(String.format("DB maintenance: Move horizon to %s, check starts at %s", horizon.toString(), startDate.toString()));
		List<Ride> lastRecurrentRides = rideDao.findLastRecurrentRides();
		log.debug(String.format("Found %d last recurrent rides", lastRecurrentRides.size()));
		for (Ride ride : lastRecurrentRides) {
    		Collection<Ride> rides = generateRides(ride, startDate, horizon);
    		for (Ride r : rides) {
            	rideDao.save(r);
			}
    		count += rides.size();
		}
		log.info(String.format("Added %d recurrent rides", count));
	}

    /**
     * List all rides owned by the calling user (driverId is null) or owned by the specified user. Soft deleted rides are omitted.
     * @return A list of rides owned by the calling user.
     * @throws BadRequestException 
     */
    public List<Ride> listRides(Long driverId, LocalDate since, LocalDate until, Boolean deletedToo, Integer maxResults, Integer offset) throws BadRequestException {
    	List<Ride> rides = Collections.emptyList();
    	if (since == null) {
    		since = LocalDate.now();
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
    	User driver = null;
    	if (driverId != null) {
    		driver = userDao.find(driverId)
    					.orElse(null);
    	}
    	if (driver != null) {
    		List<Long> rideIds = rideDao.findByDriver(driver, since, until, deletedToo, maxResults, offset);
    		if (rideIds.size() > 0) {
    			rides = rideDao.fetch(rideIds, Ride.BOOKINGS_ENTITY_GRAPH);
    		}
    	}
    	return rides;
    	
    }
    
    public List<Ride> search(GeoLocation fromPlace, GeoLocation toPlace, LocalDateTime fromDate, LocalDateTime toDate, Integer nrSeats, Integer maxResults, Integer offset) {
    	if (nrSeats == null) {
    		nrSeats = 1;
    	}
    	if (maxResults == null) {
    		maxResults = 10;
    	}
    	if (offset == null) {
    		offset = 0;
    	}
    	List<Long> rideIds = rideDao.search(fromPlace, toPlace, MAX_BEARING_DIFFERENCE, fromDate, toDate, nrSeats, maxResults, offset);
    	List<Ride> rides = new ArrayList<>();
    	if (! rideIds.isEmpty()) {
    		rides.addAll(rideDao.fetch(rideIds, Ride.SEARCH_RIDES_ENTITY_GRAPH));
    	}
    	return rides;
    }
 
    private void validateCreateUpdateRide(Ride ride)  throws CreateException {
    	if (ride.getRideTemplate() == null) {
    		throw new CreateException("Constraint violation: A ride must have a template");
    	}
    	RideTemplate template = ride.getRideTemplate();
    	if (template.getFromPlace() == null || template.getToPlace() == null) {
    		throw new CreateException("Constraint violation: A new ride must have a 'fromPlace' and a 'toPlace'");
    	}
    	if (ride.getBookings() != null && !ride.getBookings().isEmpty()) {
    		throw new CreateException("Constraint violation: A new ride cannot contain bookings");
    	}
    	if (template.getCarRef() == null) {
    		throw new CreateException("Constraint violation: A ride must have a car defined");
    	}
    	if (template.getMaxDetourMeters() != null && template.getMaxDetourMeters() <= 0) {
    		throw new CreateException("Constraint violation: The maximum detour in meters must be greater than 0");
    	}
    	if (template.getMaxDetourSeconds() != null && template.getMaxDetourSeconds() <= 0) {
    		throw new CreateException("Constraint violation: The maximum detour in seconds must be greater than 0");
    	}
    }

    private void updateCarItinerary(Ride ride) {
    	RideTemplate template = ride.getRideTemplate();
    	EligibleArea ea = calculateShareEligibility(ride);
    	template.setShareEligibility(ea.eligibleAreaGeometry);
    	template.setCarthesianDistance(Math.toIntExact(Math.round(ea.carthesianDistance)));
    	template.setCarthesianBearing(Math.toIntExact(Math.round(ea.carthesianBearing)));
    	LocalDate date = ride.getDepartureTime().toLocalDate();
    	LocalTime time = ride.getDepartureTime().toLocalTime();
    	PlanResponse result = otpClient.createPlan(template.getFromPlace().getLocation(), template.getToPlace().getLocation(), 
    			date, time, false, new TraverseMode[] { TraverseMode.CAR }, false, OTP_MAX_WALK_DISTANCE, null, 1);
    	if (result.error != null) {
			String msg = String.format("Unable to determine reference CAR plan due to OTP Planner Error: %s - %s", result.error.message, result.error.msg);
			if (result.error.missing != null && result.error.missing.size() > 0) {
				msg = String.format("%s Missing parameters [ %s ]", msg, String.join(",", result.error.missing));
			}
			throw new SystemException(msg);
    	} else {
	    	if (log.isDebugEnabled()) {
	        	log.debug("Create plan for ride: \n" + result.plan.toString());
	    	}
	    	Leg leg = result.plan.itineraries.get(0).legs.get(0);
	    	template.setEstimatedDistance(Math.toIntExact(Math.round(leg.distance)));
	    	template.setEstimatedDrivingTime(Math.toIntExact(Math.round(leg.getDuration())));
	    	// Old cars don't have the CO2 emission specificiation
	    	if (template.getCar().getCo2Emission() != null) {
	    		template.setEstimatedCO2Emission(Math.toIntExact(Math.round(template.getEstimatedDistance() * template.getCar().getCo2Emission() / 1000.0)));
	    	}
			ride.updateEstimatedArrivalTime();
    	}    	
    }
    /**
     * Creates a ride. In case recurrence is set, all following rides are created as well, up to 8 weeks in advance. 
     * @param ride
     * @return The ID of the ride just created.
     * @throws CreateException In case of trouble like wrong parameter values.
     * @throws NotFoundException
     */
    public Long createRide(Ride ride) throws CreateException, NotFoundException {
    	User caller = userManager.registerCallingUser();
    	validateCreateUpdateRide(ride);
    	RideTemplate template = ride.getRideTemplate();
    	Car car = carDao.find(RideshareUrnHelper.getId(Car.URN_PREFIX, template.getCarRef()))
    			.orElseThrow(() -> new CreateException("Cannot find car: " + template.getCarRef()));
    	if (! RideshareUrnHelper.getId(User.URN_PREFIX, car.getDriverRef()).equals(caller.getId())) {
    		throw new CreateException("Constraint violation: The car is not owned by the owner of the ride.");
    	}
    	template.setDriver(caller);
    	template.setCar(car);
    	updateCarItinerary(ride);
    	Long rideId = null;
    	if (template.getRecurrence() == null) {
        	rideDao.save(ride);
        	rideId = ride.getId();
    	} else {
    		Collection<Ride> rides = generateRides(ride, ride.getDepartureTime().toLocalDate(), LocalDate.now().plusWeeks(HORIZON_WEEKS));
    		for (Ride r : rides) {
            	rideDao.save(r);
            	if (rideId == null) {
            		rideId = r.getId();
            	}
			}
    	}
    	return rideId;
    }

    protected Collection<Ride> generateRides(Ride ride, LocalDate firstDate,  LocalDate horizon) {
    	List<Ride> rides = new ArrayList<>();
		LocalTime time = ride.getDepartureTime().toLocalTime();
    	RecurrenceIterator rix = new RecurrenceIterator(ride.getRideTemplate().getRecurrence(), ride.getDepartureTime().toLocalDate(), firstDate, horizon);
    	while (rix.hasNext()) {
			LocalDateTime dt = LocalDateTime.of(rix.next(), time); 
			Ride r = Ride.createRide(ride.getRideTemplate(), dt);
			rides.add(r);
		}
    	return rides;
    }
    
    /**
     * Calculates an ellipse with the property that the distance from one focal point (departure stop) to
     * the border of the ellipse and then to the other focal point (arrival) is equal to the maximum detour distance.
     * The distance is calculated from the nominal speed (m/s). 
     * @param r the ride.
     * @return a Geometry (polygon) object.
     */
    protected EligibleArea calculateShareEligibility(Ride r) {
    	// See https://en.wikipedia.org/wiki/Ellipse
    	Integer maxDetourDistance = null;
    	if (r.getRideTemplate().getMaxDetourMeters() != null) {
    		maxDetourDistance = r.getRideTemplate().getMaxDetourMeters();
    	} else if (r.getRideTemplate().getMaxDetourSeconds() != null) {
    		maxDetourDistance = Math.round(r.getRideTemplate().getMaxDetourSeconds() * DEFAULT_NOMINAL_SPEED);    		
    	}
    	return EllipseHelper.calculateEllipse(r.getRideTemplate().getFromPlace().getLocation().getPoint(), 
    			r.getRideTemplate().getToPlace().getLocation().getPoint(), 
    			maxDetourDistance != null ? maxDetourDistance / 2.0 : null, DEFAULT_RELATIVE_MAX_DETOUR / 2);
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

    public void updateRide(Long rideId, Ride ride) throws CreateException, NotFoundException {
    	User caller = userManager.registerCallingUser();
    	Ride ridedb = rideDao.find(rideId)
    			.orElseThrow(NotFoundException::new);
    	userManager.checkOwnership(ridedb.getRideTemplate().getDriver(), Ride.class.getSimpleName());
    	if (ridedb.getBookings().size() > 0) {
    		// What if there is already a booking
    		throw new CreateException("THe ride has already bookings, an update is not allowed");
    	}
    	validateCreateUpdateRide(ride);
    	RideTemplate template = ride.getRideTemplate();
    	ride.setId(ridedb.getId());
    	Car car = carDao.find(RideshareUrnHelper.getId(Car.URN_PREFIX, template.getCarRef()))
    			.orElseThrow(() -> new CreateException("Cannot find car: " + template.getCarRef()));
    	if (! RideshareUrnHelper.getId(User.URN_PREFIX, car.getDriverRef()).equals(caller.getId())) {
    		throw new CreateException("Constraint violation: The car is not owned by the owner of the ride.");
    	}
    	// Set all relations
    	template.setDriver(caller);
    	template.setCar(car);
    	template.setId(ridedb.getRideTemplate().getId());
    	template.getFromPlace().setId(ridedb.getRideTemplate().getFromPlace().getId());
    	template.getToPlace().setId(ridedb.getRideTemplate().getToPlace().getId());
    	updateCarItinerary(ride);
    	// Now comes the difficult part with the recurrence
    	// 1. No recurrence in DB nor update -> merge
    	// 2. Recurrence in update only --> merge current ride, generate rides, starting with next day (the updated ride is the first)
    	// 3. Recurrence in DB only. 
    	// 3.1. THIS Create a new template for this ride
    	// 3.2. THIS_AND_FOLLOWING Set horizon at current template, remove all future rides
    	// 4. Recurrence in DB and update
    	// 4.1. THIS Create a new template for this ride, generata rides
    	// 4.2. THIS_AND_FOLLOWING Set horizon at current template, remove all future rides (or reuse them) and generate rides
    	throw new UnsupportedOperationException("Update of a ride is not allowed");
    }
    
    private void removeRide(Long rideId, final String reason) {
    	Ride ridedb;
		try {
			ridedb = rideDao.find(rideId)
					.orElseThrow(NotFoundException::new);
	    	if (ridedb.getBookings().size() > 0) {
	    		// Perform a soft delete
	    		ridedb.setDeleted(true);
	    		ridedb.getBookings().forEach(b -> b.markAsCancelled(reason, true));
			} else {
				rideDao.remove(ridedb);
//	    		ridedb.getStops().forEach(s -> stopDao.remove(s));
			}
		} catch (NotFoundException e) {
			log.warn(String.format("Ride %d not found, ignoring...", rideId));
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
     * @param scope The extent of deletion in case of a recurrent ride. 
     * @throws NotFoundException
     */
    public void removeRide(Long rideId, final String reason, RideScope scope) throws NotFoundException {
    	Ride ridedb = rideDao.find(rideId)
    			.orElseThrow(NotFoundException::new);
    	userManager.checkOwnership(ridedb.getRideTemplate().getDriver(), Ride.class.getSimpleName());
    	removeRide(rideId, reason);
    	if (scope == RideScope.THIS_AND_FOLLOWING) {
    		// Deletes this ride and all that follow
    		rideDao.findFollowingRideIds(ridedb.getRideTemplate(), ridedb.getDepartureTime())
    			.forEach(rid -> removeRide(rid, reason));
    		// Set the horizon of the template to the departure date of this ride.
    		ridedb.getRideTemplate().getRecurrence().setHorizon(LocalDate.from(ridedb.getDepartureTime().atZone(ZoneId.systemDefault())));
    	}
    	// Check how many rides are attached to the template. If 0 then delete the template too.
    	if (rideTemplateDao.getNrRidesAttached(ridedb.getRideTemplate()) == 0L) {
    		rideTemplateDao.remove(ridedb.getRideTemplate());
			stopDao.remove(ridedb.getRideTemplate().getFromPlace());
			stopDao.remove(ridedb.getRideTemplate().getToPlace());
    	}
    }
 
}
