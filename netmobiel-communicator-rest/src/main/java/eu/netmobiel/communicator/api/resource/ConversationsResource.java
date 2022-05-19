package eu.netmobiel.communicator.api.resource;

import java.net.URI;
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
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.api.ConversationsApi;
import eu.netmobiel.communicator.api.mapping.ConversationMapper;
import eu.netmobiel.communicator.api.mapping.MessageMapper;
import eu.netmobiel.communicator.api.mapping.PageMapper;
import eu.netmobiel.communicator.filter.ConversationFilter;
import eu.netmobiel.communicator.filter.MessageFilter;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
@RequestScoped
public class ConversationsResource extends CommunicatorResource implements ConversationsApi {

	@Inject
	private PageMapper pageMapper;

	@Inject
	private ConversationMapper conversationMapper;

	@Inject
	private MessageMapper messageMapper;

	@Inject
    private PublisherService publisherService;

	@Override
	public Response createConversation(String xDelegator,
			eu.netmobiel.communicator.api.model.Conversation apiConversation) {
    	Response rsp = null;
		try {
			Conversation conversation = conversationMapper.map(apiConversation);
			CallingContext<CommunicatorUser> callingContext = userManager.findCallingContext(securityIdentity);
			CommunicatorUser owner = callingContext.getEffectiveUser();
			if (apiConversation.getOwner() != null) {
				if (!owner.getManagedIdentity().equals(apiConversation.getOwner().getManagedIdentity()) 
						&& !request.isUserInRole("admin")) {
					throw new SecurityException("You don't have the privilege to create a conversation");
				}
				owner = userManager.findOrRegisterUser(conversation.getOwner());
			}
			conversation.setOwner(owner);
			// If (the topic or user role are not present, other services are requested to provide proper data.
			String newConversationId = UrnHelper.createUrn(Conversation.URN_PREFIX, publisherService.createConversation(conversation));
			rsp = Response.created(URI.create(newConversationId)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getConversation(String xDelegator, String conversationId) {
    	Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Conversation.URN_PREFIX, conversationId);
        	Conversation conv = publisherService.getConversation(cid);
			CallingContext<CommunicatorUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(context, conv.getOwner());
			rsp = Response.ok(conversationMapper.mapComplete(conv)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response listConversations(String xDelegator, String context, String ownerId, 
			OffsetDateTime since, OffsetDateTime until, String select, String sortDir, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Conversation> result = null;
		try {
			CallingContext<CommunicatorUser> callingContext = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser owner = resolveUserReference(callingContext, ownerId);
			allowAdminOrEffectiveUser(callingContext, owner);
			ConversationFilter filter = new ConversationFilter(owner, "ACTUAL".equals(select), 
					"ARCHIVED".equals(select), since, until, context, sortDir);
			Cursor cursor = new Cursor(maxResults, offset);
			result = publisherService.listConversations(filter, cursor);
			rsp = Response.ok(pageMapper.mapShallow(result)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response listConversationsForInbox(String xDelegator, String ownerId, String select, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Conversation> result = null;
		try {
			if (ownerId == null) {
				ownerId = "me";
			}
			CallingContext<CommunicatorUser> callingContext = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser owner = resolveUserReference(callingContext, ownerId);
			allowAdminOrEffectiveUser(callingContext, owner);
			result = publisherService.listConversationsForInbox(owner, "ACTUAL".equals(select), 
					"ARCHIVED".equals(select), SortDirection.DESC, maxResults, offset);
			if (!request.isUserInRole("admin")) {
				// If I am not an admin, then remove all the envelopes of other people if i am not the sender
				for (Conversation c : result.getData()) {
					removeOtherRecipients(owner, c.getRecentMessage());
				}
			}
			rsp = Response.ok(pageMapper.mapShallow(result)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response updateConversation(String xDelegator, String conversationId,
			eu.netmobiel.communicator.api.model.Conversation conversation) {
    	Response rsp = null;
    	try {
        	Long cid = UrnHelper.getId(Conversation.URN_PREFIX, conversationId);
        	// Check whether this call is allowed
			Conversation conv = publisherService.getConversation(cid);
			CallingContext<CommunicatorUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(context, conv.getOwner());

        	conv = conversationMapper.map(conversation);
			conv.setId(cid);
			publisherService.updateConversation(conv);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response listConversationMessages(String xDelegator, String conversationId, String deliveryMode,
			String sortDir, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Message> result = null;
		try {
			Long convId = UrnHelper.getId(Conversation.URN_PREFIX, conversationId);
        	// Check whether this call is allowed
			Conversation conv = publisherService.getConversation(convId);
			CallingContext<CommunicatorUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(context, conv.getOwner());

			MessageFilter filter = new MessageFilter(convId, sortDir);
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
			rsp = Response.ok(messageMapper.map(result)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response acknowledgeConversation(String xDelegator, String conversationId) {
    	Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Conversation.URN_PREFIX, conversationId);
        	Conversation conv = publisherService.getConversation(cid);
			CallingContext<CommunicatorUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(context, conv.getOwner());
        	publisherService.acknowledgeConversation(conv);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
