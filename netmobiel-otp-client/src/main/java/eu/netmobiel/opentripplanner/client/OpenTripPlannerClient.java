package eu.netmobiel.opentripplanner.client;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.MinumumDistanceFilter;
import eu.netmobiel.opentripplanner.api.model.Itinerary;
import eu.netmobiel.opentripplanner.api.model.Leg;
import eu.netmobiel.opentripplanner.api.model.PlanResponse;
import eu.netmobiel.opentripplanner.api.model.TraverseMode;

@ApplicationScoped
public class OpenTripPlannerClient {
    
    private static final String OTP_GRAPHQL_REQUEST = "/routers/nl/index/graphql"; 
    private static final String OTP_PLAN_REQUEST = "/routers/nl/plan"; 
    public static final int MINIMUM_PLANNING_DISTANCE_METERS = 20;

    public static final BiPredicate<GeoLocation, GeoLocation> tooClose = (locA, locB)-> locA.getDistanceFlat(locB) < MINIMUM_PLANNING_DISTANCE_METERS;

//  https://otp.netmobiel.eu:8080/otp
    @Resource(lookup = "java:global/openTripPlanner/apiUrl")
    private String openTripPlannerApi;

    private ResteasyClient client;
	
    @Inject
    private Logger log;
    
	@PostConstruct
	public void createClient() {
		client = new ResteasyClientBuilder()
				.connectionPoolSize(200)
				.connectionCheckoutTimeout(5, TimeUnit.SECONDS)
				.maxPooledPerRoute(20)
				.register(new Jackson2ObjectMapperContextResolver())
				.property("resteasy.preferJacksonOverJsonB", true)
				.build();
	}

	@PreDestroy
	void cleanup() {
		client.close();
	}
	
    protected String grapqlQuery(String queryValue) {
		String url = openTripPlannerApi + OTP_GRAPHQL_REQUEST;
		WebTarget target = client.target(url);
		String query = "{ \"query\": \"" + queryValue + "\" }";
		String result = null;
		Entity<?> e = Entity.entity(query, MediaType.APPLICATION_JSON);
		try (Response response = target.request(MediaType.APPLICATION_JSON).post(e)) {
			if (response.getStatusInfo() != Response.Status.OK) {
				throw new WebApplicationException("Error retrieving data from OTP", response);
			}
	        result = response.readEntity(String.class);
		}
        return result;
    }

    public List<JsonObject> fetchStopsByRadius(double lat, double lon, int radius) {
		String graph = String.format("{ stopsByRadius(lat: %f, lon: %f, radius: %d) { " + 
					"    edges { node { stop { id name lat lon gtfsId platformCode } } } } }", lat, lon, radius);
		String result = grapqlQuery(graph);
        JsonReader jsonReader = Json.createReader(new StringReader(result));
        JsonObject jobj = jsonReader.readObject();
        JsonArray edges = jobj.getJsonObject("data").getJsonObject("stopsByRadius").getJsonArray("edges");
        List<JsonObject> stops = edges.getValuesAs(JsonObject.class).stream()
        		.map(node ->  node.getJsonObject("node").getJsonObject("stop"))
        		.collect(Collectors.toList());
        return stops;
    }

    public List<JsonObject> fetchAllStops() {
        String result = grapqlQuery("{ stops { id gtfsId name lat lon platformCode } }"); 
        JsonReader jsonReader = Json.createReader(new StringReader(result));
        JsonObject jobj = jsonReader.readObject();
        JsonArray jstops = jobj.getJsonObject("data").getJsonArray("stops");
        List<JsonObject> stops = jstops.getValuesAs(JsonObject.class);
        return stops;
    }

    public List<JsonObject> fetchAllClusters() {
        String result = grapqlQuery("{ clusters { id gtfsId name lat lon stops { id } } }"); 
        JsonReader jsonReader = Json.createReader(new StringReader(result));
        JsonObject jobj = jsonReader.readObject();
        JsonArray jclusters = jobj.getJsonObject("data").getJsonArray("clusters");
        List<JsonObject> clusters = jclusters.getValuesAs(JsonObject.class);
        return clusters;
    }

    public List<JsonObject> fetchAllRoutes() {
        String result = grapqlQuery("{ routes { id gtfsId shortName longName type stops { id } } }"); 
        JsonReader jsonReader = Json.createReader(new StringReader(result));
        JsonObject jobj = jsonReader.readObject();
        JsonArray jroutes = jobj.getJsonObject("data").getJsonArray("routes");
        List<JsonObject> routes = jroutes.getValuesAs(JsonObject.class);
        return routes;
    }

    public List<JsonObject> fetchAllTransfers() {
        String result = grapqlQuery("{ stops { id transfers { stop { id } distance } } }"); 
        JsonReader jsonReader = Json.createReader(new StringReader(result));
        JsonObject jobj = jsonReader.readObject();
        JsonArray jstops = jobj.getJsonObject("data").getJsonArray("stops");
        log.info("fetchAllTransfers: #" + jstops.size() + " stops");
        List<JsonObject> transfers = jstops.getValuesAs(JsonObject.class);
        return transfers;
    }

