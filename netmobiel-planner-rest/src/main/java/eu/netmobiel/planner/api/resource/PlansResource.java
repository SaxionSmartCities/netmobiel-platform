package eu.netmobiel.planner.api.resource;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.api.PlansApi;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.api.mapping.TripPlanMapper;
import eu.netmobiel.planner.filter.TripPlanFilter;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.planner.service.TripPlanManager;

/**
 * Implementation for the /plans endpoint. The security has been placed in this handler. The service
 * does not impose restrictions.
 * 
 * The header parameter xDelegator is extracted by the generated Api, but remains unsued. The implementation uses a CDI method to 
 * produce and inject the security identity. 
 *  
 * @author Jaap Reitsma
 *
 */
@RequestScoped
public class PlansResource extends PlannerResource implements PlansApi {

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

    @Inject
	private SecurityIdentity securityIdentity;

    @Context
	private HttpServletRequest request;

	@Override
	public Response createPlan(String xDelegator, eu.netmobiel.planner.api.model.TripPlan tripPlan) {
    	Response rsp = null;
		// The owner of the trip will be the effective user.
		try {
			CallingContext<PlannerUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
    		PlannerUser traveller = context.getEffectiveUser();
			TripPlan plan = tripPlanMapper.map(tripPlan);
			Long newPlanId = tripPlanManager.createTripPlan(context.getCallingUser(), traveller, plan, Instant.now());
			String urn = UrnHelper.createUrn(TripPlan.URN_PREFIX, newPlanId);
			rsp = Response.created(URI.create(urn)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getPlan(String xDelegator, String planId) {
    	Response rsp = null;
		try {
        	Long tid = UrnHelper.getId(TripPlan.URN_PREFIX, planId);
			CallingContext<PlannerUser> context = userManager.findCallingContext(securityIdentity);
        	TripPlan plan = tripPlanManager.getTripPlan(tid);
        	allowAdminOrEffectiveUser(request, context, plan.getTraveller());
			rsp = Response.ok(tripPlanMapper.map(plan)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response listPlans(String xDelegator, String userRef, String planType, OffsetDateTime since, OffsetDateTime until, 
			Boolean inProgress, String sortDir, Integer maxResults, Integer offset) {
	    	Response rsp = null;
			try {
				CallingContext<PlannerUser> context = userManager.findCallingContext(securityIdentity);
		    	PlannerUser traveller = null;
		    	if (userRef == null) {
		    		traveller = context.getEffectiveUser();
		    	} else {
		    		traveller = userManager.resolveUrn(userRef)
		    				.orElseThrow(() -> new IllegalStateException("Didn't expect user null from " + userRef));
		    	}
		    	TripPlanFilter filter = new TripPlanFilter(traveller, since, until, planType, inProgress, sortDir); 
				Cursor cursor = new Cursor(maxResults, offset);
		    	allowAdminOrEffectiveUser(request, context, traveller);
		    	PagedResult<TripPlan> results = null;
	        	// Only retrieve if a user exists in the trip service
		    	if (traveller != null && traveller.getId() != null) {
		    		results = tripPlanManager.listTripPlans(filter, cursor);
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

	/**
	 * Cancels a shout-out plan. Only an open shout-out can be cancelled. 
	 * An already closed shout-out is ignored. It is an error to close a non-shout-out plan (not found status).
	 * Only the admin or the effective owner can close a shout-out.
	 */
	@Override
	public Response cancelPlan(String xDelegator, String shoutOutPlanId, String reason) {
    	Response rsp = null;
		try {
        	Long tid = UrnHelper.getId(TripPlan.URN_PREFIX, shoutOutPlanId);
        	// Check whether this caller is allowed to cancel the shout-out.
        	TripPlan plan = tripPlanManager.getShoutOutPlan(tid);
			CallingContext<PlannerUser> context = userManager.findCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(request, context, plan.getTraveller());
			tripPlanManager.cancelShoutOut(tid, reason);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
