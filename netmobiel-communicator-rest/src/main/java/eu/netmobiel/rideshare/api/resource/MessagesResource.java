package eu.netmobiel.rideshare.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.communicator.api.MessagesApi;
import eu.netmobiel.communicator.api.model.Message;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.rideshare.api.mapping.MessageMapper;

@ApplicationScoped
public class MessagesResource implements MessagesApi {

	@Inject
	private MessageMapper mapper;

    @Inject
    private PublisherService publisherService;

    @Override
	public Response sendMessage(Message msg) {
    	Response rsp = null;
		try {
			publisherService.publish(mapper.map(msg), 
					msg.getRecipients().stream().map(u -> mapper.map(u)).collect(Collectors.toList()));
			rsp = Response.status(Status.ACCEPTED).build();
		} catch (CreateException e) {
			throw new InternalServerErrorException(e);
		} catch (eu.netmobiel.commons.exception.BadRequestException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response listMessages(Boolean groupByConveration, String participant, String context, 
			OffsetDateTime since, OffsetDateTime until, Integer maxResults, Integer offset) {
		Response rsp = null;
		if (groupByConveration != null && groupByConveration) {
			if (context != null || since != null || until != null) {
				throw new BadRequestException("Parameters 'context', 'since' or 'until' are not allowed when listing conversations"); 
			}
			List<Envelope> envelopes = publisherService.listConversation(participant, maxResults, offset); 
			rsp = Response.ok(envelopes.stream().map(e -> mapper.map(e)).collect(Collectors.toList())).build();
		} else {
			List<Envelope> envelopes = publisherService.listEnvelopes(participant, context, 
							since != null ? since.toInstant() : null, 
							until != null ? until.toInstant() : null, 
							maxResults, offset); 
			rsp = Response.ok(envelopes.stream().map(e -> mapper.map(e)).collect(Collectors.toList())).build();
		}
		return rsp;
	}

	@Override
	public Response acknowledgeMessage(Integer messageId) {
    	Response rsp = null;
    	try {
			publisherService.updateAcknowledgment(messageId.longValue(), Instant.now());
			rsp = Response.noContent().build();
		} catch (eu.netmobiel.commons.exception.NotFoundException e) {
			rsp = Response.status(Status.NOT_FOUND).build();
		}
    	return rsp;
	}

	@Override
	public Response removeAcknowledgement(Integer messageId) {
    	Response rsp = null;
    	try {
			publisherService.updateAcknowledgment(messageId.longValue(), null);
			rsp = Response.noContent().build();
		} catch (eu.netmobiel.commons.exception.NotFoundException e) {
			rsp = Response.status(Status.NOT_FOUND).build();
		}
    	return rsp;
	}

}
