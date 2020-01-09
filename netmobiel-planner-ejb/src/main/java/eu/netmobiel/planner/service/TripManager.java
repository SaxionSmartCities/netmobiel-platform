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
    public List<Trip> listTrips(User traveller, Instant since, Instant until) throws BadRequestException {
    	List<Trip> trips = Collections.emptyList();
//    	if (since == null) {
//    		since = Instant.now();
//    	}
    	if (until != null && since != null && ! until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: 'until' must be later than 'since'.");
    	}
    	if (traveller != null) {
    		trips = tripDao.findByTraveller(traveller, since, until, false, Trip.LIST_TRIPS_ENTITY_GRAPH);
    	}
    	return trips;
    	
    }

    /**
     * List all trips owned by  the calling user. Soft deleted trips are omitted.
     * @return A list of trips owned by the calling user.
     */
    public List<Trip> listMyTrips(Instant since, Instant until) throws BadRequestException {
    	return listTrips(userManager.findCallingUser(), since, until);
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
     * Removes a trip. If the trip contains a booked ride, it is soft-deleted. 
     * If a ride is recurring and the scope is set to <code>this-and-following</code> 
     * then all following trips are removed as well. The <code>horizon</code> date of the
     * preceding trips of the same trip, if any, is set to the day of the departure 
     * date of the ride being deleted.
     * @param rideId The ride to remove.
     * @param reason The reason why it was cancelled (optional).
     * @throws NotFoundException
     */
    public void removeTrip(Long rideId, final String reason) throws NotFoundException {
    	Trip tripdb = tripDao.find(rideId)
    			.orElseThrow(NotFoundException::new);
//    	security.checkOwnership(tripdb.getTraveller(), Trip.class.getSimpleName());
//    	tripdb.getItinerary().legs.forEach(leg -> {
//    		if (leg.ride != null) {
//    			 We have a ride. Cancel it
//    			rideshareDao.
//    		}
//    	});
//    	if (tripdb.getBookings().size() > 0) {
//    		// Perform a soft delete
//    		tripdb.setDeleted(true);
//    		tripdb.getBookings().forEach(b -> b.markAsCancelled(reason, true));
//		} else {
//			tripDao.remove(tripdb);
//		}
    }
 
}
