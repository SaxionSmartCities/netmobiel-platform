package eu.netmobiel.profile.service;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.filter.ComplimentsFilter;
import eu.netmobiel.profile.model.Compliments;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.repository.ComplimentsDao;
import eu.netmobiel.profile.repository.ProfileDao;

/**
 * Bean class for the Compliment service.  
 */
@Stateless
@Logging
public class ComplimentManager {
	public static final Integer MAX_RESULTS = 10; 

    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    
    @Inject
    private ComplimentsDao complimentsDao;

    private void prepareCompliments(Compliments complimentSet) throws BadRequestException, NotFoundException {
		if (complimentSet.getContext() == null) {
			throw new BadRequestException("Compliment context is a mandatory parameter");
		} 
		if (complimentSet.getReceiver() == null) {
			throw new BadRequestException("Compliment receiver is a mandatory parameter");
		} 
    	Profile rcvProfile = profileDao.getReferenceByManagedIdentity(complimentSet.getReceiver().getManagedIdentity())
    			.orElseThrow(() -> new NotFoundException("No such profile: " + complimentSet.getReceiver().getManagedIdentity()));
    	complimentSet.setReceiver(rcvProfile);

    	if (complimentSet.getSender() == null) {
			throw new BadRequestException("Compliment sender is a mandatory parameter");
		}
    	Profile sndProfile = profileDao.getReferenceByManagedIdentity(complimentSet.getSender().getManagedIdentity())
    			.orElseThrow(() -> new NotFoundException("No such profile: " + complimentSet.getSender().getManagedIdentity()));
    	complimentSet.setSender(sndProfile);

    	if (rcvProfile.equals(sndProfile)) {
    		throw new BadRequestException("You cannot compliment yourself");
    	}
    }

    public Long createCompliments(Compliments complimentSet) throws BadRequestException, NotFoundException {
    	prepareCompliments(complimentSet);
		complimentsDao.save(complimentSet);
		return complimentSet.getId();
	}

	public Optional<Compliments> lookupComplimentSet(String receiverManagedIdentity, String context) {
		return complimentsDao.findComplimentSetByReceiverAndContext(receiverManagedIdentity, context);
	}

	public @NotNull PagedResult<Compliments> listCompliments(ComplimentsFilter filter, Cursor cursor) throws BadRequestException {
		cursor.validate(MAX_RESULTS, 0);
		filter.validate();
    	PagedResult<Long> prs = complimentsDao.listComplimentSets(filter, Cursor.COUNTING_CURSOR);
    	List<Compliments> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = complimentsDao.listComplimentSets(filter, cursor);
    		results = complimentsDao.loadGraphs(pids.getData(), Compliments.LIST_COMPLIMENTS_ENTITY_GRAPH, Compliments::getId);
    	}
    	return new PagedResult<>(results, cursor, prs.getTotalCount());
	}

	public void updateCompliments(Long complimentsId, Compliments complimentSet) throws NotFoundException, BadRequestException {
    	prepareCompliments(complimentSet);
    	complimentSet.setId(complimentsId);
    	complimentsDao.merge(complimentSet);
	}

	public Compliments getCompliment(Long complimentId) throws NotFoundException {
		return complimentsDao.loadGraph(complimentId, Compliments.LIST_COMPLIMENTS_ENTITY_GRAPH)
				.orElseThrow(() -> new NotFoundException("No such compliment: " + complimentId));
	}

	public void removeCompliment(Long complimentId) throws NotFoundException {
		Compliments c = complimentsDao.find(complimentId)
				.orElseThrow(() -> new NotFoundException("No such compliment: " + complimentId));
		complimentsDao.remove(c);
	}

}