	public static URI createURI(String path) {
    	try {
			return new URI(path);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Unexpected exception", e);
		}
    }

    public PlanResponse createPlan(GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, boolean useTimeAsArriveBy, 
    		TraverseMode[] modes, boolean showIntermediateStops, Integer maxWalkDistance, GeoLocation[] via, Integer maxItineraries) throws NotFoundException {
		PlanResponse result = null;
		
    	List<GeoLocation> places = new ArrayList<>();
    	places.add(fromPlace);
    	if (via != null) {
    		places.addAll(Arrays.asList(via));
    	}
    	places.add(toPlace);
    	// Remove places that are too close, OTP will not accept
    	places = places.stream()
    			.filter(new MinumumDistanceFilter(OpenTripPlannerClient.MINIMUM_PLANNING_DISTANCE_METERS))
    			.collect(Collectors.toList());
    	if (places.size() < 2) {
    		// This can only mean that the ride has from an do very close
    		throw new NotFoundException("Ride departure and arrival location are too close");
    	}
   		List<GeoLocation> vias = places.subList(1, places.size() - 1);
		
		boolean forcedDepartureTime = false;
		if (vias != null && vias.size() > 0) {
			if (useTimeAsArriveBy && ! TraverseMode.containsTransit(modes)) {
				forcedDepartureTime = true;
				useTimeAsArriveBy = false;
//				throw new IllegalArgumentException("Due to bug #2345 in OTP (https://github.com/opentripplanner/OpenTripPlanner/issues/2345) + 
//					the intermediatePlaces cannot be combined with arriveBy=true if there is no transit involved");
			}
		}
   		// The OTP translates the input date using the system default zone. So we do the opposite conversion. 
    	// @see OTP Planner org.opentripplanner.api.resource.PlannerResource.plan(UriInfo, Request)
		// Skip the milliseconds
		LocalDateTime localTravelTime = travelTime
				.truncatedTo(ChronoUnit.SECONDS)
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();
		UriBuilder ub = UriBuilder.fromUri(createURI(openTripPlannerApi + OTP_PLAN_REQUEST));
		ub.queryParam("fromPlace", fromPlace.toString());
		ub.queryParam("toPlace", toPlace.toString());
		ub.queryParam("date", DateTimeFormatter.ISO_LOCAL_DATE.format(localTravelTime.toLocalDate()));
		ub.queryParam("time", DateTimeFormatter.ISO_LOCAL_TIME.format(localTravelTime.toLocalTime()));
		ub.queryParam("arriveBy", Boolean.toString(useTimeAsArriveBy));
		ub.queryParam("showIntermediateStops", Boolean.toString(showIntermediateStops));
		ub.queryParam("mode", Arrays.asList(modes).stream()
				.map(m -> m.name())
				.collect(Collectors.joining(",")));
		if (maxWalkDistance != null) {
			ub.queryParam("maxWalkDistance", String.valueOf(maxWalkDistance));
		}
		if (maxItineraries != null) {
			ub.queryParam("numItineraries", maxItineraries.toString());
		}
		if (vias != null) {
			vias.forEach(loc -> ub.queryParam("intermediatePlaces", loc.toString()));
		}
		WebTarget target = client.target(ub);
		if (log.isDebugEnabled()) {
			log.debug("OTP request: " + target.getUri().toString());
		}
		try (Response response = target.request().get()) {
			if (response.getStatusInfo() != Response.Status.OK) {
				throw new WebApplicationException("Error retrieving data from OTP", response);
			}
//			response.bufferEntity();
//			log.debug(JsonHelper.prettyPrint(JsonHelper.parseJson(response.readEntity(String.class))));
	        result = response.readEntity(PlanResponse.class);
		}
//		if (result.error != null) {
//			String msg = String.format("%s - %s", result.error.message, result.error.msg);
//			if (result.error.missing != null && result.error.missing.size() > 0) {
//				msg = String.format("%s Missing parameters [ %s ]", msg, String.join(",", result.error.missing));
//			}
//			throw new WebApplicationException(msg, result.error.message.getStatus());
//		}
		if (result.plan != null && forcedDepartureTime) {
			// Shift the plan in time (earlier) so that arrivalTime equals departureTime
			// The plan header is ok. Just modify itinerary and legs. 
			for (Itinerary it : result.plan.itineraries) {
				it.startTime = it.startTime.minusSeconds(it.duration);  
				it.endTime = it.endTime.minusSeconds(it.duration);
				for (Leg leg : it.legs) {
					if (leg.mode.isTransit()) {
						throw new RuntimeException("Leg should not be shifted, it is time-dependent - " + leg.toString());
					}
					leg.startTime = leg.startTime.minusSeconds(it.duration);  
					leg.endTime = leg.endTime.minusSeconds(it.duration);
				}
			}
		}
		return result;
    }

}
