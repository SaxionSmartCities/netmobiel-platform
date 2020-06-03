package eu.netmobiel.rideshare.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.exception.SoftRemovedException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.rideshare.api.RidesApi;
import eu.netmobiel.rideshare.api.mapping.BookingMapper;
import eu.netmobiel.rideshare.api.mapping.PageMapper;
import eu.netmobiel.rideshare.api.mapping.RideMapper;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideScope;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.service.RideManager;
import eu.netmobiel.rideshare.service.UserManager;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@RequestScoped
public class RidesResource implements RidesApi {

    @Inject
    private RideManager rideManager;

    @Inject
    private BookingManager bookingManager;

    @Inject
    private RideMapper mapper;

    @Inject
    private BookingMapper bookingMapper;
    
    @Inject
    private PageMapper pageMapper;
    
    @Inject
    private UserManager userManager;
    
	private Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    /**
     * List all rides owned by the calling user. Soft deleted rides are omitted.
     * @return A list of rides owned by the calling user.
     */
    public Response listRides(String driverId, OffsetDateTime sinceDate, OffsetDateTime untilDate, Boolean deletedToo, Integer maxResults, Integer offset) {
//    	LocalDate sinceDate = since != null ? LocalDate.parse(since) : null;
//    	LocalDate untilDate =  until != null ? LocalDate.parse(until) : null;
    	if (sinceDate == null) {
    		sinceDate = OffsetDateTime.now();
    	}

    	PagedResult<Ride> rides;
		try {
			Long did = null;
			if (driverId != null) {
				did = RideshareUrnHelper.getId(User.URN_PREFIX, driverId);
			} else {
				did = userManager.findCallingUser().getId();
			}
			if (did != null) {
				rides = rideManager.listRides(did, toInstant(sinceDate), toInstant(untilDate), deletedToo, maxResults, offset);
			} else {
				rides = new PagedResult<Ride>();
			}
		} catch (eu.netmobiel.commons.exception.BadRequestException | eu.netmobiel.commons.exception.NotFoundException e) {
			throw new WebApplicationException("Error listing rides", e);
		}
		// Map the rides as my rides: Brand/model car only, no driver info (because it is the specified driver)
    	return Response.ok(pageMapper.mapMine(rides)).build();
    }

    /**
     * Creates a single ride, if recurrence is set then create rides up to 8 weeks in advance.
     * @param ridedt the ride object. In case of recurrence the ride is used as a template. The ride is
     * in the body of the post.
     * @return A 201 status is returned with the Location header set to the first ride created.
     */
    @Override
	public Response createRide(eu.netmobiel.rideshare.api.model.Ride ridedt) {
    	Response rsp = null;
		try {
			Ride ride = mapper.map(ridedt);
			// The owner of the ride will be the owner of the car.
			String newRideId = RideshareUrnHelper.createUrn(Ride.URN_PREFIX, rideManager.createRide(ride));
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newRideId)).build();
		} catch (ApplicationException e) {
			throw new BadRequestException("Error creating ride", e);
		}
    	return rsp;
    }
    
    /**
     * Return all information about a single ride. 
     * @param rideId The id of the ride. Both the primary key (a number) as the urn format are accepted.
     * @return A ride object if found.
     */
    @Override
	public Response getRide(String rideId) {
    	Ride ride = null;
    	try {
        	Long cid = RideshareUrnHelper.getId(Ride.URN_PREFIX, rideId);
			ride = rideManager.getRide(cid);
		} catch (eu.netmobiel.commons.exception.NotFoundException e) {
			throw new NotFoundException();
		}
    	// Return all information, including car, driver and bookings
    	return Response.ok(mapper.mapDetailed(ride)).build();
    }

    /**
     * Updates a ride. 
     * @param rideId The ride id.  Both the primary key (a number) as the urn format are accepted.
     * @param scope The scope of the delete action in case of a recurrent ride: Only this one or this one and all following.
     * 		If not set then the scope is set to THIS.
     * @param ridedt The new value of the ride. 
     * @return
     */
    public Response updateRide(String rideId, String scope, eu.netmobiel.rideshare.api.model.Ride ridedt) {
    	Response rsp = null;
    	try {
			Ride ride = mapper.map(ridedt);
    		RideScope rs = scope == null ? RideScope.THIS: RideScope.lookup(scope);
			rideManager.updateRide(ride, rs);
			rsp = Response.noContent().build();
		} catch (ApplicationException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }

    /**
     * Deletes a ride. If a ride is already booked then the ride is soft deleted. Soft deleted rides are 
     * default not listed and can never be found.
     * @param rideId The ride id.  Both the primary key (a number) as the urn format are accepted.
     * @param scope The scope of the delete action in case of a recurrent ride: Only this one or this one and all following.
     * 		If not set then the scope is set to THIS.
     * @param reason The (optional) reason why the ride was cancelled.
     * @return
     */
    @Override
	public Response deleteRide(String rideId, String scope, String reason) {
    	Response rsp = null;
    	try {
    		RideScope rs = scope == null ? RideScope.THIS: RideScope.lookup(scope);
        	Long cid = RideshareUrnHelper.getId(Ride.URN_PREFIX, rideId);
			rideManager.removeRide(cid, reason, rs);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (eu.netmobiel.commons.exception.NotFoundException e) {
	    	rsp = Response.status(Status.NOT_FOUND).build();
		} catch (SoftRemovedException e) {
	    	rsp = Response.status(Status.GONE).build();
		}
    	return rsp;
    }

    /**
     * Create a booking on a ride.
     * @param rideId The ride id.  Both the primary key (a number) as the urn format are accepted.
     * @param bookingdt The booking to make, this is the body of the post.
     * @return A 201 status is returned with the Location header set to the booking created.
     */
    @Override
	public Response createBooking(String rideId, eu.netmobiel.rideshare.api.model.Booking bookingdt)  {
    	Response rsp = null;
		try {
			User passenger = userManager.registerCallingUser();
        	Booking booking = bookingMapper.map(bookingdt);
			String newBookingId = bookingManager.createBooking(rideId, passenger, booking);
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newBookingId)).build();
		} catch (ApplicationException e) {
			throw new BadRequestException("Error creating booking for ride " + rideId, e);
		}
    	return rsp;
    }
    
}
