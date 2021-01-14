package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.api.TripsApi;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.api.mapping.TripMapper;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.planner.service.TripManager;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@RequestScoped
public class TripsResource implements TripsApi {

	@Inject
    private Logger log;
 
    @Inject
    private TripMapper tripMapper;

    @Inject
    private PageMapper pageMapper;

    @Inject
    private TripManager tripManager;

	@Inject
    private PlannerUserManager userManager;

    @Context
    private HttpServletRequest request;
    
	private Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    @Override
	public Response createTrip(eu.netmobiel.planner.api.model.Trip trip) {
    	Response rsp = null;
		// The owner of the trip will be the calling user.
		try {
			PlannerUser traveller = userManager.registerCallingUser();
			Trip dtrip = tripMapper.map(trip);
			String newTripId = PlannerUrnHelper.createUrn(Trip.URN_PREFIX, tripManager.createTrip(traveller, dtrip));
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newTripId)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response deleteTrip(String tripId, String reason) {
    	Response rsp = null;
    	try {
        	Long tid = UrnHelper.getId(Trip.URN_PREFIX, tripId);
			tripManager.removeTrip(tid, reason);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new javax.ws.rs.BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response getTrip(String someId) {
    	Response rsp = null;
		Trip trip;
		try {
			if (! UrnHelper.isUrn(someId) || UrnHelper.matchesPrefix(Trip.URN_PREFIX, someId)) {
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
			rsp = Response.ok(tripMapper.mapInDetail(trip)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response getTrips(String userRef, String tripState, OffsetDateTime since, OffsetDateTime until, Boolean deletedToo, String sortDir, Integer maxResults, Integer offset) {
    	Response rsp = null;
		try {
			TripState state = tripState == null ? null : TripState.valueOf(tripState);
			SortDirection sortDirection = sortDir == null ? SortDirection.ASC : SortDirection.valueOf(sortDir);
	    	PlannerUser traveller = null;
	    	if (userRef == null) {
	    		traveller = userManager.findCallingUser();
	    	} else {
	    		traveller = userManager.resolveUrn(userRef).orElse(null);
	    	}
	    	
	    	PagedResult<Trip> results = null;
        	// Only retrieve if a user exists in the trip service
	    	if (traveller != null && traveller.getId() != null) {
	    		results = tripManager.listTrips(traveller, state, toInstant(since), toInstant(until), deletedToo, sortDirection, maxResults, offset);
	    	} else {
	    		results = PagedResult.<Trip>empty();
	    	}
			rsp = Response.ok(pageMapper.mapMine(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response confirmTrip(String tripId, Boolean confirmationValue) {
    	Response rsp = null;
    	try {
        	Long tid = UrnHelper.getId(Trip.URN_PREFIX, tripId);
        	//TODO Add security restriction
			tripManager.confirmTrip(tid, confirmationValue, false);
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
			tripManager.confirmTrip(tid, Boolean.TRUE, true);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new javax.ws.rs.BadRequestException(e);
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
			tripManager.confirmTripByTransportProvider(tripId, null, Boolean.FALSE, true);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new javax.ws.rs.BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
