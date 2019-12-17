package eu.netmobiel.planner.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.NameValuePair;
import eu.netmobiel.commons.util.UriHelper;
import eu.netmobiel.opentripplanner.client.OpenTripPlannerClient;
import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.model.OtpRoute;
import eu.netmobiel.planner.model.OtpStop;
import eu.netmobiel.planner.model.OtpTransfer;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TraverseMode;

@ApplicationScoped
public class OpenTripPlannerDao {
    @SuppressWarnings("unused")
	@Inject
    private Logger log;
    
    @Inject
    private OpenTripPlannerClient otpClient;
    

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

    public TripPlan createPlan(GeoLocation fromPlace, GeoLocation toPlace, LocalDateTime fromDate, LocalDateTime toDate, 
    		TraverseMode[] modes, boolean showIntermediateStops, Integer maxWalkDistance, List<GeoLocation> via, Integer maxItineraries) {
		return null;
    }
}
