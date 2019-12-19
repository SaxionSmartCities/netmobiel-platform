package eu.netmobiel.planner.api.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.planner.service.GeocodeService;

/**
 * Geocoder API.
 */
@Path("/geocode")
@ApplicationScoped
public class GeocodeApi {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Inject
    private GeocodeService geocoder;

    @GET
    @Path("/suggest")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSuggestions(@QueryParam("query") String query) {
    	String jsonResult = geocoder.listSuggestions(query);
        return Response.status(Response.Status.OK).entity(jsonResult).build();
    }

    @GET
    @Path("/geocode")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLocation(@QueryParam("locationId") String locationId) {
    	String jsonResult = geocoder.getLocation(locationId);
        return Response.status(Response.Status.OK).entity(jsonResult).build();
    }
}
