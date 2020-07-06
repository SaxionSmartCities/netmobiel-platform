package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.planner.api.ShoutOutsApi;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.api.mapping.TripPlanMapper;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.service.TripPlanManager;
import eu.netmobiel.planner.service.UserManager;

@ApplicationScoped
public class ShoutOutsResource implements ShoutOutsApi {
	private static final Integer DEFAULT_DEP_ARR_RADIUS = 10000;
	
	@SuppressWarnings("unused")
	@Inject
    private Logger log;
 
    @Inject
    private PageMapper pageMapper;

    @Inject
    private TripPlanManager tripPlanManager;

    @EJB(name = "java:app/netmobiel-planner-ejb/UserManager")
    private UserManager userManager;

    @Inject
    private TripPlanMapper tripPlanMapper;

	@Override
    public Response listShoutOuts(String location, OffsetDateTime startTime, Integer depArrRadius, Integer travelRadius, Integer maxResults, Integer offset) { 
    	Response rsp = null;
    	if (location == null) {
    		throw new BadRequestException("Missing mandatory parameter: location");
    	}
		try {
			Integer smallRadius = depArrRadius != null ? depArrRadius : DEFAULT_DEP_ARR_RADIUS; 
			PagedResult<TripPlan> result = tripPlanManager.listShoutOuts(GeoLocation.fromString(location), 
					startTime != null ? startTime.toInstant() : Instant.now(), 
					smallRadius, travelRadius != null ? travelRadius : smallRadius, maxResults, offset);
			rsp = Response.ok(pageMapper.mapShoutOutPlans(result)).build();
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response resolveShoutOut(String planId, OffsetDateTime now, String from, String to, OffsetDateTime travelTime, Boolean useAsArrivalTime) {
    	Response rsp = null;
    	if (planId == null) {
    		throw new BadRequestException("Missing mandatory path parameter: planId");
    	}
    	if (from == null) {
    		throw new BadRequestException("Missing mandatory parameter: from");
     	}
    	if (now == null) {
    		now = OffsetDateTime.now();
     	}
		try {
			User driver = userManager.registerCallingUser();
			TripPlan driverPlan = new TripPlan();
			driverPlan.setFrom(GeoLocation.fromString(from));
			if (to != null) {
				driverPlan.setTo(GeoLocation.fromString(to));
			}
			driverPlan.setTravelTime(travelTime != null ? travelTime.toInstant() : null);
			driverPlan.setUseAsArrivalTime(Boolean.TRUE.equals(useAsArrivalTime));
			driverPlan = tripPlanManager.resolveShoutOut(now.toInstant(), driver, planId, driverPlan);
			rsp = Response.ok(tripPlanMapper.map(driverPlan)).build();
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}


}
