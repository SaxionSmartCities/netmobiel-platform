package eu.netmobiel.planner.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.TripDao;

@Stateless
@Logging
public class TripManager {
	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Inject
    private TripDao tripDao;
    
    @EJB(name = "java:app/netmobiel-planner-ejb/UserManager")
    private UserManager userManager;

    /**
     * List all trips owned by the specified user. Soft deleted trips are omitted.
     * @return A list of trips owned by the specified user.
     */
    public List<Trip> listTrips(User traveller, Instant since, Instant until, Boolean deletedToo, Integer maxResults, Integer offset) throws BadRequestException {
    	List<Trip> trips = Collections.emptyList();
    	if (until != null && since != null && !until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: 'until' must be later than 'since'.");
    	}
    	if (maxResults != null && maxResults > 100) {
    		throw new BadRequestException("Constraint violation: 'maxResults' <= 100.");
    	}
    	if (maxResults != null && maxResults <= 0) {
    		throw new BadRequestException("Constraint violation: 'maxResults' > 0.");
    	}
    	if (maxResults != null && offset < 0) {
    		throw new BadRequestException("Constraint violation: 'offset' >= 0.");
    	}
    	if (traveller != null) {
    		List<Long> tripIds = tripDao.findByTraveller(traveller, since, until, deletedToo, maxResults, offset);
    		if (tripIds.size() > 0) {
    			trips = tripDao.fetch(tripIds, Trip.LIST_TRIPS_ENTITY_GRAPH);
    		}
    	}
    	return trips;
    	
    }

    /**
     * List all trips owned by  the calling user. Soft deleted trips are omitted.
     * @return A list of trips owned by the calling user.
     */
    public List<Trip> listMyTrips(Instant since, Instant until, Boolean deletedToo, Integer maxResults, Integer offset) throws BadRequestException {
    	return listTrips(userManager.findCallingUser(), since, until, deletedToo, maxResults, offset);
    }
    
    private void validateCreateUpdateTrip(Trip trip)  throws BadRequestException {
    	if (trip.getDepartureTime() == null) {
    		throw new BadRequestException("Constraint violation: A new trip must have a 'departureTime'");
    	}
    	if (trip.getFrom() == null || trip.getTo() == null) {
    		throw new BadRequestException("Constraint violation: A new trip must have a 'fromPlace' and a 'toPlace'");
    	}
    }

    /**
     * Creates a trip on behalf of a user. 
     * @param user the user for whom the trip is created
     * @param trip the new trip
     * @return The ID of the trip just created.
     * @throws CreateException In case of trouble, like wrong parameter values.
     * @throws BadRequestException In case of bad parameters.
     */
    public Long createTrip(User traveller, Trip trip) throws BadRequestException {
    	validateCreateUpdateTrip(trip);
    	trip.setTraveller(traveller);
       	tripDao.save(trip);
    	return trip.getId();
    }
    /**
     * Creates a trip. 
     * @param trip the new trip
     * @return The ID of the trip just created.
     * @throws CreateException In case of trouble, like wrong parameter values.
     * @throws BadRequestException In case of bad parameters.
     */
    public Long createTrip(Trip trip) throws BadRequestException {
    	return createTrip(userManager.registerCallingUser(), trip);
    }

    /**
     * Retrieves a ride. Anyone can read a ride, given the id. All details are retrieved.
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Trip getTrip(Long id) throws NotFoundException {
    	Trip tripdb = tripDao.find(id, tripDao.createLoadHint(null))
    			.orElseThrow(NotFoundException::new);
    	return tripdb;
    }
    
    /**
     * Removes a trip. Whether or not a trip is soft-deleted or hard-deleted dependson the trip state.
     * @param tripId The trip to remove.
     * @throws NotFoundException The trip doesnot exist.
     */
    public void removeTrip(Long tripId) throws NotFoundException {
    	Trip tripdb = tripDao.find(tripId)
    			.orElseThrow(NotFoundException::new);
    	//    	security.checkOwnership(tripdb.getTraveller(), Trip.class.getSimpleName());
    	if (tripdb.getState() == TripState.PLANNING) {
    		// Hard delete
			tripDao.remove(tripdb);
    	} else {
    		tripdb.setDeleted(true);
    	}
    }
 
}
