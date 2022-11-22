package eu.netmobiel.planner.api.resource;

import java.net.URI;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.RemoveException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.model.ConfirmationReasonType;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.api.TripsApi;
import eu.netmobiel.planner.api.mapping.LegMapper;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.api.mapping.TripMapper;
import eu.netmobiel.planner.api.model.Leg.ConfirmationReasonEnum;
import eu.netmobiel.planner.filter.TripFilter;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.planner.service.TripManager;

/**
 * Implementation for the /trips endpoint. The security has been placed in this handler. The service itself
 * does not impose restrictions.
 * 
 * The header parameter xDelegator is extracted by the generated Api, but remains unsued. The implementation uses a CDI method to 
 * produce and inject the security identity. 
 *
 * @author Jaap Reitsma
 *
 */
@RequestScoped
public class TripsResource extends PlannerResource implements TripsApi {

	@Inject
    private Logger log;
 
    @Inject
    private TripMapper tripMapper;

    @Inject
    private PageMapper pageMapper;

    
    @Inject
    private LegMapper legMapper;

    @Inject
    private TripManager tripManager;

	@Inject
    private PlannerUserManager userManager;

    @Inject
	private SecurityIdentity securityIdentity;

    @Context
    private HttpServletRequest request;
    
    @Override
	public Response createTrip(String xDelegator, eu.netmobiel.planner.api.model.Trip trip) {
    	Response rsp = null;
		// The owner of the trip, the traveller, will be the effective user.
		try {
			CallingContext<PlannerUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
    		PlannerUser traveller = context.getEffectiveUser();
    		PlannerUser organizer = context.getCallingUser();
			Trip dtrip = tripMapper.map(trip);
			Long newTripId = tripManager.createTrip(organizer, traveller, dtrip);
			String urn = UrnHelper.createUrn(Trip.URN_PREFIX, newTripId);
			rsp = Response.created(URI.create(urn)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getTrip(String xDelegator, String someId) {
    	Response rsp = null;
		try {
			Trip trip = null;
			if (! UrnHelper.isUrn(someId) || UrnHelper.matchesPrefix(Trip.URN_PREFIX, someId)) {
				// If not a urn, it will get the Long identifier
	        	Long tid = UrnHelper.getId(Trip.URN_PREFIX, someId);
				trip = tripManager.getTrip(tid);
			} else if (UrnHelper.matchesPrefix(Itinerary.URN_PREFIX, someId)) {
	        	Long iid = UrnHelper.getId(Itinerary.URN_PREFIX, someId);
				trip = tripManager.getTripByItinerary(iid);
			} else if (UrnHelper.matchesPrefix(Leg.URN_PREFIX, someId)) {
	        	Long lid = UrnHelper.getId(Leg.URN_PREFIX, someId);
				trip = tripManager.getTripByLeg(lid);
			} else {
				throw new BadRequestException("Don't understand urn: " + someId);
			}
			CallingContext<PlannerUser> context = userManager.findCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(request, context, trip.getTraveller());
			rsp = Response.ok(tripMapper.mapInDetail(trip)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response getTrips(String xDelegator, String userRef, String tripState, 
			OffsetDateTime since, OffsetDateTime until, Boolean deletedToo, Boolean skipCancelled,
			String sortDir, Integer maxResults, Integer offset) {
    	Response rsp = null;
		try {
			CallingContext<PlannerUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
    		PlannerUser traveller = null;
	    	if (userRef == null) {
	    		traveller = context.getEffectiveUser();
	    	} else {
	    		traveller = userManager.resolveUrn(userRef)
	    				.orElseThrow(() -> new IllegalStateException("Didn't expect user null from " + userRef));
	    	}
	    	allowAdminOrEffectiveUser(request, context, traveller);
	    	TripFilter filter = new TripFilter(traveller, since, until, tripState, deletedToo, skipCancelled, sortDir);
	    	Cursor cursor = new Cursor(maxResults, offset);
	    	PagedResult<Trip> results = tripManager.listTrips(filter, cursor);
			rsp = Response.ok(pageMapper.mapMine(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response confirmTrip(String xDelegator, String tripId, Boolean confirmationValue, String reason) {
    	Response rsp = null;
    	try {
        	Long tid = UrnHelper.getId(Trip.URN_PREFIX, tripId);
        	ConfirmationReasonEnum reasonEnum = reason == null ? null : 
        		ConfirmationReasonEnum.valueOf(reason);
        	ConfirmationReasonType reasonType = legMapper.map(reasonEnum); 
			Trip trip = tripManager.getTripBasics(tid);
			CallingContext<PlannerUser> context = userManager.findCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(request, context, trip.getTraveller());
			tripManager.confirmTrip(tid, confirmationValue, reasonType, true);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new javax.ws.rs.BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response settleDisputeInFavorOfProvider(String tripId) {
		log.info(String.format("Settle trip %s in favor of the provider", tripId));
		if (! request.isUserInRole("admin")) {
			return Response.status(Status.FORBIDDEN).build();
		}
    	Response rsp = null;
    	try {
        	Long tid = UrnHelper.getId(Trip.URN_PREFIX, tripId);
			tripManager.confirmTrip(tid, Boolean.TRUE, ConfirmationReasonType.DISPUTED, true);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new javax.ws.rs.BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response unconfirmTrip(String xDelegator, String tripId) {
    	Response rsp = null;
    	try {
        	Long tid = UrnHelper.getId(Trip.URN_PREFIX, tripId);
			Trip trip = tripManager.getTripBasics(tid);
			CallingContext<PlannerUser> context = userManager.findCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(request, context, trip.getTraveller());
			tripManager.unconfirmTrip(tid);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new javax.ws.rs.BadRequestException(e);
		} catch (RemoveException e) {
			throw new SecurityException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}


	@Override
	public Response settleDisputeInFavorOfTraveller(String tripId) {
		log.info(String.format("Settle trip %s in favor of the traveller", tripId));
		if (! request.isUserInRole("admin")) {
			return Response.status(Status.FORBIDDEN).build();
		}
    	Response rsp = null;
    	try {
			tripManager.afterConfirmTripByTransportProvider(tripId, null, Boolean.FALSE, ConfirmationReasonType.DISPUTED, true);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new javax.ws.rs.BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response deleteTrip(String xDelegator, String tripId, String reason, Boolean hard) {
    	Response rsp = null;
    	try {
        	Long tid = UrnHelper.getId(Trip.URN_PREFIX, tripId);
			Trip trip = tripManager.getTripBasics(tid);
			CallingContext<PlannerUser> context = userManager.findCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(request, context, trip.getTraveller());
			tripManager.removeTrip(tid, reason, Boolean.TRUE.equals(hard));
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new javax.ws.rs.BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response runTripStateMachine(String tripId) {
    	Response rsp = null;
    	try {
        	if (!request.isUserInRole("admin")) {
        		throw new SecurityException("You have no access rights");
        	}
        	Long tid = UrnHelper.getId(Trip.URN_PREFIX, tripId);
			tripManager.updateStateMachine(tid);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
