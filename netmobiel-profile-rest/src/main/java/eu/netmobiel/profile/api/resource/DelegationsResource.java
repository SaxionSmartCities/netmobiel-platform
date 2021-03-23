package eu.netmobiel.profile.api.resource;

import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.profile.api.DelegationsApi;
import eu.netmobiel.profile.api.mapping.DelegationMapper;
import eu.netmobiel.profile.filter.DelegationFilter;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.DelegationManager;

@RequestScoped
@Logging
public class DelegationsResource implements DelegationsApi {

	@Inject
	private DelegationMapper mapper;

	@Inject
	private DelegationManager delegationManager;

	@Context
	private HttpServletRequest request;

	private Long resolveDelegationRef(String delegationRef) {
    	Long id = null;
    	if (UrnHelper.isUrn(delegationRef)) {
   			id = UrnHelper.getId(Profile.URN_PREFIX, delegationRef);
    	} else {
			id = UrnHelper.getId(delegationRef);
    	}
    	return id;
	}

	@Override
	public Response getDelegations(String delegate, String delegator, OffsetDateTime since, OffsetDateTime until,
			Boolean inactiveToo, String sortDir, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			Cursor cursor = new Cursor(maxResults, offset);
			DelegationFilter filter = new DelegationFilter(mapper.mapProfileRef(delegate), mapper.mapProfileRef(delegator), since, until, Boolean.TRUE.equals(inactiveToo));
			filter.setSortDir(sortDir);
	    	PagedResult<Delegation> results = delegationManager.listDelegations(filter, cursor, Delegation.PROFILES_ENTITY_GRAPH);
			rsp = Response.ok(mapper.mapWithShallowProfiles(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response createDelegation(eu.netmobiel.profile.api.model.Delegation profile) {
    	Response rsp = null;
		try {
			Delegation domprof = mapper.map(profile);
			if (domprof.getDelegate() == null || domprof.getDelegator() == null) { 
				throw new BadRequestException("delegator (or delegateRef) and delegate (or delegatorRef) are mandatory attributes");
			}
	    	Long id = delegationManager.createDelegation(domprof, true);
			rsp = Response.created(UriBuilder.fromResource(DelegationsApi.class)
					.path(DelegationsApi.class.getMethod("getDelegation", String.class)).build(id)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException | NoSuchMethodException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getDelegation(String delegationRef) {
    	Response rsp = null;
		try {
			Long id = resolveDelegationRef(delegationRef);
			Delegation delegation = delegationManager.getDelegation(id, Delegation.PROFILES_ENTITY_GRAPH);
   			rsp = Response.ok(mapper.mapWithShallowProfiles(delegation)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response deleteDelegation(String delegationRef) {
		Response rsp = null;
		try {
			Long id = resolveDelegationRef(delegationRef);
	    	delegationManager.removeDelegation(id);
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}




}
