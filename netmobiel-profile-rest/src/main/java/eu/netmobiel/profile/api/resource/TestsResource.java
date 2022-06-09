package eu.netmobiel.profile.api.resource;

import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.Logging;

@Path("/tests")
@RequestScoped
@Logging
public class TestsResource {
	@Inject
    private Logger log;

	@Inject
	private SecurityIdentity securityIdentity;

	@Context
	private HttpServletRequest request;

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
    @Path("/400nb")
    @Produces(MediaType.APPLICATION_JSON)
    public Response badRequestNetmobiel() {
    	throw new WebApplicationException(new eu.netmobiel.commons.exception.BadRequestException("This is a really bad request"));
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
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response check() {
    	log.debug("User: " + securityIdentity);
//    	String me = securityIdentity.getEffectivePrincipal().getName();
    	Optional<String> sessionId = SecurityIdentity.getKeycloakSessionId(securityIdentity.getEffectivePrincipal());
    	log.debug("Session kcid: " + (sessionId.isPresent() ? sessionId.get() : "Unknown"));
    	log.debug("Session rsid: " + request.getRequestedSessionId());
    	HttpSession session = request.getSession();
    	log.debug("Session http: " + session.getId());
    	return Response.ok().build();
    }

}
