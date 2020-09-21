package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.planner.api.PlansApi;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.api.mapping.TripPlanMapper;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.planner.service.TripPlanManager;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@ApplicationScoped
public class PlansResource implements PlansApi {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;
 
    @Inject
    private TripPlanMapper tripPlanMapper;

    @Inject
    private PageMapper pageMapper;

    @Inject
    private TripPlanManager tripPlanManager;

	@Inject
    private PlannerUserManager userManager;

	private Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

	@Override
	public Response createPlan(eu.netmobiel.planner.api.model.TripPlan tripPlan) {
    	Response rsp = null;
		// The owner of the trip will be the calling user.
		try {
			PlannerUser traveller = userManager.registerCallingUser();
			TripPlan plan = tripPlanMapper.map(tripPlan);
			String newPlanId = PlannerUrnHelper.createUrn(TripPlan.URN_PREFIX, tripPlanManager.createTripPlan(traveller, plan, Instant.now()));
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newPlanId)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getPlan(String planId) {
    	Response rsp = null;
		TripPlan plan;
		try {
        	Long tid = PlannerUrnHelper.getId(TripPlan.URN_PREFIX, planId);
			plan = tripPlanManager.getTripPlan(tid);
			rsp = Response.ok(tripPlanMapper.map(plan)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response listPlans(String userRef, String planType, OffsetDateTime since, OffsetDateTime until, 
			Boolean inProgressOnly, String sortDir, Integer maxResults, Integer offset) {
	    	Response rsp = null;
			try {
				PlanType type = planType == null ? null : PlanType.valueOf(planType);
				SortDirection sortDirection = sortDir == null ? SortDirection.ASC : SortDirection.valueOf(sortDir);
		    	PlannerUser traveller = null;
		    	if (userRef == null) {
		    		traveller = userManager.findCallingUser();
		    	} else {
		    		traveller = userManager.resolveUrn(userRef).orElse(null);
		    	}
		    	
		    	PagedResult<TripPlan> results = null;
	        	// Only retrieve if a user exists in the trip service
		    	if (traveller != null && traveller.getId() != null) {
		    		results = tripPlanManager.listTripPlans(traveller, type, toInstant(since), toInstant(until), inProgressOnly, sortDirection, maxResults, offset);
		    	} else {
		    		results = PagedResult.<TripPlan>empty();
		    	}
				rsp = Response.ok(pageMapper.mapPlans(results)).build();
			} catch (IllegalArgumentException e) {
				throw new BadRequestException(e);
			} catch (BusinessException e) {
				throw new WebApplicationException(e);
			}
	    	return rsp;
	}

}
