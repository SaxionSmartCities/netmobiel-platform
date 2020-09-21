package eu.netmobiel.communicator.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.communicator.api.MessagesApi;
import eu.netmobiel.communicator.api.mapping.MessageMapper;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.CommunicatorUserManager;
import eu.netmobiel.communicator.service.PublisherService;

@ApplicationScoped
public class MessagesResource implements MessagesApi {

	@Inject
	private MessageMapper mapper;

    @Inject
    private PublisherService publisherService;

	@Inject
    private CommunicatorUserManager userManager;

    @Override
	public Response sendMessage(eu.netmobiel.communicator.api.model.Message msg) {
    	Response rsp = null;
		try {
			CommunicatorUser caller = userManager.registerCallingUser();
			publisherService.validateMessage(caller, mapper.map(msg));
			publisherService.publish(caller, mapper.map(msg));
			rsp = Response.status(Status.ACCEPTED).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response listMessages(Boolean groupByConversation, String participant, String context, 
			OffsetDateTime since, OffsetDateTime until, String deliveryMode, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Message> result = null;
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
		return rsp;
	}

	@Override
	public Response acknowledgeMessage(Integer messageId) {
    	Response rsp = null;
    	try {
			CommunicatorUser caller = userManager.findCallingUser();
			publisherService.updateAcknowledgment(caller, messageId.longValue(), Instant.now());
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response removeAcknowledgement(Integer messageId) {
    	Response rsp = null;
    	try {
			CommunicatorUser caller = userManager.findCallingUser();
			publisherService.updateAcknowledgment(caller, messageId.longValue(), null);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
