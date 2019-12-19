package eu.netmobiel.planner.api.resource;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.api.SearchApi;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.PlannerManager;

@ApplicationScoped
public class SearchResource implements SearchApi {

	@Inject
    private Logger log;
 
	@Inject
    private PlannerManager plannerManager;

    public Response searchPlan(
    		String fromPlace, 
    		String toPlace, 
    		String fromDate,
    		String toDate,
    		Integer nrSeats,
    		Integer maxResults,
    		Integer offset
    	) {
    	TripPlan plan = null;
    	if (fromPlace == null || (fromDate == null && toDate == null) || toPlace == null) {
    		throw new BadRequestException("Missing one or more mandatory parameters: fromPlace, toPlace, fromDate or toDate");
    	} else if (fromPlace != null && toPlace != null && (fromDate != null || toDate != null)) {
    		try {
    			LocalDateTime departureDate = fromDate != null ? LocalDateTime.parse(fromDate) : null;
    			LocalDateTime arrivalDate = toDate != null ? LocalDateTime.parse(toDate) : null;
	    		plan = plannerManager.searchMultiModal(GeoLocation.fromString(fromPlace), GeoLocation.fromString(toPlace), departureDate, arrivalDate, nrSeats);
	    		if (log.isDebugEnabled()) {
	    			log.debug("Multimodal plan: \n" + plan.toString());
	    		}
    		} catch (DateTimeParseException ex) {
    			throw new BadRequestException("Date parameter has unrecognized format", ex);
    		} catch (IllegalArgumentException ex) {
    			throw new BadRequestException("Input parameter has unrecognized format", ex);
    		}
    	}
    	return Response.ok(plan).build();
    }
}
