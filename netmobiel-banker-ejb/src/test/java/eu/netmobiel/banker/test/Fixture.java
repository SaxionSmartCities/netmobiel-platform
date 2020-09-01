package eu.netmobiel.banker.test;

import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.User;

public class Fixture {

	private Fixture() {
		// No instances allowed
	}

	public static User createUser(String identity, String givenName, String familyName, String email) {
		return new User(identity, givenName, familyName, email);
	}
	
	public static User createUser(LoginContext loginContext) {
        Subject subject = loginContext.getSubject();
        @SuppressWarnings("rawtypes")
		Set<KeycloakPrincipal> ps = subject.getPrincipals(KeycloakPrincipal.class);
        @SuppressWarnings("unchecked")
		KeycloakPrincipal<KeycloakSecurityContext> p = ps.iterator().next();
        return createUser(p.getKeycloakSecurityContext().getToken());
	}

	public static User createUser(AccessToken token) {
		return new User(token.getSubject(), token.getGivenName(), token.getFamilyName(), token.getEmail());
	}

	public static User createDriver1() {
		return createUser("ID1", "Carla1", "Netmobiel", null);
	}
	
	public static User createDriver2() {
		return createUser("ID2", "Carla2", "Netmobiel", null);
	}
	public static User createDriver3() {
		return createUser("ID3", "Carla3", "Netmobiel", null);
	}

	public static User createPassenger1() {
		return createUser("IP1", "Simon1", "Netmobiel", null);
	}
	
	public static User createPassenger2() {
		return createUser("IP2", "Simon2", "Netmobiel", null);
	}

    public static Account createAccount(String ncan, String name, AccountType type) {
    	Account acc = new Account();
    	acc.setAccountType(type);
    	acc.setName(name);
    	acc.setNcan(ncan);
    	return acc;
    }
    
}
