package eu.netmobiel.planner.api.resource;

import java.time.OffsetDateTime;
import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.api.SearchApi;
import eu.netmobiel.planner.api.mapping.TripPlanMapper;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.PlannerManager;

@ApplicationScoped
public class SearchResource implements SearchApi {

	@Inject
    private Logger log;
 
	@Inject
    private PlannerManager plannerManager;

    @Inject
    private TripPlanMapper tripPlanMapper;

    public Response searchPlan(
    		String from, 
    		String to, 
    		OffsetDateTime departureTime,
    		OffsetDateTime arrivalTime,
    		String modalities,
    		Integer maxWalkDistance,
    		Integer nrSeats
    	) {
    	
    	TripPlan plan = null;

    	if (from == null || (departureTime == null && arrivalTime == null) || to == null) {
    		throw new BadRequestException("Missing one or more mandatory parameters: from, to, departureTime or arrivalTime");
    	}
    	TraverseMode[] domainModalities = null;
    	if (modalities != null) {
    		domainModalities = parseModalities(modalities);
    	}
    	
    	if (maxWalkDistance != null) {
    		if (maxWalkDistance < 0) {
    			throw new BadRequestException("Constraint validation error: maxWalkDistance == null || maxWalkDistance >= 0");
    		}
    	} else {
    		maxWalkDistance = 1000;
    	}
    	if (nrSeats != null) {
    		if (nrSeats < 1) {
    			throw new BadRequestException("Constraint validation error: nrSeats == null || nrSeats >= 1");
        	}
    	} else {
    		nrSeats = 1;
    	}
//    	plan = new TripPlan(GeoLocation.fromString(from), GeoLocation.fromString(to), 
//		departureTime != null ? departureTime.toInstant() : null, arrivalTime != null ? arrivalTime.toInstant() : null, 
//				domainModalities, maxWalkDistance, nrSeats);
//        log.debug("TripPlan: " + plan.toString());
    		try {
	    		plan = plannerManager.searchMultiModal(GeoLocation.fromString(from), GeoLocation.fromString(to), 
	    					departureTime == null ? null : departureTime.toInstant(), arrivalTime == null ? null : arrivalTime.toInstant(), 
	    					domainModalities, maxWalkDistance, nrSeats);
	    		if (log.isDebugEnabled()) {
	    			log.debug("Multimodal plan: \n" + plan.toString());
	    		}
    		} catch (IllegalArgumentException ex) {
    			throw new BadRequestException("Input parameter has unrecognized format", ex);
    		}
    	return Response.ok(tripPlanMapper.map(plan)).build();
    }
    
    private TraverseMode[] parseModalities(String modalities) {
    	if (modalities == null) {
    		return null;
    	}
    	String modes[] = modalities.split("[,\\s]+");
    	return Arrays.stream(modes)
    			.map(m -> TraverseMode.valueOf(m))
    			.toArray(TraverseMode[]::new);
    }
}
