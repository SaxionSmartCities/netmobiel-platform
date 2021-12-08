package eu.netmobiel.rideshare.api.resource;

import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.api.BookingsApi;
import eu.netmobiel.rideshare.api.mapping.BookingMapper;
import eu.netmobiel.rideshare.api.mapping.PageMapper;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.service.RideshareUserManager;

@RequestScoped
public class BookingsResource extends RideshareResource implements BookingsApi {

	@Inject
	private BookingMapper mapper;

	@Inject
	private PageMapper pageMapper;

	@Inject
    private BookingManager bookingManager;

    @Inject
    private RideshareUserManager userManager;

	/**
     * Lists the bookings driven by the calling user.
     * @return an array of bookings.
     */
    @Override
	public Response getBookings(OffsetDateTime sinceDate, OffsetDateTime untilDate, Integer maxResults, Integer offset) {
    	if (sinceDate == null) {
    		sinceDate = OffsetDateTime.now();
    	}
    	PagedResult<Booking> bookings;
    	Long userId = userManager.findCallingUser().getId();
		try {
			if (userId == null) {
				bookings = PagedResult.empty();
			} else {
				bookings = bookingManager.listBookings(userId, toInstant(sinceDate), toInstant(untilDate), maxResults, offset);
			}
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		} 
    	return Response.ok(pageMapper.mapMyBookings(bookings)).build();
    }

    @Override
	public Response getBooking(String bookingId) {
    	Booking booking = null;
    	try {
        	Long cid = UrnHelper.getId(Booking.URN_PREFIX, bookingId);
			booking = bookingManager.getBooking(cid);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return Response.ok(mapper.mapInDetail(booking)).build();
    }

    @Override
	public Response deleteBooking(String bookingId, String reason) {
    	Response rsp = null;
    	try {
//        	Long cid = RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingId);
//        	User initiator = userManager.findCallingUser();
        	//FIXME who is calling? Can a driver call this interface?
        	boolean isDriver = false;
        	// FIXME add security
			bookingManager.removeBooking(bookingId, reason, isDriver, true);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }

}
