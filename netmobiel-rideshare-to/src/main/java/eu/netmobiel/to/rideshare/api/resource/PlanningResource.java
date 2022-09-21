package eu.netmobiel.to.rideshare.api.resource;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.service.RideManager;
import eu.netmobiel.rideshare.service.RideshareTompService;
import eu.netmobiel.to.rideshare.api.mapping.BookingMapper;
import eu.netmobiel.tomp.api.PlanningApi;
import eu.netmobiel.tomp.api.model.Planning;
import eu.netmobiel.tomp.api.model.PlanningRequest;

@RequestScoped
public class PlanningResource extends TransportOperatorResource implements PlanningApi {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;
 
    @Inject
    private BookingMapper bookingMapper;

    @Inject
    private RideshareTompService tompService;

	@Context 
	private HttpServletResponse response;
	
    /**
     * Returns informative options for the given travel plan. <p>Start time can be defined, but is optional. If startTime is not provided, 
     * but required by the third party API, a default value of \"Date.now()\" is used. [from MaaS-API /listing]. During the routing phase 
     * this service can be used to check availability without any state changes. 
     * <p>In the final check, just before presenting the alternatives to the user, a call should be made using `booking-intent`, requesting 
     * the TO to provide booking IDs to reference to during communication with the MP.
     * @see (2.1) in the process flow - planning.
     * 
     * @param acceptLanguage A list of the languages/localizations the user would like to see the results in. For user privacy and ease of use 
     * 				on the TO side, this list should be kept as short as possible, ideally just one language tag from the list in operator/information
     * @param api API description, can be TOMP or maybe other (specific/derived) API definitions
     * @param apiVersion Version of the API.
     * @param maasId The ID of the sending maas operator
     * @param body the actal planning request.
     * @param addressedTo The ID of the maas operator that has to receive this message
     * @returns a quick overview of possible bookings
     */
	@Override
	public Planning planningInquiriesPost(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, @Valid PlanningRequest body, String addressedTo) {
		return createPlanning(body, false);
	}

    /**
     * Returns bookable offers for the given travel plan. 
     * <p>Start time can be defined, but is optional. If startTime is not provided, but required by the third party API, a default value of \"Date.now()\" is used. 
     * [from MaaS-API /listing]. During the routing phase this service can be used to check availability without any state changes. <p>In the final check, 
     * just before presenting the alternatives to the user, a call should be made using `booking-intent`, requesting the TO to provide booking IDs to 
     * reference to during communication with the MP. 
     * @see (2.1) in the process flow - planning
     * 
     * @param acceptLanguage A list of the languages/localizations the user would like to see the results in. For user privacy and ease of use 
     * 				on the TO side, this list should be kept as short as possible, ideally just one language tag from the list in operator/information
     * @param api API description, can be TOMP or maybe other (specific/derived) API definitions
     * @param apiVersion Version of the API.
     * @param maasId The ID of the sending maas operator
     * @param body the actal planning request.
     * @param addressedTo The ID of the maas operator that has to receive this message
     * @returns A planning with persistent booking id.
     *  
     */
	@Override
	public Planning planningOffersPost(@NotNull String acceptLanguage, @NotNull String api, @NotNull String apiVersion,
			@NotNull String maasId, @Valid PlanningRequest body, String addressedTo) {
		return createPlanning(body, true);
	}

	public Planning createPlanning(PlanningRequest body, boolean makeItAnOffer) {
		String travellerId = null;
		// The traveller is during planning used the exclude the traveller's own rides (in case the traveller is a driver too). 
		if (body.getTravelers() != null && body.getTravelers().size() > 0) {
			travellerId = body.getTravelers().get(0).getKnownIdentifier();
		} else {
			travellerId = getCaller();
		}
		OffsetDateTime earliestDepTime = body.getDepartureTime();
		OffsetDateTime latestArrTime = body.getArrivalTime();
		if (earliestDepTime == null && latestArrTime == null) {
			earliestDepTime = OffsetDateTime.now();
		}
		// Estimated distance is also a field. It is used to get an asset with enough fuel. Not used with rideshare.
		GeoLocation from = PlaceHelper.createGeolocation (body.getFrom());
		GeoLocation to = PlaceHelper.createGeolocation (body.getTo());
		int nrSeats = body.getNrOfTravelers() != null ? body.getNrOfTravelers() : 1;
		
    	if (from == null || to == null) {
    		throw new BadRequestException("Missing one or more mandatory parameters: from, to");
    	}
		Planning planning = new Planning();
		planning.setValidUntil(OffsetDateTime.now().plusMinutes(15));
		try {
			List<Booking> bookings = tompService.searchTompRides(travellerId, from, to, 
					toInstant(earliestDepTime), toInstant(latestArrTime), body.getRadius(), nrSeats, makeItAnOffer, RideManager.MAX_RESULTS); 
			planning.setOptions(bookingMapper.mapBookings(bookings));
			response.setStatus(HttpServletResponse.SC_CREATED);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		} catch (DateTimeParseException ex) {
			throw new BadRequestException("Date parameter has unrecognized format", ex);
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException("Input parameter has unrecognized format", ex);
		}
    	return planning; 
	}

}
