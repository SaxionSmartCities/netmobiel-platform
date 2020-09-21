package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.planner.api.ShoutOutsApi;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.api.mapping.TripPlanMapper;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.planner.service.TripPlanManager;
import eu.netmobiel.planner.util.PlannerUrnHelper;

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

	@Inject
    private PlannerUserManager userManager;

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
	public Response resolveShoutOut(String planId, OffsetDateTime now, String from, String to, OffsetDateTime travelTime, 
			Boolean useAsArrivalTime, String modality, String agencyId) {
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
    	TraverseMode mode = TraverseMode.RIDESHARE;  
    	if (modality != null) {
        	try {
    	    	mode = TraverseMode.valueOf(modality);
        	} catch (IllegalArgumentException ex) {
        		throw new BadRequestException("Failed to parse modality: " + modality, ex);
        	}
    	}
		try {
			PlannerUser driver = userManager.registerCallingUser();
			TripPlan driverPlan = new TripPlan();
			driverPlan.setFrom(GeoLocation.fromString(from));
			if (to != null) {
				driverPlan.setTo(GeoLocation.fromString(to));
			}
			driverPlan.setTravelTime(travelTime != null ? travelTime.toInstant() : null);
			driverPlan.setUseAsArrivalTime(Boolean.TRUE.equals(useAsArrivalTime));
			driverPlan = tripPlanManager.resolveShoutOut(now.toInstant(), driver, planId, driverPlan, mode);
			rsp = Response.ok(tripPlanMapper.map(driverPlan)).build();
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response addSolution(String shoutOutPlanId, eu.netmobiel.planner.api.model.TravelOffer travelOffer) {
    	Response rsp = null;
		try {
        	Long shoutOutId = PlannerUrnHelper.getId(TripPlan.URN_PREFIX, shoutOutPlanId);
        	if (travelOffer == null || travelOffer.getPlanRef() == null) {
        		throw new eu.netmobiel.commons.exception.BadRequestException("planRef is a mandatory attribute");
        	}
        	Long providedSolutionPlanId = PlannerUrnHelper.getId(TripPlan.URN_PREFIX, travelOffer.getPlanRef());
			PlannerUser driver = userManager.registerCallingUser();
			if (travelOffer.getDriverRef() != null) {
				if (!driver.getReference().equals(travelOffer.getDriverRef()) && !driver.getKeyCloakUrn().equals(travelOffer.getDriverRef())) {
					throw new SecurityException(String.format("User %s is not allowed to offer rides on behalf of %s", driver.getManagedIdentity(), travelOffer.getDriverRef()));
				}
			}
			tripPlanManager.addShoutOutSolution(shoutOutId, providedSolutionPlanId, driver.getKeyCloakUrn(), travelOffer.getVehicleRef());
			rsp = Response.accepted().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response getShoutOut(String shoutOutPlanId) {
    	Response rsp = null;
		TripPlan plan;
		try {
        	Long tid = PlannerUrnHelper.getId(TripPlan.URN_PREFIX, shoutOutPlanId);
			plan = tripPlanManager.getShoutOutPlan(tid);
			rsp = Response.ok(tripPlanMapper.map(plan)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response cancelPlan(String shoutOutPlanId) {
    	Response rsp = null;
		try {
        	Long tid = PlannerUrnHelper.getId(TripPlan.URN_PREFIX, shoutOutPlanId);
			tripPlanManager.cancelShoutOut(tid);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}


}
