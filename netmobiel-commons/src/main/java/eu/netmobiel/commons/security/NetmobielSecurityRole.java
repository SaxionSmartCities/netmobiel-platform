package eu.netmobiel.commons.security;

public enum NetmobielSecurityRole {
	/**
	 * The administrator, can do do everything
	 */
	ADMIN("admin"),
	/**
	 * A delegate can book trips on behalf of their delegators. 
	 */
	DELEGATE("delegate"),
	/**
	 * A treasurer can handle withdrawals and execute payment batches.
	 */
	TREASURER("treasurer");
	
	/**
	 * The name of the role in the IDentity Manager, i.e., Keycloak.  
	 */
	private String roleName;
	 
    private NetmobielSecurityRole(String code) {
        this.roleName = code;
    }
 
    public String getRoleName() {
        return roleName;
    }

}
