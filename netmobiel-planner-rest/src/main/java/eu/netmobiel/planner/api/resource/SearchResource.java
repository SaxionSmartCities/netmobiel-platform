package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.api.SearchApi;
import eu.netmobiel.planner.api.mapping.TripPlanMapper;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.planner.service.TripPlanManager;

@RequestScoped
public class SearchResource implements SearchApi {
	private static final int DEFAULT_MAX_WALK_DISTANCE = 1000;
	@Inject
    private Logger log;
 
	@Inject
    private TripPlanManager plannerManager;

	@Inject
    private PlannerUserManager userManager;

    @Inject
    private TripPlanMapper tripPlanMapper;

    @Context
    private SecurityContext securityContext;
    
	private Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    public Response searchPlan(
    		String from, 
    		String to, 
    		OffsetDateTime travelTime,
    		Boolean useAsArrivalTime,
    		OffsetDateTime earliestDepartureTime,
    		OffsetDateTime latestArrivalTime,
    		String modalities,
    		Integer maxWalkDistance,
    		Integer nrSeats,
    		OffsetDateTime now,
    		Integer maxTransfers,
    		Boolean firstLegRideshare,
    		Boolean lastLegRideshare
    	) {
    	
    	TripPlan plan = new TripPlan();
    	if (now == null) {
    		now = OffsetDateTime.now();
    	}
    	if (from == null || to == null) {
    		throw new BadRequestException("Missing one or more mandatory parameters: from, to");
    	}
    	Set<TraverseMode> domainModalities = parseModalities(modalities);
    	if (domainModalities == null) {
    		domainModalities = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.WALK, TraverseMode.RIDESHARE, TraverseMode.TRANSIT }));
    	}
    	if (maxWalkDistance == null) {
    		maxWalkDistance = DEFAULT_MAX_WALK_DISTANCE;
    	} else if (maxWalkDistance < 0) {
			throw new BadRequestException("Constraint validation error: maxWalkDistance == null || maxWalkDistance >= 0");
    	}
    	if (nrSeats != null) {
    		if (nrSeats < 1) {
    			throw new BadRequestException("Constraint validation error: nrSeats == null || nrSeats >= 1");
        	}
    	} else {
    		nrSeats = 1;
    	}
		try {
			PlannerUser traveller = userManager.findOrRegisterCallingUser();
			plan.setFrom(GeoLocation.fromString(from));
			plan.setTo(GeoLocation.fromString(to));
			plan.setTravelTime(toInstant(travelTime));
			plan.setUseAsArrivalTime(Boolean.TRUE.equals(useAsArrivalTime));
			plan.setEarliestDepartureTime(toInstant(earliestDepartureTime));
			plan.setLatestArrivalTime(toInstant(latestArrivalTime));
			plan.setTraverseModes(domainModalities);
			plan.setMaxWalkDistance(maxWalkDistance);
			plan.setNrSeats(nrSeats);
			plan.setMaxTransfers(maxTransfers);
			plan.setFirstLegRideshareAllowed(Boolean.TRUE.equals(firstLegRideshare));
			plan.setLastLegRideshareAllowed(Boolean.TRUE.equals(lastLegRideshare));
    		plan.setPlanType(PlanType.REGULAR);

    		plan = plannerManager.createAndReturnTripPlan(traveller, plan, toInstant(now));
    		if (log.isDebugEnabled()) {
    			log.debug("Multimodal plan for " + traveller.getEmail() + ":\n" + plan.toString());
    		}
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException("Input parameter has unrecognized format", ex);
		}
    	return Response.ok(tripPlanMapper.map(plan)).build();
    }
    
    private Set<TraverseMode> parseModalities(String modalities) {
    	Set<TraverseMode> traverseModes = new HashSet<>();
    	if (modalities != null && modalities.trim().length() > 0) {
        	try {
    	    	String modes[] = modalities.split("[,\\s]+");
    	    	traverseModes = Arrays.stream(modes)
    	    			.map(m -> TraverseMode.valueOf(m))
    	    			.collect(Collectors.toSet());
        	} catch (IllegalArgumentException ex) {
        		throw new BadRequestException("Failed to parse modalities: " + modalities, ex);
        	}
    	}
    	return traverseModes;
    }
}
