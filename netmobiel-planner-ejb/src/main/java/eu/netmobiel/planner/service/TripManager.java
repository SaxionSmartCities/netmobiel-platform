package eu.netmobiel.planner.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

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
     * List all trips owned by  the calling user. Soft deleted trips are omitted.
     * @return A list of trips owned by the calling user.
     */
    public List<Trip> listMyTrips(LocalDate since, LocalDate until) throws FinderException {
    	List<Trip> trips = Collections.emptyList();
    	if (since == null) {
    		since = LocalDate.now();
    	}
    	if (until != null && since != null && ! until.isAfter(since)) {
    		throw new FinderException("Constraint violation: The 'until' date must be greater than the 'since' date.");
    	}
    	User caller = userManager.findCallingUser();
    	if (caller != null) {
    		trips = tripDao.findByTraveller(caller, since, until, false, null);
    	}
    	return trips;
    	
    }
    
    private void validateCreateUpdateTrip(Trip trip)  throws CreateException {
    	if (trip.getDepartureTime() == null) {
    		throw new CreateException("Constraint violation: A new trip must have a 'departureTime'");
    	}
    	if (trip.getFrom() == null || trip.getTo() == null) {
    		throw new CreateException("Constraint violation: A new trip must have a 'fromPlace' and a 'toPlace'");
    	}
    }

    /**
     * Creates a trip. 
     * @param trip the new trip
     * @return The ID of the trip just created.
     * @throws CreateException In case of trouble, like wrong parameter values.
     * @throws ObjectNotFoundException
     */
    public Long createTrip(Trip trip) throws CreateException, ObjectNotFoundException {
    	User caller = userManager.registerCallingUser();
    	validateCreateUpdateTrip(trip);
    	trip.setTraveller(caller);
       	tripDao.save(trip);
    	return trip.getId();
    }

    /**
     * Retrieves a ride. Anyone can read a ride, given the id. All details are retrieved.
     * @param id
     * @return
     * @throws ObjectNotFoundException
     */
    public Trip getTrip(Long id) throws ObjectNotFoundException {
    	Trip tripdb = tripDao.find(id, tripDao.createLoadHint(null))
    			.orElseThrow(ObjectNotFoundException::new);
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
     * @throws ObjectNotFoundException
     */
    public void removeTrip(Long rideId, final String reason) throws ObjectNotFoundException {
    	Trip tripdb = tripDao.find(rideId)
    			.orElseThrow(ObjectNotFoundException::new);
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
