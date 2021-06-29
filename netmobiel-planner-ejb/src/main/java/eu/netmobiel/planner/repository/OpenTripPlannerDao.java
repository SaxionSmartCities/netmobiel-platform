package eu.netmobiel.planner.repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Coordinate;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.opentripplanner.api.model.PlanResponse;
import eu.netmobiel.opentripplanner.client.OpenTripPlannerClient;
import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.model.OtpRoute;
import eu.netmobiel.planner.model.OtpStop;
import eu.netmobiel.planner.model.OtpTransfer;
import eu.netmobiel.planner.model.PlannerReport;
import eu.netmobiel.planner.model.PlannerResult;
import eu.netmobiel.planner.model.ToolType;
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
    	List<OtpStop> stops = null;
    	List<JsonObject> jstops = otpClient.fetchAllStops(); 
        log.info("fetchAllStops: #" + jstops.size() + " stops");
        try (Jsonb jsonb = JsonbBuilder.create()) {
            stops = jstops.stream()
            		.map(stop -> jsonb.fromJson(stop.toString(), OtpStop.class))
            		.collect(Collectors.toList());
        } catch (Exception e) {
        	throw new SystemException("Error closing Jsonb", e);
		}
        return stops;
    }

    public List<OtpCluster> fetchAllClusters() {
    	List<JsonObject> jclusters = otpClient.fetchAllClusters();
        log.info("fetchAllClusters: #" + jclusters.size() + " clusters");
        List<OtpCluster> clusters = null;
        try (Jsonb jsonb = JsonbBuilder.create()) {
            clusters = jclusters.stream()
            		.map(stop -> jsonb.fromJson(stop.toString(), OtpCluster.class))
            		.collect(Collectors.toList());
        } catch (Exception e) {
        	throw new SystemException("Error closing Jsonb", e);
        }
        return clusters;
    }

    public List<OtpRoute> fetchAllRoutes() {
    	List<JsonObject> jroutes = otpClient.fetchAllRoutes();
    	List<OtpRoute> routes = null;
        try (Jsonb jsonb = JsonbBuilder.create()) {
	        log.info("fetchAllRoutes: #" + jroutes.size() + " routes");
	        routes = jroutes.stream()
	        		.map(stop -> jsonb.fromJson(stop.toString(), OtpRoute.class))
	        		.collect(Collectors.toList());
        } catch (Exception e) {
        	throw new SystemException("Error closing Jsonb", e);
        }
        return routes;
    }

    public List<OtpTransfer> fetchAllTransfers() {
    	List<JsonObject> jstops = otpClient.fetchAllTransfers();
    	List<OtpTransfer> transfers = null;
        try (Jsonb jsonb = JsonbBuilder.create()) {
            log.info("fetchAllTransfers: #" + jstops.size() + " stops");
            transfers = jstops.stream()
            		.map(jstop -> {
            			OtpStop stop = jsonb.fromJson(jstop.toString(), OtpStop.class);
            			stop.getTransfers().forEach(t -> t.setFromStop(stop));
            			return stop;
            		})
            		.flatMap(stop -> stop.getTransfers().stream())
            		.collect(Collectors.toList());
        } catch (Exception e) {
        	throw new SystemException("Error closing Jsonb", e);
        }
        return transfers;
    }

    /**
     * Call the OTP to create a trip plan with a number of possible itineraries.
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
     * @return A planner result consisting of a report and a list of itineraries.
     * @throws NotFoundException When no itinerary could be found. 
     * @throws BadRequestException When the planner cannot plan due to the combination of parameters.
     */
    public PlannerResult createPlan(Instant now, GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, boolean isArrivalPinned, 
    		Set<TraverseMode> modes, boolean showIntermediateStops, Integer maxWalkDistance, Integer maxTransfers, List<GeoLocation> via, Integer maxItineraries) {
    	PlannerReport report = new PlannerReport();
    	report.setRequestTime(now);
    	report.setTravelTime(travelTime);
    	report.setUseAsArrivalTime(isArrivalPinned);
    	report.setFrom(fromPlace);
    	report.setTo(toPlace);
    	report.setTraverseModes(modes);
    	report.setMaxWalkDistance(maxWalkDistance);
    	report.setToolType(ToolType.OPEN_TRIP_PLANNER);
    	report.setMaxResults(maxItineraries);
    	report.setViaLocations(via);
    	report.setRequestGeometry(GeometryHelper.createLines(fromPlace.getPoint().getCoordinate(), 
    			toPlace.getPoint().getCoordinate(), 
    			via == null ? null : via.stream().map(loc -> loc.getPoint().getCoordinate()).toArray(Coordinate[]::new))
    	);
    	
    	eu.netmobiel.opentripplanner.api.model.TraverseMode[] otpModes = modes.stream()
    			.map(m -> eu.netmobiel.opentripplanner.api.model.TraverseMode.valueOf(m.name()))
    			.toArray(eu.netmobiel.opentripplanner.api.model.TraverseMode[]::new);
    	GeoLocation otpVia[] = via == null ? null : via.toArray(new GeoLocation[via.size()]);
    	long start = System.currentTimeMillis();
		PlannerResult plannerResult = new PlannerResult(report);
    	try {
        	PlanResponse result = otpClient.createPlan(fromPlace, toPlace, travelTime, isArrivalPinned, 
					otpModes, showIntermediateStops, maxWalkDistance, maxTransfers, otpVia, maxItineraries);
    		if (result.error != null) {
    			String msg = String.format("OTP Planner Error: %s - %s", result.error.message, result.error.msg);
    			if (result.error.missing != null && result.error.missing.size() > 0) {
    				msg = String.format("%s Missing parameters [ %s ]", msg, String.join(",", result.error.missing));
    			}
    			report.setErrorText(msg);
    			report.setErrorVendorCode(result.error.message.name());
    			report.setStatusCode(result.error.message.getStatus().getStatusCode());
    		} else {
    			report.setStatusCode(Response.Status.OK.getStatusCode());
    			TripPlan plan = tripPlanMapper.map(result.plan);
    			report.setNrItineraries(plan.getItineraries().size());
    			plannerResult.addItineraries(plan.getItineraries());
    		}
    	} catch (NotFoundException ex) {
			report.setErrorText(ex.getMessage());
			report.setErrorVendorCode(ex.getVendorCode());
			report.setStatusCode(Response.Status.NOT_FOUND.getStatusCode());
    	} catch (WebApplicationException ex) {
			report.setErrorText(ex.getMessage());
			report.setStatusCode(ex.getResponse().getStatus());
    	} catch (Exception ex) {
			report.setErrorText(String.join(" - ", ExceptionUtil.unwindExceptionMessage("Error calling OTP", ex)));
			report.setErrorVendorCode(ex.getClass().getSimpleName());
			report.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    	}
    	report.setExecutionTime(System.currentTimeMillis() - start);
		return plannerResult;
    }
    
}
