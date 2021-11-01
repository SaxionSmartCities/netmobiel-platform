package eu.netmobiel.rideshare.service;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.RideshareUserDao;

@ApplicationScoped
@Logging
public class IdentityHelper {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Inject
    private RideshareUserDao userDao;
    
    
    public Optional<RideshareUser> resolveUrn(String userRef) throws BadRequestException {
    	RideshareUser user = null;
    	if (UrnHelper.isUrn(userRef)) {
        	NetMobielModule module = NetMobielModule.getEnum(UrnHelper.getService(userRef));
        	if (module == NetMobielModule.RIDESHARE) {
    			Long did = UrnHelper.getId(RideshareUser.URN_PREFIX, userRef);
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
