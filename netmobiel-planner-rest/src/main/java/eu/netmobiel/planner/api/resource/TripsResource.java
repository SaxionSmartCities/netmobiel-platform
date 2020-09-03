package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.planner.api.TripsApi;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.api.mapping.TripMapper;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.service.TripManager;
import eu.netmobiel.planner.service.UserManager;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@ApplicationScoped
public class TripsResource implements TripsApi {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;
 
    @Inject
    private TripMapper tripMapper;

    @Inject
    private PageMapper pageMapper;

    @EJB
    private TripManager tripManager;

    @EJB(name = "java:app/netmobiel-planner-ejb/UserManager")
    private UserManager userManager;

	private Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    @Override
	public Response createTrip(eu.netmobiel.planner.api.model.Trip trip) {
    	Response rsp = null;
		// The owner of the trip will be the calling user.
		try {
			User traveller = userManager.registerCallingUser();
			Trip dtrip = tripMapper.map(trip);
			String newTripId = PlannerUrnHelper.createUrn(Trip.URN_PREFIX, tripManager.createTrip(traveller, dtrip));
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newTripId)).build();
		} catch (ApplicationException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response deleteTrip(String tripId) {
    	Response rsp = null;
    	try {
        	Long tid = PlannerUrnHelper.getId(Trip.URN_PREFIX, tripId);
        	String reason = null;
			tripManager.removeTrip(tid, reason);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new javax.ws.rs.BadRequestException(e);
		} catch (NotFoundException e) {
	    	rsp = Response.status(Status.GONE).build();
		}
    	return rsp;
	}

	@Override
	public Response getTrip(String tripId) {
    	Response rsp = null;
		Trip trip;
		try {
        	Long tid = PlannerUrnHelper.getId(Trip.URN_PREFIX, tripId);
			trip = tripManager.getTrip(tid);
			rsp = Response.ok(tripMapper.mapInDetail(trip)).build();
		} catch (ApplicationException e) {
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
	    	User traveller = null;
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
		} catch (ApplicationException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
