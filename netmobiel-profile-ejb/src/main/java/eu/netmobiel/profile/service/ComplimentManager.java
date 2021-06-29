package eu.netmobiel.profile.service;

import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.filter.ComplimentFilter;
import eu.netmobiel.profile.model.Compliment;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.repository.ComplimentDao;
import eu.netmobiel.profile.repository.ProfileDao;

/**
 * Bean class for the Compliment service.  
 */
@Stateless
@Logging
@DeclareRoles({ "admin", "delegate" })
public class ComplimentManager {
	public static final Integer MAX_RESULTS = 10; 

	@Resource
    private SessionContext sessionContext;

    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    
    @Inject
    private ComplimentDao complimentDao;
    
    public ComplimentManager() {
    }

	public Long createCompliment(Compliment compliment) throws BadRequestException, NotFoundException {
		if (compliment.getReceiver() == null) {
			throw new BadRequestException("Compliment receiver is a mandatory parameter");
		} 
    	Profile rcvProfile = profileDao.getReferenceByManagedIdentity(compliment.getReceiver().getManagedIdentity())
    			.orElseThrow(() -> new NotFoundException("No such profile: " + compliment.getReceiver().getManagedIdentity()));
    	compliment.setReceiver(rcvProfile);

    	if (compliment.getSender() == null) {
			throw new BadRequestException("Compliment sender is a mandatory parameter");
		}
    	Profile sndProfile = profileDao.getReferenceByManagedIdentity(compliment.getSender().getManagedIdentity())
    			.orElseThrow(() -> new NotFoundException("No such profile: " + compliment.getSender().getManagedIdentity()));
    	compliment.setSender(sndProfile);
    	
		if (compliment.getCompliment() == null) {
			throw new BadRequestException("Compliment type is a mandatory parameter");
		}
		complimentDao.save(compliment);
		return compliment.getId();
	}

	public @NotNull PagedResult<Compliment> listCompliments(ComplimentFilter filter, Cursor cursor) throws BadRequestException {
		cursor.validate(MAX_RESULTS, 0);
		filter.validate();
    	PagedResult<Long> prs = complimentDao.listCompliments(filter, Cursor.COUNTING_CURSOR);
    	List<Compliment> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = complimentDao.listCompliments(filter, cursor);
    		results = complimentDao.loadGraphs(pids.getData(), Compliment.LIST_COMPLIMENTS_ENTITY_GRAPH, Compliment::getId);
    	}
    	return new PagedResult<>(results, cursor, prs.getTotalCount());
	}

	public Compliment getCompliment(Long complimentId) throws NotFoundException {
		return complimentDao.find(complimentId)
				.orElseThrow(() -> new NotFoundException("No such compliment: " + complimentId));
	}

	public void removeCompliment(Long complimentId) throws NotFoundException {
		Compliment c = complimentDao.find(complimentId)
				.orElseThrow(() -> new NotFoundException("No such compliment: " + complimentId));
		complimentDao.remove(c);
	}

}
