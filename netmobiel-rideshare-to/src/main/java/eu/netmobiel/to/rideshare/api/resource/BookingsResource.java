package eu.netmobiel.to.rideshare.api.resource;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.service.RideshareUserManager;
import eu.netmobiel.to.rideshare.api.mapping.BookingMapper;
import eu.netmobiel.tomp.api.BookingsApi;
import eu.netmobiel.tomp.api.model.BookingOperation;
import eu.netmobiel.tomp.api.model.BookingRequest;
import eu.netmobiel.tomp.api.model.BookingState;
import eu.netmobiel.tomp.api.model.Notification;

@RequestScoped
public class BookingsResource extends TransportOperatorResource implements BookingsApi {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;
 
    @Inject
    protected RideshareUserManager rideshareUserManager;

    @Inject
    private BookingManager bookingManager;

    @Inject
    private BookingMapper bookingMapper;

    @Context 
	private HttpServletResponse response;
	
    /**
     * Creates a new `Booking` for the TO in **Pending** state. The ID of the posted booking should be the ID provided in the 
     * previous step (planning). The Booking may be modified in the response, e.g. location being adjusted for a more suitable 
     * pick-up location. In addition, the service may contain a **meta** attribute for arbitrary TO metadata that the TO needs 
     * later, and **token** attribute depicting how long the current state is valid. 
     * @see (3.2) in the process flow - booking. 
     * <p>The MP can implement this endpoint when it allows direct booking by TOs. The specific TO can book an asset from themselves 
     * to get it registrated and handled (financially) by the MP. 
     * 
     * @param body the booking request. The current implementation expects an ID only.  
     * @param acceptLanguage A list of the languages/localizations the user would like to see the results in. For user privacy and ease of use 
     * 				on the TO side, this list should be kept as short as possible, ideally just one language tag from the list in operator/information
     * @param api API description, can be TOMP or maybe other (specific/derived) API definitions
     * @param apiVersion Version of the API.
     * @param maasId The ID of the sending maas operator
     * @param addressedTo The ID of the maas operator that has to receive this message
     * @returns a quick overview of possible bookings
     */
	@Override
	public eu.netmobiel.tomp.api.model.Booking bookingsPost(@Valid BookingRequest body, @NotNull String acceptLanguage,
			@NotNull String api, @NotNull String apiVersion, @NotNull String maasId, String addressedTo) {
		// Add the traveller and set the booking in pending state.
		if (body.getCustomer() == null || body.getCustomer().getKnownIdentifier() == null) {
			throw new BadRequestException("Traveller/Customer is mandatory when creating a booking");
		}
		NetMobielUserImpl customer = new NetMobielUserImpl(body.getCustomer().getKnownIdentifier(), 
				body.getCustomer().getFirstName(), 
				body.getCustomer().getLastName(), 
				body.getCustomer().getEmail());
		GeoLocation from = null;
		if (body.getFrom() != null) {
			from = PlaceHelper.createGeolocation (body.getFrom());
		}
		GeoLocation to = null;
		if (body.getTo() != null) {
			to = PlaceHelper.createGeolocation (body.getTo());
		}
		Booking b = new Booking();
		b.setPickup(from);
		b.setDropOff(to);
		try {
			Booking bdb = bookingManager.createTompBooking(body.getId(), customer, b);
			return bookingMapper.map(bdb);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
	}

	@Override
	public List<eu.netmobiel.tomp.api.model.Booking> bookingsGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, @NotNull BookingState state, String addressedTo) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	
    /**
     * This endpoint **must** be used to alter the state of a booking:
     * <br>- The operation 'CANCEL' Cancels the booking (see <4> in the process flow - booking), 
     * <br>- the operation 'EXPIRE' informs that the booking-option is expired (see <5> in the process flow - booking) and 
     * <br>- the 'COMMIT' actually makes this booking option a real confirmed booking. (see also (3.2) in process flow - booking). 
     * This event should also be used to commit in the 'postponed-commit' scenario.
     * <br> - 'DENY' tells the MP that the leg is cancelled in the post-commit scenario. 
     * <p> `CANCEL` - Cancels a confirmed booking. Cancelling twice should still return 204. 
     * <br> `EXPIRE` - Typically for sending back a signal from TO to MP to tell the pending state is expired. 
     * Expiring twice should return 204. Expiring a booking in a non-pending state will result in 403. 
     * <br> `COMMIT` - Turns the booking in a confirmed state, after all legs are in state pending. 
     * Committing twice will result in 204. If the booking is in state CANCELLED or EXPIRED, a commit will result a 403. 
     * <br> `DENY` - Used for the 'postponed-commit' scenario. Whenever a TO cannot give guarantees directly to fulfil a booking, 
     * it can return a 'COMMIT', but the state of the booking object should be 'POSTPONED-COMMIT'. 
     * In the conditions returned in the planning phase is stated until when this phase can be. 
     * After this time it will become expired. Otherwise, it can be committed when the leg is confirmed or denied (using this event).
     * 
     * @param acceptLanguage A list of the languages/localizations the user would like to see the results in. For user privacy and ease of use 
     * 				on the TO side, this list should be kept as short as possible, ideally just one language tag from the list in operator/information
     * @param api API description, can be TOMP or maybe other (specific/derived) API definitions
     * @param apiVersion Version of the API.
     * @param maasId The ID of the sending maas operator
     * @param id The booking ID
     * @param body the operation to execute
     * @param addressedTo The ID of the maas operator that has to receive this message
     * @returns a quick overview of possible bookings
     */
	@Override
	public eu.netmobiel.tomp.api.model.Booking bookingsIdEventsPost(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String id, @Valid BookingOperation body,
			String addressedTo) {
		try {
			if (body.getOperation() == BookingOperation.OperationEnum.COMMIT) {
				Booking bdb = bookingManager.commitTompBooking(id);
				return bookingMapper.map(bdb);
			} else if (body.getOperation() == BookingOperation.OperationEnum.CANCEL) {
				Booking bdb = bookingManager.cancelTompBooking(id);
				return bookingMapper.map(bdb);
			}
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return null;
	}

    /**
     * Returns the booking. See (3.5.2) in the process flow - booking. In the 'meta'-field the digital tickes can be returned 
     * (see (3.3) in the process flow - booking).
     * 
     * @param acceptLanguage A list of the languages/localizations the user would like to see the results in. For user privacy and ease of use 
     * 				on the TO side, this list should be kept as short as possible, ideally just one language tag from the list in operator/information
     * @param api API description, can be TOMP or maybe other (specific/derived) API definitions
     * @param apiVersion Version of the API.
     * @param maasId The ID of the sending maas operator
     * @param id The booking ID
     * @param addressedTo The ID of the maas operator that has to receive this message
     * @returns a quick overview of possible bookings
     */
	@Override
	public eu.netmobiel.tomp.api.model.Booking bookingsIdGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String id, String addressedTo) {
		try {
			Booking bdb = bookingManager.getShallowBooking(id);
			return bookingMapper.map(bdb);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
	}

	@Override
	public List<Notification> bookingsIdNotificationsGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String id, String addressedTo) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void bookingsIdNotificationsPost(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String id, @Valid Notification body,
			String addressedTo) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public eu.netmobiel.tomp.api.model.Booking bookingsIdPut(eu.netmobiel.tomp.api.model.@Valid Booking body,
			@NotNull String acceptLanguage, @NotNull String api, @NotNull String apiVersion, @NotNull String maasId,
			String id, String addressedTo) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void bookingsIdSubscriptionDelete(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String id, String addressedTo) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void bookingsIdSubscriptionPost(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String id, String addressedTo) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
