package eu.netmobiel.communicator.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.security.NetmobielSecurityRole;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.communicator.api.MessagesApi;
import eu.netmobiel.communicator.api.mapping.MessageMapper;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.CommunicatorUserManager;
import eu.netmobiel.communicator.service.PublisherService;

@RequestScoped
public class MessagesResource implements MessagesApi {

	@Inject
	private MessageMapper mapper;

    @Inject
    private PublisherService publisherService;

	@Inject
    private CommunicatorUserManager userManager;

    @Inject
	private SecurityIdentity securityIdentity;

	@Context
	private HttpServletRequest request;

    @Override
	public Response sendMessage(String xDelegator, eu.netmobiel.communicator.api.model.Message apiMsg) {
    	Response rsp = null;
		try {
			CallingContext<CommunicatorUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
			Message msg = mapper.map(apiMsg);
			CommunicatorUser sender = null;
			if (msg.isToAllUsers()) {
				if (!request.isUserInRole(NetmobielSecurityRole.ADMIN.getRoleName())) {
					throw new SecurityException("You have no privilege to send a message to all users");
				}
			} else {
				sender = context.getEffectiveUser();
			}
			// Validate for the API users.
			publisherService.validateMessage(sender, msg);
			publisherService.publish(sender, msg);
			rsp = Response.status(Status.ACCEPTED).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response listMessages(String xDelegator, Boolean groupByConversation, String participant, String context, 
			OffsetDateTime since, OffsetDateTime until, String deliveryMode, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Message> result = null;
		try {
			if (participant == null && !request.isUserInRole("admin")) {
				participant = securityIdentity.getEffectivePrincipal().getName();
			} else if ("me".equals(participant)) {
				participant = securityIdentity.getEffectivePrincipal().getName();
			}
			if (groupByConversation != null && groupByConversation) {
				if (context != null || deliveryMode != null || since != null || until != null) {
					throw new BadRequestException("Parameters 'context', 'deliveryMode', 'since' or 'until' are not allowed when listing conversations"); 
				}
				result = publisherService.listConversations(participant, maxResults, offset); 
			} else {
				DeliveryMode dm = deliveryMode == null ? DeliveryMode.MESSAGE : 
					(deliveryMode.isEmpty() ? DeliveryMode.ALL :  
						Stream.of(DeliveryMode.values())
							.filter(m -> m.getCode().equals(deliveryMode))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("Unsupported DeliveryMode: " + deliveryMode)));
				result = publisherService.listMessages(participant, context, 
								since != null ? since.toInstant() : null, 
								until != null ? until.toInstant() : null,
								dm,
								maxResults, offset); 
			}
			rsp = Response.ok(mapper.map(result)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response acknowledgeMessage(String xDelegator, Integer messageId) {
    	Response rsp = null;
    	try {
			CallingContext<CommunicatorUser> context = userManager.findCallingContext(securityIdentity);
			CommunicatorUser recipient = context.getEffectiveUser();
			publisherService.updateAcknowledgment(recipient, messageId.longValue(), Instant.now());
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response removeAcknowledgement(String xDelegator, Integer messageId) {
    	Response rsp = null;
    	try {
			CallingContext<CommunicatorUser> context = userManager.findCallingContext(securityIdentity);
			CommunicatorUser recipient = context.getEffectiveUser();
			publisherService.updateAcknowledgment(recipient, messageId.longValue(), null);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
