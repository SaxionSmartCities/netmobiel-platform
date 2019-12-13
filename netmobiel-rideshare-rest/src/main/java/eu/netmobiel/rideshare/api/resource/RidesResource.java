package eu.netmobiel.rideshare.api.resource;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import com.github.dozermapper.core.Mapper;

import eu.netmobiel.rideshare.api.RidesApi;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideScope;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.service.RideManager;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@ApplicationScoped
public class RidesResource implements RidesApi {

    @Inject
    private RideManager rideManager;

    @Inject
    private BookingManager bookingManager;

    @Inject
    private Mapper mapper;
    /**
     * List all rides owned by the calling user. Soft deleted rides are omitted.
     * @return A list of rides owned by the calling user.
     */
    public Response listRides(String driverId, LocalDate sinceDate, LocalDate untilDate) {
//    	LocalDate sinceDate = since != null ? LocalDate.parse(since) : null;
//    	LocalDate untilDate =  until != null ? LocalDate.parse(until) : null;
    	List<Ride> rides;
		try {
			Long did = null;
			if (driverId != null) {
				did = RideshareUrnHelper.getId(User.URN_PREFIX, driverId);
			}
			rides = rideManager.listRides(did, sinceDate, untilDate);
		} catch (FinderException e) {
			throw new BadRequestException("Error finding rides", e);
		}
    	return Response.ok(rides.stream()
    			.map(r -> mapper.map(r, eu.netmobiel.rideshare.api.model.Ride.class, "my-details"))
    			.collect(Collectors.toList())).build();
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
			Ride ride = mapper.map(ridedt, Ride.class, "default");
			String newRideId = RideshareUrnHelper.createUrn(Ride.URN_PREFIX, rideManager.createRide(ride));
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newRideId)).build();
		} catch (CreateException e) {
			throw new BadRequestException("Error creating ride", e);
		} catch (ObjectNotFoundException e) {
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
		} catch (ObjectNotFoundException e) {
			throw new NotFoundException();
		}
    	return Response.ok(mapper.map(ride, eu.netmobiel.rideshare.api.model.Ride.class, "detail")).build();
    }

    /**
     * Updates a ride. A ride with a booking cannot be updated.
     * @param rideId The ride id.  Both the primary key (a number) as the urn format are accepted.
     * @param scope The scope of the delete action in case of a recurrent ride: Only this one or this one and all following.
     * 		If not set then the scope is set to THIS.
     * @param ridedt The new value of the ride. 
     * @return
     */
    public Response updateRide(String rideId, String scope, eu.netmobiel.rideshare.api.model.Ride ridedt) {
    	throw new UnsupportedOperationException("updateRide");
//    	Response rsp = null;
//    	try {
//			Ride ride = mapper.map(ridedt, Ride.class, "default");
//        	Long cid = UrnHelper.getId(Ride.URN_PREFIX, rideId);
//			rideManager.updateRide(cid, ride);
//			rsp = Response.noContent().build();
//		} catch (ObjectNotFoundException e) {
//			rsp = Response.status(Status.NOT_FOUND).build();
//		}
//    	return rsp;
    }

    /**
     * Deletes a ride. If a ride is already booked then the ride is soft deleted. Soft deleted rides are 
     * default not listed and can never be found.
     * @param rideId The ride id.  Both the primary key (a number) as the urn format are accepted.
     * @param reason The (optional) reason why the ride was cancelled.
     * @param scope The scope of the delete action in case of a recurrent ride: Only this one or this one and all following.
     * 		If not set then the scope is set to THIS.
     * @return
     */
    @Override
	public Response deleteRide(String rideId, String reason, String scope) {
    	Response rsp = null;
    	try {
    		RideScope rs = scope == null ? RideScope.THIS : Stream.of(RideScope.values())
    		          .filter(c -> c.getCode().equals(scope))
    		          .findFirst()
    		          .orElseThrow(() -> new IllegalArgumentException("No such scope: " + scope));
        	Long cid = RideshareUrnHelper.getId(Ride.URN_PREFIX, rideId);
			rideManager.removeRide(cid, reason, rs);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (ObjectNotFoundException e) {
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
        	Long rid = RideshareUrnHelper.getId(Ride.URN_PREFIX, rideId);
        	Booking booking = mapper.map(bookingdt, Booking.class, "default");
			String newBookingId = RideshareUrnHelper.createUrn(Booking.URN_PREFIX, bookingManager.createBooking(rid, booking));
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newBookingId)).build();
		} catch (CreateException e) {
			throw new BadRequestException("Error creating booking for ride " + rideId, e);
		} catch (ObjectNotFoundException e) {
			throw new BadRequestException("Error creating booking for ride " + rideId, e);
		}
    	return rsp;
    }
    
}
