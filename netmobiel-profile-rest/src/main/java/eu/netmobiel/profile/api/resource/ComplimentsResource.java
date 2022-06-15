package eu.netmobiel.profile.api.resource;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.profile.api.ComplimentsApi;
import eu.netmobiel.profile.api.mapping.ComplimentMapper;
import eu.netmobiel.profile.api.model.ComplimentTypesResponse;
import eu.netmobiel.profile.filter.ComplimentsFilter;
import eu.netmobiel.profile.model.ComplimentType;
import eu.netmobiel.profile.model.Compliments;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.ComplimentManager;

@RequestScoped
public class ComplimentsResource extends BasicResource implements ComplimentsApi {

	@Inject
	private ComplimentManager complimentManager;

	@Inject
	private ComplimentMapper mapper;

    private void validateInput(Compliments c) throws BadRequestException, NotFoundException {
		if (c.getContext() == null) {
			throw new BadRequestException("Compliment context is a mandatory parameter");
		} 
		if (c.getReceiver() == null) {
			throw new BadRequestException("Compliment receiver is a mandatory parameter");
		} 
		String me = securityIdentity.getEffectivePrincipal().getName();
		if (me.equals(c.getReceiver().getManagedIdentity())) {
			throw new BadRequestException("You cannot compliment yourself");
		}
		final boolean privileged = isAdmin();
		if (c.getSender() != null ) {
			if (! privileged) {
				if (!me.equals(c.getSender().getManagedIdentity())) {
					throw new SecurityException("You have no privilege to assign a compliment on behalf of this user: " + c.getSender().getManagedIdentity());
				}
			}
		} else {
			c.setSender(new Profile(me));
		}
		// Only admin can set published time.
		if (!privileged || c.getPublished() == null) {
			c.setPublished(Instant.now());
		}
    }

	@Override
	public Response createCompliment(String xDelegator, eu.netmobiel.profile.api.model.Compliments complimentSet) {
		ResponseBuilder rspb = null;
		try {
			Compliments c = mapper.map(complimentSet);
			validateInput(c);
			Optional<Compliments> currentCs = complimentManager.lookupComplimentSet(c.getReceiver().getManagedIdentity(), c.getContext());
			Long id = null;
			if (currentCs.isEmpty()) {
				id = complimentManager.createCompliments(c);
			} else {
				id = currentCs.get().getId();
				complimentManager.updateCompliments(id, c);
			}
			String urn = UrnHelper.createUrn(Compliments.URN_PREFIX, id);
			rspb = Response.created(URI.create(urn));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rspb.build();
	}

	@Override
	public Response getCompliments(String xDelegator, String senderId, String receiverId, String context) {
		Response rsp = null;
		try {
			Cursor cursor = new Cursor(100, 0);
			ComplimentsFilter filter = new ComplimentsFilter(resolveIdentity(xDelegator, receiverId), resolveIdentity(xDelegator, senderId));
			filter.setContext(context);
			// Anyone can see the compliments for someone
	    	PagedResult<Compliments> results = complimentManager.listCompliments(filter, cursor);
			rsp = Response.ok(mapper.map(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getCompliment(String xDelegator, String complimentId) {
		Response rsp = null;
		try {
	    	Compliments c = complimentManager.getCompliment(Long.parseLong(complimentId));
			validateInput(c);
			String me = securityIdentity.getEffectivePrincipal().getName();
			if (! isAdmin() && !c.getReceiver().getManagedIdentity().equals(me) && !c.getSender().getManagedIdentity().equals(me)) {
				throw new SecurityException("You have no privilege to inspect a compliment that is not made by you or paid to you");
			}
	    	rsp = Response.ok(mapper.map(c)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response updateCompliment(String xDelegator, String complimentId, eu.netmobiel.profile.api.model.Compliments complimentSet) {
		Response rsp = null;
		try {
			Long cid = Long.parseLong(complimentId);
	    	Compliments c = complimentManager.getCompliment(cid);
	    	validateInput(c);
	    	c = mapper.map(complimentSet);
			complimentManager.updateCompliments(cid, c);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response deleteCompliment(String xDelegator, String complimentId) {
		Response rsp = null;
		try {
	    	Compliments c = complimentManager.getCompliment(Long.parseLong(complimentId));
			String me = securityIdentity.getEffectivePrincipal().getName();
			if (! isAdmin() && !c.getSender().getManagedIdentity().equals(me)) {
				throw new SecurityException("You have no privilege to remove a compliment that is not made by you");
			}
	    	complimentManager.removeCompliment(Long.parseLong(complimentId));
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getComplimentTypes() {
		ComplimentTypesResponse ctr = new ComplimentTypesResponse();
		List<eu.netmobiel.profile.api.model.ComplimentType> types = Stream.of(ComplimentType.values())
				.map(ct -> mapper.map(ct))
				.collect(Collectors.toList());
		ctr.getComplimentTypes().addAll(types);
		return Response.ok(ctr).build();
	}

}
