package eu.netmobiel.rideshare.api.resource;

import java.net.URI;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.api.RidesApi;
import eu.netmobiel.rideshare.api.mapping.BookingMapper;
import eu.netmobiel.rideshare.api.mapping.PageMapper;
import eu.netmobiel.rideshare.api.mapping.RideMapper;
import eu.netmobiel.rideshare.filter.RideFilter;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideScope;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.service.RideManager;
import eu.netmobiel.rideshare.service.RideshareUserManager;

@RequestScoped
public class RidesResource extends RideshareResource implements RidesApi {

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
    private RideshareUserManager userManager;
    
    @Context
    private HttpServletRequest request;

    /**
     * List all rides owned by the calling user. Soft deleted rides are omitted.
     * @return A list of rides owned by the calling user.
     */
    @Override
	public Response listRides(String driverId, OffsetDateTime since, OffsetDateTime until, String state, String bookingState,
    		String siblingRideId, Boolean deletedToo, String sortDir, Integer maxResults, Integer offset) {
    	Response rsp = null;
    	if (since == null && until == null) {
    		since = OffsetDateTime.now();
    	}

    	PagedResult<Ride> rides = null;
		try {
			RideshareUser driver = null;
			if (driverId != null) {
				driver = userManager.resolveUrn(driverId)
						.orElseThrow(() -> new NotFoundException("No such user: " + driverId));
			} else {
				driver = userManager.findCallingUser();
			}
			if (driver.getId() != null) {
				RideFilter filter = new RideFilter(driver.getId(), since, until, state, bookingState, 
						UrnHelper.getId(Ride.URN_PREFIX, siblingRideId), sortDir, Boolean.TRUE.equals(deletedToo));
				Cursor cursor = new Cursor(maxResults, offset);
				rides = rideManager.listRides(filter, cursor);
			} else {
				rides = PagedResult.empty();
			}
			// Map the rides as my rides: Brand/model car only, no driver info (because it is the specified driver)
			rsp = Response.ok(pageMapper.mapMine(rides)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp; 
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
			RideshareUser driver = userManager.findOrRegisterCallingUser();
			ride.setDriver(driver);
			// The owner of the ride will be the caller
			String newRideId = UrnHelper.createUrn(Ride.URN_PREFIX, rideManager.createRide(ride));
			rsp = Response.created(URI.create(newRideId)).build();
		} catch (BusinessException e) {
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
        	Long cid = UrnHelper.getId(Ride.URN_PREFIX, rideId);
			RideshareUser caller = userManager.findCallingUser();
			ride = rideManager.getRide(cid);
			allowAdminOrCaller(request, caller, ride.getDriver());
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
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
    @Override
	public Response updateRide(String rideId, String scope, eu.netmobiel.rideshare.api.model.Ride ridedt) {
    	Response rsp = null;
    	try {
        	Long cid = UrnHelper.getId(Ride.URN_PREFIX, rideId);
			RideshareUser caller = userManager.findCallingUser();
			Ride rdb = rideManager.getRideWithDriver(cid);
			allowAdminOrCaller(request, caller, rdb.getDriver());
			Ride ride = mapper.map(ridedt);
			ride.setId(cid);
    		RideScope rs = scope == null ? RideScope.THIS: RideScope.lookup(scope);
			rideManager.updateRide(ride, rs);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
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
     * @param hard If set to true then remove the ride from the listing.
     * @return
     */
	@Override
	public Response deleteRide(String rideId, String scope, String reason, Boolean hard) {
    	Response rsp = null;
    	try {
    		RideScope rs = scope == null ? RideScope.THIS: RideScope.lookup(scope);
        	Long cid = UrnHelper.getId(Ride.URN_PREFIX, rideId);
			RideshareUser caller = userManager.findCallingUser();
			Ride rdb = rideManager.getRideWithDriver(cid);
			allowAdminOrCaller(request, caller, rdb.getDriver());
			rideManager.removeRide(cid, reason, rs, Boolean.TRUE.equals(hard));
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
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
			RideshareUser passenger = userManager.findOrRegisterCallingUser();
        	Booking booking = bookingMapper.map(bookingdt);
			String newBookingUrn = bookingManager.createBooking(rideId, passenger, booking);
			rsp = Response.created(URI.create(newBookingUrn)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }
    
}
