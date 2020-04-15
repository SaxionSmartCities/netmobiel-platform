package eu.netmobiel.rideshare.rest.security;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.security.ISecurityContext;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.service.UserManager;

/**
 * The security context to store user info. The security token is created by CDI (request scope) and completed 
 * by the keycloak security filter. Inject an ISecurityContext object to use it elsewhere.
 */
@RequestScoped
public class SecurityContext implements ISecurityContext {
    
    @Inject
    private UserManager userManager;

    private User user;
    
    private String token;
    
    /**
     * Constructor.
     */
    public SecurityContext() {
    }
    
    /**
     * @param user
     */
    public void setUser(User user) {
        this.user = user;
    }
    
    @Override
    public User getCurrentUser() {
        return user;
    }

    /**
     * @return the token
     */
    @Override
	public String getToken() {
        return token;
    }

    /**
     * @param token the token to set
     */
    public void setToken(String token) {
        this.token = token;
    }

    @Override
	public void registerUser() {
        try {
        	if (user.getId() == null) {
        		user = userManager.register(user);
        	}
        } catch (Exception ex) {
        	throw new SecurityException("Unable to register or lookup user " + (user != null ? user.toString() : "<null>"), ex);
        }
    }

    @Override
	public boolean loadUser() {
        try {
            user = userManager.find(user);
        } catch (Exception ex) {
        	throw new SecurityException("Unable to lookup user " + (user != null ? user.toString() : "<null>"), ex);
        }
        return isUserDefined();
    }

    @Override
	public void checkOwnership(NetMobielUser owner, String objectName) {
    	if (getCurrentUser().getId() == null) {
    		loadUser();
    	}
    	if (getCurrentUser().getManagedIdentity() == null || ! owner.getManagedIdentity().equals(getCurrentUser().getManagedIdentity())) {
    		throw new SecurityException(objectName + " is not owned by calling user");
    	}
    }

	@Override
	public boolean isUserDefined() {
		return getCurrentUser().getId() != null;
	}

	public String toString() {
		return user != null ? user.toString() : "<null>";
	}
}
