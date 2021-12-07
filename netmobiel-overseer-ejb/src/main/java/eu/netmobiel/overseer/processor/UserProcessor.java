package eu.netmobiel.overseer.processor;

import java.util.Optional;

import javax.annotation.security.RunAs;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.commons.annotation.Updated;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.service.CommunicatorUserManager;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.service.RideshareUserManager;

/**
 * Stateless bean for the management of the high-level user handling, involving multiple modules.
 *  
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
@RunAs("system") 
public class UserProcessor {
    @Inject
    private BankerUserManager bankerUserManager;
    @Inject
    private CommunicatorUserManager communicatorUserManager;
    @Inject
    private PlannerUserManager plannerUserManager;
    @Inject
    private RideshareUserManager rideshareUserManager;
    
    public void onUserUpdated(@Observes(during = TransactionPhase.IN_PROGRESS) @Updated NetMobielUser nbuser) 
    		throws BusinessException {
		Optional<BankerUser> buser = bankerUserManager.findByManagedIdentity(nbuser.getManagedIdentity());
		if (buser.isPresent()) {
			// Update banker user
			bankerUserManager.updateUser(buser.get().getId(), nbuser);
		}
		buser = null;
		Optional<CommunicatorUser> cuser = communicatorUserManager.findByManagedIdentity(nbuser.getManagedIdentity());
		if (cuser.isPresent()) {
			// Update Communicator user
			communicatorUserManager.updateUser(cuser.get().getId(), nbuser);
		}
		cuser = null;
		Optional<PlannerUser> puser = plannerUserManager.findByManagedIdentity(nbuser.getManagedIdentity());
		if (puser.isPresent()) {
			// Update Planner user
			plannerUserManager.updateUser(puser.get().getId(), nbuser);
		}
		puser = null;
		Optional<RideshareUser> rsuser = rideshareUserManager.findByManagedIdentity(nbuser.getManagedIdentity());
		if (rsuser.isPresent()) {
			// Update Rideshare user
			rideshareUserManager.updateUser(rsuser.get().getId(), nbuser);
		}
		rsuser = null;
    }
}
