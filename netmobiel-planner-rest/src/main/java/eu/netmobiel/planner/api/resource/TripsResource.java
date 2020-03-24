package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.planner.api.TripsApi;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.api.mapping.TripMapper;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.service.TripManager;
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

    @Override
	public Response createTrip(eu.netmobiel.planner.api.model.Trip trip) {
    	Response rsp = null;
		try {
			Trip dtrip = tripMapper.map(trip);
			String newTripId = PlannerUrnHelper.createUrn(Trip.URN_PREFIX, tripManager.createTrip(dtrip, true));
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
			tripManager.removeTrip(tid);
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
	public Response getTrips(String userRef, OffsetDateTime since, OffsetDateTime until, Boolean deletedToo, Integer maxResults, Integer offset) {
    	Response rsp = null;
		try {
			PagedResult<Trip> result = tripManager.listTrips(userRef, since != null ? since.toInstant() : Instant.now(), 
					until != null ? until.toInstant() : null, deletedToo, maxResults, offset);
			rsp = Response.ok(pageMapper.mapMine(result)).build();
		} catch (ApplicationException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response updateTrip(String tripId, eu.netmobiel.planner.api.model.Trip trip) {
		throw new UnsupportedOperationException("Not implemented");
	}

}
