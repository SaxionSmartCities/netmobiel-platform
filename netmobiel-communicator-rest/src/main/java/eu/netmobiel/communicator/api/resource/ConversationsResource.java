package eu.netmobiel.communicator.api.resource;

import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.google.common.base.Objects;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.api.ConversationsApi;
import eu.netmobiel.communicator.api.mapping.ConversationMapper;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.service.CommunicatorUserManager;
import eu.netmobiel.communicator.service.PublisherService;
@RequestScoped
public class ConversationsResource extends CommunicatorResource implements ConversationsApi {

	@Inject
	private ConversationMapper mapper;

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
				throw new SecurityException("You have no privilege to create a conversation");
			}
			CallingContext<CommunicatorUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser traveller = context.getEffectiveUser();
			Conversation conversation = mapper.map(apiConversation);
			conversation.setOwner(traveller);
			String newConversationId = UrnHelper.createUrn(Conversation.URN_PREFIX, publisherService.createConversation(conversation));
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newConversationId)).build();
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
			if (! UrnHelper.isUrn(conversationId) || UrnHelper.matchesPrefix(Conversation.URN_PREFIX, conversationId)) {
	        	Long tid = UrnHelper.getId(Conversation.URN_PREFIX, conversationId);
				conv = publisherService.getConversation(tid);
			} else {
				throw new BadRequestException("Don't understand urn: " + conversationId);
			}
			CallingContext<CommunicatorUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
        	allowAdminOrEffectiveUser(request, context, conv.getOwner());
			rsp = Response.ok(mapper.map(conv)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response listConversations(String xDelegator, String owner, OffsetDateTime since, OffsetDateTime until,
			String select, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Conversation> result = null;
		try {
			if (owner == null && !request.isUserInRole("admin")) {
				owner = securityIdentity.getEffectivePrincipal().getName();
			} else if ("me".equals(owner)) {
				owner = securityIdentity.getEffectivePrincipal().getName();
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
			result = publisherService.listConversations(owner, actualOnly, archivedOnly, maxResults, offset); 
			rsp = Response.ok(mapper.map(result)).build();
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
        	Conversation ride = mapper.map(conversation);
			ride.setId(cid);
			publisherService.updateConversation(ride);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	@Override
	public Response listConversationMessages(String xDelegator, String conversationId, String deliveryMode,
			Integer maxResults, Integer offset) {
		// TODO Auto-generated method stub
		return null;
	}

}
