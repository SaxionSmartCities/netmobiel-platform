package eu.netmobiel.rideshare.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.rideshare.api.BookingsApi;
import eu.netmobiel.rideshare.api.mapping.BookingMapper;
import eu.netmobiel.rideshare.api.mapping.PageMapper;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@RequestScoped
public class BookingsResource implements BookingsApi {

	@Inject
	private BookingMapper mapper;

	@Inject
	private PageMapper pageMapper;

	@Inject
    private BookingManager bookingManager;

	private Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

	/**
     * Lists the bookings driven by the calling user.
     * @return an array of bookings.
     */
    public Response getBookings(OffsetDateTime sinceDate, OffsetDateTime untilDate, Integer maxResults, Integer offset) {
    	PagedResult<Booking> bookings;
    	Long userId = 0L;
		try {
			bookings = bookingManager.listBookings(userId, toInstant(sinceDate), toInstant(untilDate), maxResults, offset);
		} catch (BadRequestException | eu.netmobiel.commons.exception.NotFoundException e) {
			throw new WebApplicationException(e);
		} 
    	return Response.ok(pageMapper.mapMyBookings(bookings)).build();
    }

    public Response getBooking(String bookingId) {
    	Booking booking = null;
    	try {
        	Long cid = RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingId);
			booking = bookingManager.getBooking(cid);
		} catch (eu.netmobiel.commons.exception.NotFoundException e) {
			throw new NotFoundException();
		}
    	return Response.ok(mapper.mapInDetail(booking)).build();
    }

    public Response deleteBooking(String bookingId, String reason) {
    	Response rsp = null;
    	try {
        	Long cid = RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingId);
        	User initiator = null;
			bookingManager.removeBooking(initiator, cid, reason);
			rsp = Response.noContent().build();
		} catch (eu.netmobiel.commons.exception.NotFoundException e) {
	    	rsp = Response.status(Status.GONE).build();
		}
    	return rsp;
    }

}
