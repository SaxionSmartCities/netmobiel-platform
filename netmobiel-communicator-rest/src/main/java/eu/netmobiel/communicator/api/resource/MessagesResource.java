package eu.netmobiel.communicator.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.security.SecurityIdentity;
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

    @Inject
	private SecurityIdentity securityIdentity;

	@Context
	private HttpServletRequest request;

    @Override
	public Response sendMessage(String xDelegator, eu.netmobiel.communicator.api.model.Message msg) {
    	Response rsp = null;
		try {
			CallingContext<CommunicatorUser> callingContext = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser sender = callingContext.getEffectiveUser();
			Message message = mapper.map(msg);
			if (msg.getSender() != null && !request.isUserInRole("admin")) {
				throw new SecurityException("You have no privilege to specify a sender");
			}
			message.setSender(sender);
			Long mid = publisherService.chat(sender, message);
			if (!callingContext.getCallingUser().equals(sender)) {
				publisherService.informDelegates(sender, "Persoonlijk bericht van " + sender.getName(), DeliveryMode.ALL);
			}
			String newMsgUrn = UrnHelper.createUrn(Message.URN_PREFIX, mid);
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newMsgUrn)).build() ;
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response listMessages(String xDelegator, String participant, String context, 
			OffsetDateTime since, OffsetDateTime until, String deliveryMode, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Message> result = null;
		try {
			String me = securityIdentity.getEffectivePrincipal().getName();
			if ("me".equals(participant)) {
				participant = me;
			}
			if (participant == null && !request.isUserInRole("admin")) {
				participant = me;
			}  
			if (!me.equals(participant) && !request.isUserInRole("admin")) {
				throw new SecurityException("You don't have the privilege the messages of someone else");
			}
			MessageFilter filter = new MessageFilter(participant, since, until, context, SortDirection.DESC.name());
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
					removeOtherRecipients(me, msg);
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
