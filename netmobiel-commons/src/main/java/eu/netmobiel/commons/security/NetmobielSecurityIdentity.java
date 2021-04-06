package eu.netmobiel.commons.security;

import java.security.Principal;
import java.util.Objects;

import org.keycloak.representations.AccessToken;

import eu.netmobiel.commons.model.NetMobielUser;

public class NetmobielSecurityIdentity implements SecurityIdentity {

    private final Principal principal;
    private final Principal effectivePrincipal;
    private final AccessToken token; 
    private NetMobielUser realUser; 

    public NetmobielSecurityIdentity(Principal principal, Principal effectivePrincipal, AccessToken token) {
        this.principal = principal;
        this.effectivePrincipal = effectivePrincipal;
        this.token = token;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Principal getEffectivePrincipal() {
        return effectivePrincipal;
    }

    @Override
    public AccessToken getToken() {
        return token;
    }
    
    @Override
    public boolean isDelegationActive() {
        return ! Objects.equals(principal, effectivePrincipal);
    }
    @Override
	public NetMobielUser getRealUser() {
    	if (realUser == null) {
    		realUser = SecurityIdentity.createUserFromToken(getToken()); 
    	}
    	return realUser;
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getRealUser());
		if (isDelegationActive() && effectivePrincipal != null) {
			builder.append(" (effective ");
			builder.append(effectivePrincipal.getName());
			builder.append(")");
		}
		return builder.toString();
	}

}
