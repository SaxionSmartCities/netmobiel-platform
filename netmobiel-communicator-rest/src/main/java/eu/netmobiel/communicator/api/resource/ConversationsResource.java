package eu.netmobiel.communicator.api.resource;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.google.common.base.Objects;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.api.ConversationsApi;
import eu.netmobiel.communicator.api.mapping.ConversationMapper;
import eu.netmobiel.communicator.api.mapping.MessageMapper;
import eu.netmobiel.communicator.api.mapping.PageMapper;
import eu.netmobiel.communicator.filter.MessageFilter;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.CommunicatorUserManager;
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

	@Inject
    private CommunicatorUserManager userManager;

    @Inject
	private SecurityIdentity securityIdentity;

	@Context
	private HttpServletRequest request;


	@Override
	public Response createConversation(String xDelegator,
			eu.netmobiel.communicator.api.model.Conversation apiConversation) {
    	Response rsp = null;
		// The owner of the trip, the traveller, will be the effective user.
		try {
			if (!request.isUserInRole("admin")) {
				throw new SecurityException("You don't have the privilege to create a conversation");
			}
			CallingContext<CommunicatorUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser traveller = context.getEffectiveUser();
			Conversation conversation = conversationMapper.map(apiConversation);
			conversation.setOwner(traveller);
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
			Conversation conv = null;
        	Long cid = UrnHelper.getId(Conversation.URN_PREFIX, conversationId);
			conv = publisherService.getConversation(cid);
			CallingContext<CommunicatorUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(request, context, conv.getOwner());
			rsp = Response.ok(conversationMapper.mapComplete(conv)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response listConversations(String xDelegator, String context, String owner, OffsetDateTime since, OffsetDateTime until,
			String select, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Conversation> result = null;
		try {
			String me = securityIdentity.getEffectivePrincipal().getName();
			if (owner == null || "me".equals(owner)) {
				owner = me;
			}
			if (!request.isUserInRole("admin") && !me.equals(me)) {
				throw new SecurityException("You don't have the privilege to list conversations of someone else");
			}
			if (since != null || until != null) {
				throw new UnsupportedOperationException("Parameters 'since' and 'until' are not yet supported");
			}
			boolean actualOnly = false;
			boolean archivedOnly = false;
			if (Objects.equal(select,  "ACTUAL")) {
				actualOnly = true;
			} else if (Objects.equal(select,  "ARCHIVED")) {
				archivedOnly = true;
			}
			result = publisherService.listConversations(context, owner, actualOnly, archivedOnly, maxResults, offset); 
			if (!request.isUserInRole("admin")) {
				// If I am not the sender, then remove all the envelopes of other people
				for (Conversation c : result.getData()) {
					removeOtherRecipients(me, c.getRecentMessage());
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
        	allowAdminOrEffectiveUser(request, context, conv.getOwner());

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
        	allowAdminOrEffectiveUser(request, context, conv.getOwner());

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

}
