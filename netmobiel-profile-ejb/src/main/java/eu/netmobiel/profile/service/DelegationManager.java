package eu.netmobiel.profile.service;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.TokenGenerator;
import eu.netmobiel.profile.event.DelegationActivationConfirmedEvent;
import eu.netmobiel.profile.event.DelegationActivationRequestedEvent;
import eu.netmobiel.profile.event.DelegationRevokedEvent;
import eu.netmobiel.profile.event.DelegationTransferCompletedEvent;
import eu.netmobiel.profile.event.DelegationTransferRequestedEvent;
import eu.netmobiel.profile.filter.DelegationFilter;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.repository.DelegationDao;
import eu.netmobiel.profile.repository.KeycloakDao;
import eu.netmobiel.profile.repository.ProfileDao;

/**
 * Stateless EJB for handling of delegations.  
 * The security is handled in the web layer.
 */
@Stateless
@Logging
public class DelegationManager {
	public static final Integer MAX_RESULTS = 10; 
	
	
	@Resource(lookup = "java:global/profileService/delegateActivationCodeTTL")
	private Integer delegationActivationCodeTTL;

	@SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private DelegationDao delegationDao;

    @Inject
    private ProfileDao profileDao;
    
    @Inject
    private KeycloakDao keycloakDao;

    @Inject
    private Event<DelegationTransferRequestedEvent> delegationTransferRequestedEvent;
    @Inject
    private Event<DelegationTransferCompletedEvent> delegationTransferCompletedEvent;
    @Inject
    private Event<DelegationActivationRequestedEvent> delegationActivationRequestedEvent;
    @Inject
    private Event<DelegationActivationConfirmedEvent> delegationActivationConfirmedEvent;
    @Inject
    private Event<DelegationRevokedEvent> delegationRevokedEvent;

	public Profile resolveProfile(Profile p) throws NotFoundException, BadRequestException {
		if (p == null) {
			return null;
		}
		Profile pdb = null;
		if (p.getId() != null) {
			pdb = profileDao.find(p.getId()).orElseThrow(() -> new NotFoundException("No such profile: " + p.getId()));
		} else if (p.getManagedIdentity() != null) {
			pdb = profileDao.findByManagedIdentity(p.getManagedIdentity()).orElseThrow(() -> new NotFoundException("No such profile: " + p.getManagedIdentity()));
		} else {
			throw new BadRequestException("Profile has no identifier: " + p);
		}
		return pdb;
	}

	private void sendActivationCode(Delegation delegation) throws BusinessException {
		// Send the activation code to the delegator
		delegation.setActivationCode(TokenGenerator.createRandomNumber(6));
		delegation.setActivationCodeTTL(delegationActivationCodeTTL);
    	EventFireWrapper.fire(delegationActivationRequestedEvent, new DelegationActivationRequestedEvent(delegation));
    	// NOTE: the delegation object is (potentially) modified by the handler!  
	}

	private void confirmActivation(Delegation delegation) throws BusinessException {
    	EventFireWrapper.fire(delegationActivationConfirmedEvent, new DelegationActivationConfirmedEvent(delegation));
	}
	
