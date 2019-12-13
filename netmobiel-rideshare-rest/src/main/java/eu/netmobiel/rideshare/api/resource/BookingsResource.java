package eu.netmobiel.rideshare.api.resource;

import java.util.stream.Collectors;

import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.github.dozermapper.core.Mapper;

import eu.netmobiel.rideshare.api.BookingsApi;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@ApplicationScoped
public class BookingsResource implements BookingsApi {

	@Inject
	private Mapper mapper;

    @Inject
    private BookingManager bookingManager;

    /**
     * Lists the bookings driven by the calling user.
     * @return an array of bookings.
     */
    public Response getBookings() {
    	return Response.ok(bookingManager.listMyBookings().stream()
    			.map(u -> mapper.map(u,  eu.netmobiel.rideshare.api.model.Booking.class, "passenger-view"))
    			.collect(Collectors.toList()))
    			.build();
    }

    public Response getBooking(@PathParam("bookingId") String bookingId) {
    	Booking booking = null;
    	try {
        	Long cid = RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingId);
			booking = bookingManager.getBooking(cid);
		} catch (ObjectNotFoundException e) {
			throw new NotFoundException();
		}
    	return Response.ok(mapper.map(booking,  eu.netmobiel.rideshare.api.model.Booking.class, "passenger-view")).build();
    }

    public Response deleteBooking(@PathParam("bookingId") String bookingId, @QueryParam("reason") String reason) {
    	Response rsp = null;
    	try {
        	Long cid = RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingId);
			bookingManager.removeBooking(cid, reason);
			rsp = Response.noContent().build();
		} catch (ObjectNotFoundException e) {
	    	rsp = Response.status(Status.GONE).build();
		}
    	return rsp;
    }

}
