package eu.netmobiel.rideshare.api.resource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotSupportedException;
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
			publisherService.publish(mapper.map(msg), msg.getRecipients());
			rsp = Response.status(Status.ACCEPTED).build();
		} catch (CreateException e) {
			throw new InternalServerErrorException(e);
		}
		return rsp;
	}

	@Override
	public Response getMessage(String messageId) {
		throw new NotSupportedException();
	}

	@Override
	public Response listMessages(Boolean groupByConveration, String recipient, String context, 
			OffsetDateTime since, OffsetDateTime until, Integer maxResults, Integer offset) {
		Response rsp = null;
		if (groupByConveration != null && groupByConveration) {
			if (context != null || since != null || until != null) {
				throw new BadRequestException("Parameters 'context', 'since' or 'until' are not allowed whenlisting conversations"); 
			}
			List<Envelope> envelopes = publisherService.listConversation(recipient, maxResults, offset); 
			rsp = Response.ok(envelopes.stream().map(e -> mapper.map(e)).collect(Collectors.toList())).build();
		} else {
			List<Envelope> envelopes = publisherService.listEnvelopes(recipient, context, 
							since != null ? since.toInstant() : null, 
							until != null ? until.toInstant() : null, 
							maxResults, offset); 
			rsp = Response.ok(envelopes.stream().map(e -> mapper.map(e)).collect(Collectors.toList())).build();
		}
		return rsp;
	}

}
