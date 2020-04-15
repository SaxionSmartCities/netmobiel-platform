package eu.netmobiel.commons.security;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * A security context for the current request.
 */
public interface ISecurityContext {

    /**
     * Returns the User information for the currently authenticated user. The user is already registered in the database.
     */
    public NetMobielUser getCurrentUser();

    /**
     * Check if user is defined
     */
    public boolean isUserDefined();

    /**
     * Returns the token used to authenticate.
     */
    public String getToken();

    /**
     * Verifies the ownership of <code>objectName</code>, owned by <code>owner</code> equals the calling user.
     * 
     * @param owner The owner of the object
     * @param objectName The name of the object.
     */
    void checkOwnership(NetMobielUser owner, String objectName);
    

    /**
     * Registers the calling user in the (local) database. 
     */
    void registerUser();

    /**
     * Loads the user from the database. Return true if the user is known.
     */
    boolean loadUser();
}