package eu.netmobiel.communicator.api.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.service.PublisherService;

@Path("/tests")
@RequestScoped
@Logging
public class TestsResource {
	@Inject
    private Logger log;

	@Inject
	private SecurityIdentity securityIdentity;

    @Inject
    private PublisherService publisherService;

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
    	return Response.ok().build();
    }

    public static class MessageRequest {
		private String managedIdentity; 
    	private String text; 
    	public String getManagedIdentity() {
			return managedIdentity;
		}
		public void setManagedIdentity(String managedIdentity) {
			this.managedIdentity = managedIdentity;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
    }
    
    @POST
    @Path("/sms-message")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response smsMessage(MessageRequest mrq) {
    	Response rsp = null;
		try {
			publisherService.sendTextMessage(mrq.managedIdentity, mrq.text);
			rsp = Response.ok().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }

    @GET
    @Path("/sms-message/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSmsMessage(@PathParam("id") String messageId) {
    	Response rsp = null;
		try {
			Object obj = publisherService.getMessageBirdMessage(messageId);
			rsp = Response.ok(obj).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }

    @POST
    @Path("/voice-message")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response voiceMessage(MessageRequest mrq) {
    	Response rsp = null;
		try {
			publisherService.sendVoiceMessage(mrq.managedIdentity, mrq.text);
			rsp = Response.ok().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }
}
