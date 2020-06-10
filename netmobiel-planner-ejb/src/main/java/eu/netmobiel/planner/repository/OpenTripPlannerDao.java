package eu.netmobiel.planner.repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.opentripplanner.api.model.PlanResponse;
import eu.netmobiel.opentripplanner.client.OpenTripPlannerClient;
import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.model.OtpRoute;
import eu.netmobiel.planner.model.OtpStop;
import eu.netmobiel.planner.model.OtpTransfer;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.mapping.TripPlanMapper;

@ApplicationScoped
public class OpenTripPlannerDao {
	@Inject
    private Logger log;
    
    @Inject
    private OpenTripPlannerClient otpClient;
    
    @Inject
    private TripPlanMapper tripPlanMapper;

    public List<OtpStop> fetchAllStops() {
    	List<JsonObject> jstops = otpClient.fetchAllStops(); 
        log.info("fetchAllStops: #" + jstops.size() + " stops");
        Jsonb jsonb = JsonbBuilder.create();
        List<OtpStop> stops = jstops.stream()
        		.map(stop -> jsonb.fromJson(stop.toString(), OtpStop.class))
        		.collect(Collectors.toList());
        return stops;
    }

    public List<OtpCluster> fetchAllClusters() {
    	List<JsonObject> jclusters = otpClient.fetchAllClusters();
        log.info("fetchAllClusters: #" + jclusters.size() + " clusters");
        Jsonb jsonb = JsonbBuilder.create();
        List<OtpCluster> clusters = jclusters.stream()
        		.map(stop -> jsonb.fromJson(stop.toString(), OtpCluster.class))
        		.collect(Collectors.toList());
        return clusters;
    }

    public List<OtpRoute> fetchAllRoutes() {
    	List<JsonObject> jroutes = otpClient.fetchAllRoutes();
        Jsonb jsonb = JsonbBuilder.create();
        log.info("fetchAllRoutes: #" + jroutes.size() + " routes");
        List<OtpRoute> routes = jroutes.stream()
        		.map(stop -> jsonb.fromJson(stop.toString(), OtpRoute.class))
        		.collect(Collectors.toList());
        return routes;
    }

    public List<OtpTransfer> fetchAllTransfers() {
    	List<JsonObject> jstops = otpClient.fetchAllTransfers();
        Jsonb jsonb = JsonbBuilder.create();
        log.info("fetchAllTransfers: #" + jstops.size() + " stops");
        List<OtpTransfer> transfers = jstops.stream()
        		.map(jstop -> {
        			OtpStop stop = jsonb.fromJson(jstop.toString(), OtpStop.class);
        			stop.getTransfers().forEach(t -> t.setFromStop(stop));
        			return stop;
        		})
        		.flatMap(stop -> stop.getTransfers().stream())
        		.collect(Collectors.toList());
        return transfers;
    }

    /**
     * Call the OTP the create a trip plan with a number of possible itineraries.
     * @param fromPlace The place to depart from.
     * @param toPlace the intended place of arrival.
     * @param departureTime the departure time. This is an instant, i.e. a precise moment in time.
     * @param arrivalTime the intended arrival time. This is an instant, i.e. a precise moment in time.
     * @param modes An array of traversel modes like rail, bus etc.
     * @param showIntermediateStops if true then list the intermediate stops too.
     * @param maxWalkDistance the maximum distance to walk to and from transfers.
     * @param maxTransfers the maximum number of transfers one is allowed to take.
     * @param via a list of places that must be part of the itineraries.
     * @param maxItineraries The maximum number of itineraries to list.
     * @return A trip plan with 1 or more itineraries.
     * @throws NotFoundException When no itinerary could be found. 
     * @throws BadRequestException When the planner cannot plan due to the combination of parameters.
     */
    public TripPlan createPlan(GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, boolean isArrivalPinned, 
    		TraverseMode[] modes, boolean showIntermediateStops, Integer maxWalkDistance, Integer maxTransfers, List<GeoLocation> via, Integer maxItineraries) 
    				throws NotFoundException, BadRequestException {
    	eu.netmobiel.opentripplanner.api.model.TraverseMode[] otpModes = Arrays
    			.stream(modes)
    			.map(m -> eu.netmobiel.opentripplanner.api.model.TraverseMode.valueOf(m.name()))
    			.toArray(eu.netmobiel.opentripplanner.api.model.TraverseMode[]::new);
    	GeoLocation otpVia[] = via == null ? null : via.toArray(new GeoLocation[via.size()]);
    	PlanResponse result = otpClient.createPlan(fromPlace, toPlace, travelTime, isArrivalPinned, 
    					otpModes, showIntermediateStops, maxWalkDistance, maxTransfers, otpVia, maxItineraries);
		if (result.error != null) {
			String msg = String.format("OTP Planner Error: %s - %s", result.error.message, result.error.msg);
			if (result.error.missing != null && result.error.missing.size() > 0) {
				msg = String.format("%s Missing parameters [ %s ]", msg, String.join(",", result.error.missing));
			}
			if (result.error.message.getStatus().getStatusCode() >= 500) {
				throw new SystemException(msg, result.error.message.name());
			} else if (result.error.message.getStatus() == Response.Status.NOT_FOUND) {
				throw new NotFoundException(msg, result.error.message.name());
			} else {
				throw new BadRequestException(msg, result.error.message.name());
			}
		}
		TripPlan plan = tripPlanMapper.map(result.plan);
		plan.setTraverseModes(modes);
		plan.setMaxWalkDistance(maxWalkDistance);
		return plan;
    }
    
}
