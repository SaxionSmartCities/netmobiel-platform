package eu.netmobiel.profile.service;

import java.security.Principal;
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
		Principal me = sessionContext.getCallerPrincipal();
		boolean privileged = sessionContext.isCallerInRole("admin");
		if (compliment.getReceiver() == null) {
			throw new BadRequestException("Compliment receiver is a mandatory parameter");
		} else {
	    	Profile yourProfile = profileDao.getReferenceByManagedIdentity(compliment.getReceiver().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + compliment.getReceiver().getManagedIdentity()));
	    	compliment.setReceiver(yourProfile);
		}
		if (me.getName().equals(compliment.getReceiver().getManagedIdentity())) {
			throw new BadRequestException("You cannot compliment yourself");
		}
		if (compliment.getSender() == null) {
	    	Profile myProfile = profileDao.getReferenceByManagedIdentity(me.getName())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + me.getName()));
	    	compliment.setSender(myProfile);
		} else if (!me.getName().equals(compliment.getSender().getManagedIdentity()) && !privileged) {
			new SecurityException("You have no privilege to assign a compliment on behalf of someone else");
		}
		if (compliment.getCompliment() == null) {
			throw new BadRequestException("Compliment is a mandatory parameter");
		}
		complimentDao.save(compliment);
		return compliment.getId();
	}

	public @NotNull PagedResult<Compliment> listCompliments(ComplimentFilter filter, Cursor cursor) throws BadRequestException {
		cursor.validate(MAX_RESULTS, 0);
		Principal me = sessionContext.getCallerPrincipal();
		boolean privileged = sessionContext.isCallerInRole("admin");
		if (! privileged && filter.getReceiver() != null && !filter.getReceiver().equals(me.getName())) {
			new SecurityException("You have no privilege to list compliments received by someone else");
		}
		if (! privileged && filter.getSender() != null && !filter.getSender().equals(me.getName())) {
			new SecurityException("You have no privilege to list compliments sent by someone else");
		}
		if (! privileged && filter.getReceiver() == null) {
			filter.setReceiver(me.getName());
		}
		filter.validate();
    	PagedResult<Long> prs = complimentDao.listCompliments(filter, Cursor.COUNTING_CURSOR);
    	List<Compliment> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = complimentDao.listCompliments(filter, cursor);
    		results = complimentDao.loadGraphs(pids.getData(), Compliment.LIST_COMPLIMENTS_ENTITY_GRAPH, Compliment::getId);
    	}
    	return new PagedResult<Compliment>(results, cursor, prs.getTotalCount());
	}

	public Compliment getCompliment(Long complimentId) throws NotFoundException {
		return complimentDao.find(complimentId)
				.orElseThrow(() -> new NotFoundException("No such compliment: " + complimentId));
	}

    @RolesAllowed({ "admin" })
	public void removeCompliment(Long complimentId) throws NotFoundException {
		Compliment c = complimentDao.find(complimentId)
				.orElseThrow(() -> new NotFoundException("No such compliment: " + complimentId));
		complimentDao.remove(c);
	}

}
