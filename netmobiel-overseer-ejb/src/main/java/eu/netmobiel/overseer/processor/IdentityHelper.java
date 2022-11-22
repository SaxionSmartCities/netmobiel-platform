package eu.netmobiel.overseer.processor;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.service.CommunicatorUserManager;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.ProfileManager;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.service.RideshareUserManager;

@ApplicationScoped
@Logging
public class IdentityHelper {

	@Inject
    private Logger log;

    @Inject
    private RideshareUserManager rideshareUserManager;
    @Inject
    private PlannerUserManager plannerUserManager;
    @Inject
    private BankerUserManager bankerUserManager;
    @Inject
    private CommunicatorUserManager communicatorUserManager;
    @Inject
    private ProfileManager profileManager;
    
    
    /**
     * Lookup a user with a urn from keycloak, or from one of the modules and resolve it.
     * @param userRef a urn
     * @return A NetMobielUser object embedded in an Optional. If not found the Optional is empty.
     * @throws BadRequestException on error.
     */
    public Optional<NetMobielUser> resolveUserUrn(String userRef) throws BadRequestException {
    	NetMobielUser user = null;
    	if (UrnHelper.isUrn(userRef)) {
    		try {
    			NetMobielModule module = NetMobielModule.getEnum(UrnHelper.getService(userRef));
    			switch (module) {
    			case KEYCLOAK:
    				user = profileManager.getFlatProfileByManagedIdentity(UrnHelper.getIdAsString(NetMobielUser.KEYCLOAK_URN_PREFIX, userRef));
    				break;
    			case BANKER:
	        		user = bankerUserManager.getUser(UrnHelper.getId(BankerUser.URN_PREFIX, userRef));
    				break;
    			case COMMUNICATOR:
	        		user = communicatorUserManager.getUser(UrnHelper.getId(CommunicatorUser.URN_PREFIX, userRef));
    				break;
    			case PLANNER:
	        		user = plannerUserManager.getUser(UrnHelper.getId(PlannerUser.URN_PREFIX, userRef));
    				break;
    			case PROFILE:
	        		user = profileManager.getFlatProfile(UrnHelper.getId(Profile.URN_PREFIX, userRef));
    				break;
    			case RIDESHARE:
					user = rideshareUserManager.getUser(UrnHelper.getId(RideshareUser.URN_PREFIX, userRef));
    				break;
    			default:
    				throw new BadRequestException("Don't where to look for this user id: " + userRef);
    			}
			} catch (NotFoundException e) {
				// No such user
				log.warn("Cannot find this user: " + userRef);
			}
    	} else {
			throw new BadRequestException("Specify a urn for this user id: " + userRef);
    	}
    	return Optional.ofNullable(user);
    }
}
