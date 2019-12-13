package eu.netmobiel.rideshare.api.resource;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.rideshare.service.RideManager;
import eu.netmobiel.rideshare.service.UserManager;

@Path("/tests")
@ApplicationScoped
public class TestsResource {
    @Inject
    private UserManager userManager;

    @Inject
    private RideManager rideManager;

    @GET
    @Path("/ejb")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testEJBException() {
    	userManager.throwRuntimeException();
    	return Response.status(Status.EXPECTATION_FAILED).build();
    }
    @GET
    @Path("/ejbaccess")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testEJBAccessException() {
    	userManager.throwAccessException();
    	return Response.status(Status.EXPECTATION_FAILED).build();
    }

    @GET
    @Path("/rte")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testRuntime() {
    	throw new RuntimeException("This is a bug!");
    }

    @GET
    @Path("/400")
    @Produces(MediaType.APPLICATION_JSON)
    public Response badRequest() {
    	throw new BadRequestException("This is a really bad request");
    }

    @GET
    @Path("/404")
    @Produces(MediaType.APPLICATION_JSON)
    public Response notFound() {
    	throw new NotFoundException("Can't find it!");
    }

    @GET
    @Path("/500")
    @Produces(MediaType.APPLICATION_JSON)
    public Response internalServerError() {
    	throw new InternalServerErrorException("Ok, this is bad, but at least I know myself it is a 500!");
    }
    @GET
    @Path("/security")
    @Produces(MediaType.APPLICATION_JSON)
    public Response securityException() {
    	throw new SecurityException("You are not allowed to do this!");
    }

    @GET
    @Path("/doRideMaintenance")
    public Response doRideMaintenance(@QueryParam("start") String startDate) {
    	LocalDate start = null;
    	if (startDate != null) {
    		start = LocalDate.parse(startDate);
    	}
    	rideManager.instantiateRecurrentRides(start);
    	return Response.status(Status.OK).build();
    	
    }
}
