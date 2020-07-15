package eu.netmobiel.rideshare.service;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.UserDao;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@ApplicationScoped
@Logging
public class IdentityHelper {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Inject
    private UserDao userDao;
    
    
    public Optional<User> resolveUrn(String userRef) {
    	User user = null;
    	if (UrnHelper.isUrn(userRef)) {
        	NetMobielModule module = NetMobielModule.getEnum(UrnHelper.getService(userRef));
        	if (module == NetMobielModule.PLANNER) {
    			Long did = RideshareUrnHelper.getId(User.URN_PREFIX, userRef);
        		user = userDao.find(did).orElse(null);
        	} else if (module == NetMobielModule.KEYCLOAK) {
        		String managedIdentity = UrnHelper.getSuffix(userRef);
        		user = userDao.findByManagedIdentity(managedIdentity).orElse(null);
        	}
    	} else {
			Long did = UrnHelper.getId(userRef);
    		user = userDao.find(did).orElse(null);
    	}
    	return Optional.ofNullable(user);
    }
}
