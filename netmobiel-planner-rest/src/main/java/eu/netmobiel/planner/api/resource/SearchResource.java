package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.security.SecurityContextHelper;
import eu.netmobiel.planner.api.SearchApi;
import eu.netmobiel.planner.api.mapping.TripPlanMapper;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.PlannerManager;

@RequestScoped
public class SearchResource implements SearchApi {
	private static final int DEFAULT_MAX_WALK_DISTANCE = 1000;
	@Inject
    private Logger log;
 
	@Inject
    private PlannerManager plannerManager;

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
    		OffsetDateTime departureTime,
    		OffsetDateTime arrivalTime,
    		String modalities,
    		Integer maxWalkDistance,
    		Integer nrSeats,
    		OffsetDateTime now,
    		Integer maxTransfers,
    		Boolean firstLegRideshare,
    		Boolean lastLegRideshare
    	) {
    	
    	TripPlan plan = null;
    	if (now == null) {
    		now = OffsetDateTime.now();
    	}
    	if (from == null || to == null) {
    		throw new BadRequestException("Missing one or more mandatory parameters: from, to");
    	}
    	TraverseMode[] domainModalities = parseModalities(modalities);
    	if (domainModalities == null) {
    		domainModalities = new TraverseMode[] { TraverseMode.WALK, TraverseMode.RIDESHARE, TraverseMode.TRANSIT };
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
			NetMobielUser user = SecurityContextHelper.getUserContext(securityContext.getUserPrincipal());
    		plan = plannerManager.searchMultiModal(toInstant(now), GeoLocation.fromString(from), GeoLocation.fromString(to), 
    					toInstant(departureTime), toInstant(arrivalTime), domainModalities, maxWalkDistance, nrSeats,
    					maxTransfers, firstLegRideshare, lastLegRideshare);
    		if (log.isDebugEnabled()) {
    			log.debug("Multimodal plan for " + (user != null ? user.getEmail() : "<unknown>") + ":\n" + plan.toString());
    		}
		} catch (eu.netmobiel.commons.exception.BadRequestException ex) {
			throw new WebApplicationException(ex);
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException("Input parameter has unrecognized format", ex);
		}
    	return Response.ok(tripPlanMapper.map(plan)).build();
    }
    
    private TraverseMode[] parseModalities(String modalities) {
    	TraverseMode[] traverseModes = null;
    	if (modalities != null && modalities.trim().length() > 0) {
        	try {
    	    	String modes[] = modalities.split("[,\\s]+");
    	    	traverseModes = Arrays.stream(modes)
    	    			.map(m -> TraverseMode.valueOf(m))
    	    			.toArray(TraverseMode[]::new);
        	} catch (IllegalArgumentException ex) {
        		throw new BadRequestException("Failed to parse modalities: " + modalities, ex);
        	}
    	}
    	return traverseModes;
    }
}
