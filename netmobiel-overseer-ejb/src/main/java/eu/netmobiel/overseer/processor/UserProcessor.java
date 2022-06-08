package eu.netmobiel.overseer.processor;

import javax.annotation.security.RunAs;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Updated;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.service.CommunicatorUserManager;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.profile.model.Profile;
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
    
    public void syncAllUserDatabases(Profile profile) {
    	bankerUserManager.registerOrUpdateUser(profile);
    	communicatorUserManager.registerOrUpdateUser(profile, profile.getPhoneNumber(), profile.getDefaultCountry());
    	plannerUserManager.registerOrUpdateUser(profile);
    	// A passenger is also known in the rideshare
       	rideshareUserManager.registerOrUpdateUser(profile);
    }
    
    public void onUserCreation(@Observes(during = TransactionPhase.IN_PROGRESS) @Created Profile profile) {
    	syncAllUserDatabases(profile);
    }

    public void onUserUpdated(@Observes(during = TransactionPhase.IN_PROGRESS) @Updated Profile profile) {
    	syncAllUserDatabases(profile);
    }
}
