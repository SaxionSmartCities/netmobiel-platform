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
import eu.netmobiel.profile.api.model.DelegationActivation;
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

	/**
	 * Determines the real user of the call in case 'me'is used.
	 * @param profileId the profile id. If 'me' is used then the real user id is taken.
	 * @return the resolved user id (a keycloak managed identity).
	 */
    protected String resolveIdentity(String profileId) {
		String mid = null;
		if ("me".equals(profileId)) {
			mid = request.getUserPrincipal().getName();
		} else {
			mid = profileId;
		}
		return mid;
    }

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
			String me = request.getUserPrincipal().getName();
			Cursor cursor = new Cursor(maxResults, offset);
			// Replace 'me' by the caller.
			delegate = resolveIdentity(delegate);
			delegator = resolveIdentity(delegator);
			if (!request.isUserInRole("admin")) {
				if (delegator != null && !delegator.equals(me)) {
					throw new SecurityException("You have no privilege to list delegations for this delegator: " + delegator);
				}
				if (delegate != null && !delegate.equals(me)) {
					throw new SecurityException("You have no privilege to list delegations for this delegate: " + delegate);
				}
				if (delegate == null && delegator == null) {
					if (request.isUserInRole("delegate")) {
						delegate = me;
					} else {
						delegator = me;
					}
				}
			}
			DelegationFilter filter = new DelegationFilter(mapper.mapProfileRef(delegate), mapper.mapProfileRef(delegator), since, until, Boolean.TRUE.equals(inactiveToo));
			filter.setSortDir(sortDir);
	    	PagedResult<Delegation> results = delegationManager.listDelegations(filter, cursor, Delegation.PROFILES_ENTITY_GRAPH);
			rsp = Response.ok(mapper.mapWithPublicProfiles(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response createDelegation(eu.netmobiel.profile.api.model.Delegation apiDelegation) {
    	Response rsp = null;
		try {
			Delegation delegation = mapper.mapApi(apiDelegation);
			// Admins must set parameters explicitly
			if (!request.isUserInRole("admin")) {
				if (!request.isUserInRole("delegate")) {
					throw new SecurityException("You have no privilege to create a delegation");
				} 
				// Delegate role may set the delegate parameter, but only to refer to themselves
				String me = request.getUserPrincipal().getName();
				if (delegation.getDelegate() == null) {
					delegation.setDelegate(new Profile(me));
				} else {
					Profile delegateProf = delegationManager.resolveProfile(delegation.getDelegate());
					if (!me.equals(delegateProf.getManagedIdentity())) {
						throw new SecurityException("You have no privilege to create a delegation for: " + delegateProf.getManagedIdentity());
					}
				}
			}
	    	Long id = delegationManager.createDelegation(delegation, request.isUserInRole("admin"));
			rsp = Response.created(UriBuilder.fromResource(DelegationsApi.class)
					.path(DelegationsApi.class.getMethod("getDelegation", String.class)).build(id)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException | NoSuchMethodException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	/**
	 * Transfer a delegation from one delegate (me) to another.
	 * @returns A response with the location header set to the new delegation. This new delegation cannot be read by the caller,
	 * unless it is an admin.
	 */
	@Override
	public Response transferDelegation(String fromDelegationRef, eu.netmobiel.profile.api.model.Delegation apiToDelegation) {
    	Response rsp = null;
		try {
			// Check the security rules for the current delegation
			Long fromDelId = resolveDelegationRef(fromDelegationRef);
			Delegation fromDelegation = delegationManager.getDelegation(fromDelId, Delegation.PROFILES_ENTITY_GRAPH);
			if (!request.isUserInRole("admin")) {
				String me = request.getUserPrincipal().getName();
				if (! me.equals(fromDelegation.getDelegate().getManagedIdentity()) && !me.equals(fromDelegation.getDelegator().getManagedIdentity())) {
					throw new SecurityException("You have no privilege to transfer this delegation: " + fromDelegationRef);
				}
			}
			if (!request.isUserInRole("admin")) {
				if (!request.isUserInRole("delegate")) {
					throw new SecurityException("You have no privilege to transfer a delegation");
				}
				// For now no check if new delegate holds role delegate. If not, the prospected delegate cannot activate.
			}
			Delegation toDelegation = mapper.mapApi(apiToDelegation);
			if (toDelegation.getDelegator() != null) {
				throw new BadRequestException("The specification of a delegator in a transfer delegation is not allowed");
			}
			if (toDelegation.getDelegate() == null) {
				throw new BadRequestException("The specification of a delegate is mandatory");
			}
			if (toDelegation.getDelegate().equals(fromDelegation.getDelegate())) {
				throw new BadRequestException("A transfer to the same delegate is not allowed");
			}
	    	Long id = delegationManager.transferDelegation(fromDelegation.getId(), toDelegation, request.isUserInRole("admin"));
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
			if (!request.isUserInRole("admin")) {
				String me = request.getUserPrincipal().getName();
				if (! me.equals(delegation.getDelegate().getManagedIdentity()) && !me.equals(delegation.getDelegator().getManagedIdentity())) {
					throw new SecurityException("You have no privilege to inspect this delegation: " + delegationRef);
				}
			}
   			rsp = Response.ok(mapper.mapWithPublicProfiles(delegation)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response updateDelegation(String delegationRef) {
		Response rsp = null;
		try {
			Long id = resolveDelegationRef(delegationRef);
			Delegation delegation = delegationManager.getDelegation(id, Delegation.PROFILES_ENTITY_GRAPH);
			if (!request.isUserInRole("admin")) {
				String me = request.getUserPrincipal().getName();
				if (! me.equals(delegation.getDelegate().getManagedIdentity()) && !me.equals(delegation.getDelegator().getManagedIdentity())) {
					throw new SecurityException("You have no privilege to update this delegation: " + delegationRef);
				}
			}
			delegationManager.updateDelegation(id);
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	/**
	 * Compares the given activation code with the required code. The delegation is activated in case a match is found.
	 * You cannot activate if you are not the delegate (or the delegator), unless you are an admin.
	 * @param delegationRef the delegation id.
	 * @param delegationActivation the object containing the activation code.
	 */
	@Override
	public Response activateDelegation(String delegationRef, DelegationActivation delegationActivation) {
		String activationCode = delegationActivation.getActivationCode();
		Response rsp = null;
		try {
			Long id = resolveDelegationRef(delegationRef);
			Delegation delegation = delegationManager.getDelegation(id, Delegation.PROFILES_ENTITY_GRAPH);
			if (!request.isUserInRole("admin")) {
				String me = request.getUserPrincipal().getName();
				if (! me.equals(delegation.getDelegate().getManagedIdentity()) && !me.equals(delegation.getDelegator().getManagedIdentity())) {
					throw new SecurityException("You have no privilege to activate this delegation: " + delegationRef);
				}
			}
			delegationManager.activateDelegation(id, activationCode);
			rsp = Response.noContent().build();
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

			Delegation delegation = delegationManager.getDelegation(id, Delegation.PROFILES_ENTITY_GRAPH);
			if (!request.isUserInRole("admin")) {
				String me = request.getUserPrincipal().getName();
				if (! me.equals(delegation.getDelegate().getManagedIdentity()) && !me.equals(delegation.getDelegator().getManagedIdentity())) {
					throw new SecurityException("You have no privilege to remove this delegation: " + delegationRef);
				}
			}
			delegationManager.removeDelegation(id);
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

}
