package eu.netmobiel.profile.service;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.TokenGenerator;
import eu.netmobiel.profile.filter.DelegationFilter;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.repository.DelegationDao;
import eu.netmobiel.profile.repository.KeycloakDao;
import eu.netmobiel.profile.repository.ProfileDao;

/**
 * Stateless EJB for handling of delegations. 
 */
@Stateless
@Logging
@DeclareRoles({ "admin" })
@RolesAllowed({ "admin" })
public class DelegationManager {
	public static final Integer MAX_RESULTS = 10; 

	@Resource
    private SessionContext sessionContext;

    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private DelegationDao delegationDao;

    @Inject
    private ProfileDao profileDao;
    
    @Inject
    private KeycloakDao keycloakDao;

    public DelegationManager() {
    }

	private Profile resolveProfile(Profile p) throws NotFoundException, BadRequestException {
		if (p == null) {
			return null;
		}
		Profile pdb = null;
		if (p.getId() != null) {
			pdb = profileDao.find(p.getId()).orElseThrow(() -> new NotFoundException("No such profile: " + p.getId()));
		} else if (p.getManagedIdentity() != null) {
			pdb = profileDao.findByManagedIdentity(p.getManagedIdentity()).orElseThrow(() -> new NotFoundException("No such profile: " + p.getId()));
		} else {
			throw new BadRequestException("Profile has no identifier: " + p);
		}
		return pdb;
	}

	public @NotNull PagedResult<Delegation> listDelegations(DelegationFilter filter, Cursor cursor) throws BadRequestException, NotFoundException {
    	// As an optimisation we could first call the data. If less then maxResults are received, we can deduce the totalCount and thus omit
    	// the additional call to determine the totalCount.
    	// For now don't do conditional things. First always total count, then data if data is requested. 
    	// Get the total count
   		filter.setDelegate(resolveProfile(filter.getDelegate()));
   		filter.setDelegator(resolveProfile(filter.getDelegator()));
		filter.validate();
    	cursor.validate(MAX_RESULTS, 0);
    	PagedResult<Long> prs = delegationDao.listDelegations(filter, Cursor.COUNTING_CURSOR);
    	List<Delegation> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = delegationDao.listDelegations(filter, cursor);
    		results = delegationDao.loadGraphs(pids.getData(), Delegation.DEFAULT_ENTITY_GRAPH, Delegation::getId);
    	}
    	return new PagedResult<Delegation>(results, cursor, prs.getTotalCount());
	}

	public Long createDelegation(Delegation delegation, boolean acceptNow) throws BusinessException {
    	  // Validate required parameters.
		if (delegation.getDelegate() == null || delegation.getDelegator() == null) { 
			throw new BadRequestException("startTime, delegatorRef and delegateRef are mandatory attributes");
		}
		delegation.setDelegate(resolveProfile(delegation.getDelegate()));
		delegation.setDelegator(resolveProfile(delegation.getDelegator()));
		delegation.setSubmissionTime(Instant.now());
		delegation.setTransferCode(TokenGenerator.createRandomNumber(6));
		if (acceptNow) {
			delegation.setActivationTime(delegation.getSubmissionTime());
		}
		// Constraint: Delegation cannot overlap in time
		// --> Currently no active delegation (between two parties)
		if (delegationDao.isDelegationActive(delegation.getDelegate(), delegation.getDelegator())) {
			throw new BadRequestException("Delegation relation already present");
		}
		delegationDao.save(delegation);
		keycloakDao.addDelegator(delegation.getDelegate(), delegation.getDelegator());
		return delegation.getId();
    }

    /**
     * Retrieves the delegation object. 
     * @param id primary key of the delegation
     * @param graphName  the name of the named entity graph to use.
     * @return The delegation object
     * @throws NotFoundException if not found
     */
    public Delegation getDelegation(Long id, String graphName) throws NotFoundException {
    	return delegationDao.loadGraph(id, graphName)
    			.orElseThrow(() -> new NotFoundException("No such delegation: " + id));
    }

	/**
	 * Removes (sort of) a delegation by setting the revocation time to 'now'.  
	 * @param id The id of the delegation object.
	 * @throws NotFoundException If the object is not found.
	 * @throws BadRequestException When the delegation is already revoked.
	 */
    public void removeDelegation(Long id) throws NotFoundException, BadRequestException {
    	Delegation delegation = delegationDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such delegation: " + id));
    	Instant now = Instant.now();
    	if (delegation.getRevocationTime() != null) {
    		throw new BadRequestException("Delegation has already been revoked");
    	}
    	delegation.setRevocationTime(now);
		keycloakDao.removeDelegator(delegation.getDelegate(), delegation.getDelegator());
    }

}
