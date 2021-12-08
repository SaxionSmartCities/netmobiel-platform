package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.api.ShoutOutsApi;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.api.mapping.TripPlanMapper;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.planner.service.TripPlanManager;

/**
 * Implementation for the /shout-outs endpoint. The security has been placed in this handler. The service
 * does not impose restrictions.
 * 
 * @author Jaap Reitsma
 *
 */
@RequestScoped
public class ShoutOutsResource extends PlannerResource implements ShoutOutsApi {
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

    @Inject
	private SecurityIdentity securityIdentity;

    /**
     * Searches for matching shout-outs. This call is for the driver to check whether there potentials rides requested.
     * Any driver (in fact anyone) can call this method. 
     */
	@Override
    public Response listShoutOuts(String location, OffsetDateTime startTime, Integer depArrRadius, 
    		Integer travelRadius, Integer maxResults, Integer offset) { 
    	Response rsp = null;
    	if (location == null) {
    		throw new BadRequestException("Missing mandatory parameter: location");
    	}
		try {
			Integer smallRadius = depArrRadius != null ? depArrRadius : DEFAULT_DEP_ARR_RADIUS;
			PlannerUser caller = userManager.findOrRegisterCallingUser();
			PagedResult<TripPlan> result = tripPlanManager.findShoutOuts(caller, GeoLocation.fromString(location), 
					startTime != null ? startTime.toInstant() : Instant.now(), 
					smallRadius, travelRadius != null ? travelRadius : smallRadius, maxResults, offset);
			rsp = Response.ok(pageMapper.mapShoutOutPlans(result)).build();
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	/**
	 * Creates a TripPlan given a specific shout-out with the criteria given by the driver. The modality and the agency are 
	 * added to force the selection of a specific transport. This is tested only for RideShare.
     * Any driver (in fact anyone) can call this method. 
	 */
	@Override
	public Response planShoutOutSolution(String shoutOutPlanId, OffsetDateTime now, String from, String to, OffsetDateTime travelTime, 
			Boolean useAsArrivalTime, String modality, String agencyId) {
    	Response rsp = null;
    	if (shoutOutPlanId == null) {
    		throw new BadRequestException("Missing mandatory path parameter: planId");
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
			// No delegation for drivers
			PlannerUser driver = userManager.findOrRegisterCallingUser(securityIdentity);
			TripPlan driverPlan = new TripPlan();
			if (from != null) {
				driverPlan.setFrom(GeoLocation.fromString(from));
			}
			if (to != null) {
				driverPlan.setTo(GeoLocation.fromString(to));
			}
			if (travelTime != null) {
				driverPlan.setTravelTime(travelTime.toInstant());
				driverPlan.setUseAsArrivalTime(Boolean.TRUE.equals(useAsArrivalTime));
			}
			driverPlan = tripPlanManager.planShoutOutSolution(now.toInstant(), driver, shoutOutPlanId, driverPlan, mode);
			rsp = Response.ok(tripPlanMapper.map(driverPlan)).build();
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	/**
	 * Adds an itinerary (travel offer) to a shout-out of a traveller. The travel offer must be created by the caller of this method.
	 * Only drivers should use this method. This is not verified in this method.
	 */
	@Override
	public Response addSolution(String shoutOutPlanId, eu.netmobiel.planner.api.model.TravelOffer travelOffer) {
    	Response rsp = null;
		try {
        	Long shoutOutId = UrnHelper.getId(TripPlan.URN_PREFIX, shoutOutPlanId);
        	if (travelOffer == null || travelOffer.getPlanRef() == null) {
        		throw new eu.netmobiel.commons.exception.BadRequestException("planRef is a mandatory attribute");
        	}
        	Long providedSolutionPlanId = UrnHelper.getId(TripPlan.URN_PREFIX, travelOffer.getPlanRef());
			PlannerUser driver = userManager.findOrRegisterCallingUser(securityIdentity);
			if (travelOffer.getDriverRef() != null) {
				if (!driver.getUrn().equals(travelOffer.getDriverRef()) && !driver.getKeyCloakUrn().equals(travelOffer.getDriverRef())) {
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

	/**
	 * Retrieves a shout-out. Anyone can retrieve a shout-out. Only the plan itself is retrieved, not the itineraries.
	 * The exception is the caller that is the driver in a leg. These itineraries are added. 
	 * The driver can see his ride in the RideShare by requesting the proposed bookings.
	 * @param shoutOutPlanId The id or urn of the shout-out plan to lookup.
	 * @return The shout-out plan object.
	 */
	@Override
	public Response getShoutOut(String shoutOutPlanId) {
    	Response rsp = null;
		TripPlan plan;
		try {
			PlannerUser driver = userManager.findOrRegisterCallingUser(securityIdentity);
        	Long tid = UrnHelper.getId(TripPlan.URN_PREFIX, shoutOutPlanId);
			plan = tripPlanManager.getShoutOutPlan(tid);
			// Remove the itineraries not driven by the caller
			plan.getItineraries().removeIf(it -> it.findLegByDriverId(driver.getManagedIdentity()).isEmpty());
			rsp = Response.ok(tripPlanMapper.mapShoutOut(plan)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
