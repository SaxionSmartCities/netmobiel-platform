package eu.netmobiel.rideshare.api.resource;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import com.github.dozermapper.core.Mapper;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.rideshare.api.SearchApi;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.RideManager;

@ApplicationScoped
public class SearchResource implements SearchApi {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;
 
	@Inject
	private Mapper mapper;
	
	@Inject
    private RideManager rideManager;

    /**
     * Search rides that fit the query..
     * Searches for matching rides. The following rules apply:<br/>
     * 1. Pickup (from) and dropoff (to) are within eligibility area of the driver's trajectory;
     * 2. The ride is in the future (near the specified date);
     * 3. The car has enough seats available [restriction: only 1 booking allowed now]. 
     * 4. The ride has not been deleted.
     * 5. The passenger and driver should more or less travel in the same direction.
     * @param fromPlace The location for pickup
     * @param toPlace The location for drop-off
     * @param fromDate The (local) date and time to depart
     * @param toDate The (local) date and time to arrive
     * @param nrSeats the number of seats required
     * @param maxResults pagination: maximum number of results
     * @param offset pagination: The offset to start (start at 0)
     * @return an array of rides.
     */
    @Override
	public Response searchRides(
    		String fromPlace, 
    		String toPlace, 
    		String fromDate,
    		String toDate,
    		Integer nrSeats,
    		Integer maxResults,
    		Integer offset
    	) {
    	List<Ride> rides = null;
    	if (fromPlace == null || (fromDate == null && toDate == null) || toPlace == null) {
    		throw new BadRequestException("Missing one or more mandatory parameters: fromPlace, toPlace, fromDate or toDate");
    	} else if (fromPlace != null && toPlace != null && (fromDate != null || toDate != null)) {
    		try {
    			LocalDateTime departureDate = fromDate != null ? LocalDateTime.parse(fromDate).minusHours(1) : null;
    			LocalDateTime arrivalDate = toDate != null ? LocalDateTime.parse(toDate).plusHours(1) : null;
	    		rides = rideManager.search(GeoLocation.fromString(fromPlace), GeoLocation.fromString(toPlace), departureDate, arrivalDate, nrSeats, maxResults, offset);
    		} catch (DateTimeParseException ex) {
    			throw new BadRequestException("Date parameter has unrecognized format", ex);
    		} catch (IllegalArgumentException ex) {
    			throw new BadRequestException("Input parameter has unrecognized format", ex);
    		}
    	}
    	return Response.ok(rides.stream().map(r -> mapper.map(r, eu.netmobiel.rideshare.api.model.Ride.class, "search")).collect(Collectors.toList())).build(); 
    }

}
