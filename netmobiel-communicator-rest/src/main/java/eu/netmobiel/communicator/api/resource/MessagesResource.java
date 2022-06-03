package eu.netmobiel.communicator.api.resource;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.api.MessagesApi;
import eu.netmobiel.communicator.api.mapping.MessageMapper;
import eu.netmobiel.communicator.filter.MessageFilter;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.CommunicatorUserManager;
import eu.netmobiel.communicator.service.PublisherService;

@RequestScoped
public class MessagesResource extends CommunicatorResource implements MessagesApi {

	@Inject
	private MessageMapper mapper;

    @Inject
    private PublisherService publisherService;

	@Inject
    private CommunicatorUserManager userManager;

    @Override
	public Response sendMessage(String xDelegator, eu.netmobiel.communicator.api.model.Message msg) {
    	Response rsp = null;
		try {
			CallingContext<CommunicatorUser> callingContext = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser sender = callingContext.getEffectiveUser();
			if (msg.getSender() != null && !request.isUserInRole("admin")) {
				throw new SecurityException("You have no privilege to specify a sender");
			}
			Message.MessageBuilder mb = Message.create()
					.withBody(msg.getBody())
					.withContext(msg.getContext())
					// One to one match
					.withDeliveryMode(DeliveryMode.valueOf(msg.getDeliveryMode().name()))
					.withSender(msg.getSenderContext(), 
							msg.getSender() != null ? msg.getSender().getManagedIdentity() : sender.getManagedIdentity());
			for (eu.netmobiel.communicator.api.model.Envelope env : msg.getEnvelopes()) {
				mb.addEnvelope(env.getContext())
					.withRecipient(env.getRecipient().getManagedIdentity())
					// Leave it to the system to identify the right conversation or a new one if needed.
					// There is no topic, so the system will ask the overseer what to do 
					.withConversationContext(env.getContext())
					.buildConversation();
			}
			Message message = mb.buildMessage();
			Long mid = publisherService.chat(message);
			String newMsgUrn = UrnHelper.createUrn(Message.URN_PREFIX, mid);
			rsp = Response.created(URI.create(newMsgUrn)).build() ;
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response listMessages(String xDelegator, String participantId, String context, 
			OffsetDateTime since, OffsetDateTime until, String deliveryMode, String sortDir, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Message> result = null;
		try {
			CallingContext<CommunicatorUser> callingContext = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser participant = resolveUserReference(callingContext, participantId);
			allowAdminOrEffectiveUser(callingContext, participant);
			MessageFilter filter = new MessageFilter(participant, since, until, context, sortDir);
			Cursor cursor = new Cursor(maxResults, offset);
			if (deliveryMode != null && !deliveryMode.isEmpty()) {
	        	DeliveryMode dm = Stream.of(DeliveryMode.values())
							.filter(m -> m.name().equals(deliveryMode))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("Unsupported DeliveryMode: " + deliveryMode));
				filter.setDeliveryMode(dm);
			} else {
				filter.setDeliveryMode(DeliveryMode.MESSAGE);
			}
			result = publisherService.listMessages(filter, cursor); 
			if (!request.isUserInRole("admin")) {
				for (Message msg : result.getData()) {
					removeOtherRecipients(participant, msg);
				}
			}
			rsp = Response.ok(mapper.map(result)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response acknowledgeMessage(String xDelegator, String messageId) {
    	Response rsp = null;
    	try {
			CallingContext<CommunicatorUser> context = userManager.findCallingContext(securityIdentity);
			CommunicatorUser recipient = context.getEffectiveUser();
        	Long mid = UrnHelper.getId(Message.URN_PREFIX, messageId);
			publisherService.updateAcknowledgment(recipient, mid, Instant.now());
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response removeAcknowledgement(String xDelegator, String messageId) {
    	Response rsp = null;
    	try {
			CallingContext<CommunicatorUser> context = userManager.findCallingContext(securityIdentity);
			CommunicatorUser recipient = context.getEffectiveUser();
        	Long mid = UrnHelper.getId(Message.URN_PREFIX, messageId);
			publisherService.updateAcknowledgment(recipient, mid, null);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response getMessage(String xDelegator, String messageId) {
    	Response rsp = null;
    	try {
			CallingContext<CommunicatorUser> context = userManager.findCallingContext(securityIdentity);
			CommunicatorUser me = context.getEffectiveUser();
        	Long mid = UrnHelper.getId(Message.URN_PREFIX, messageId);
			Message msg = publisherService.getMessage(mid);
			if (!request.isUserInRole("admin") && !msg.isUserParticipant(me)) {
	    		throw new SecurityException("You have no access rights");
			}
			rsp = Response.ok(mapper.map(msg)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