	public @NotNull PagedResult<Delegation> listDelegations(DelegationFilter filter, Cursor cursor, String graphName) throws BadRequestException, NotFoundException {
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
    		results = delegationDao.loadGraphs(pids.getData(), graphName, Delegation::getId);
    	}
    	return new PagedResult<>(results, cursor, prs.getTotalCount());
	}

	public Long createDelegation(Delegation delegation, boolean acceptNow) throws BusinessException {
    	  // Validate required parameters.
		if (delegation.getDelegate() == null || delegation.getDelegator() == null) { 
			throw new BadRequestException("delegator and delegate are mandatory attributes");
		}
		delegation.setDelegate(resolveProfile(delegation.getDelegate()));
		delegation.setDelegator(resolveProfile(delegation.getDelegator()));
		delegation.setSubmissionTime(Instant.now());
		// Constraint: Delegation cannot overlap in time
		// --> Currently no active delegation (between two parties)
		if (delegationDao.isDelegationActive(delegation.getDelegate(), delegation.getDelegator(), null)) {
			throw new BadRequestException("Delegation relation already present");
		}
		delegationDao.save(delegation);
		if (acceptNow) {
	    	activateDelegation(delegation, true);
		} else {
			sendActivationCode(delegation);
		}
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
     * Processes a transfer of a delegation from one delegate to another. The process is almost equals to the createDelegation,
     * except that now the current delegate creates a delegation for someone else.  
     * @param fromDelegationId
     * @param toDelegation
     * @param acceptNow if true then do not send an activation cod, but switch immediately.
     * @return the id of the new delegate
     * @throws BusinessException
     */
	public Long transferDelegation(Long fromDelegationId, Delegation toDelegation, boolean acceptNow) throws BusinessException {
		Delegation fromDelegation = delegationDao.find(fromDelegationId)
    			.orElseThrow(() -> new NotFoundException("No such delegation: " + fromDelegationId));
		toDelegation.setDelegator(fromDelegation.getDelegator());
		Long toDelegationId = createDelegation(toDelegation, acceptNow);
		toDelegation.setTransferFrom(fromDelegation);
		delegationTransferRequestedEvent.fire(new DelegationTransferRequestedEvent(fromDelegation, toDelegation, acceptNow));
		return toDelegationId;
	}

	/**
     * Attempts to match the activation code ands activates a delegation if a match is found.
     * @param id the delegation
     * @param code the input activation code.
     * @throws BusinessException
     */
    public void activateDelegation(Long id, String code) throws BusinessException {
    	Delegation delegation = delegationDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such delegation: " + id));
    	if (delegation.getRevocationTime() != null) {
    		throw new UpdateException("The delegation has already been revoked");
    	}
    	if (delegation.getActivationTime() != null) {
    		throw new UpdateException("The delegation has already been activated");
    	}
    	if (delegation.hasActivationCodeExpired(Instant.now())) {
    		throw new BadRequestException("The activation code has expired");
    	}
    	if (code == null || !code.equals(delegation.getActivationCode())) {
    		throw new BadRequestException("The activation code is invalid");
    	}
    	activateDelegation(delegation, false);
    }

    private void activateDelegation(Delegation delegation, boolean immediate) throws BusinessException {
		keycloakDao.addDelegator(delegation.getDelegate(), delegation.getDelegator());
		delegation.setActivationTime(Instant.now());
		confirmActivation(delegation);
		if (delegation.getTransferFrom() != null) {
	    	EventFireWrapper.fire(delegationTransferCompletedEvent, 
	    			new DelegationTransferCompletedEvent(delegation.getTransferFrom(), delegation, immediate));
			removeDelegation(delegation.getTransferFrom().getId());
		}
    }

    /**
     * Regenerates and transmits the activation code for a delegation to the delegator.
     * It is expected that the prospected delegate might execute an update when this person did not respond in time 
     * for the invitation to take over a delegator. 
     * @param id the delegation id
     * @throws NotFoundException When the delegation is not found or when required attributes
     * 			are not present. 
     * @throws BusinessException 
     */
    public void updateDelegation(Long id) throws BusinessException {
    	Delegation delegation = delegationDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such delegation: " + id));
    	if (delegation.getRevocationTime() != null) {
    		throw new UpdateException("The delegation has already been revoked");
    	}
    	if (delegation.getActivationTime() != null) {
    		throw new UpdateException("The delegation has already been activated");
    	}
		sendActivationCode(delegation);
    }
    
	/**
	 * Removes (sort of) a delegation by setting the revocation time to 'now'.  
	 * @param id The id of the delegation object.
	 * @throws BusinessException If the object is not found or already revoked.
	 */
    public void removeDelegation(Long id) throws BusinessException {
    	Delegation delegation = delegationDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such delegation: " + id));
    	Instant now = Instant.now();
    	if (delegation.getRevocationTime() != null) {
    		throw new BadRequestException("Delegation has already been revoked");
    	}
    	delegation.setRevocationTime(now);
		keycloakDao.removeDelegator(delegation.getDelegate(), delegation.getDelegator());
    	EventFireWrapper.fire(delegationRevokedEvent, new DelegationRevokedEvent(delegation));
    }

}
