package eu.netmobiel.planner.test;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.common.util.Base64Url;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

public class KeycloakClientHelper {
    private AccessToken driverAccessToken;
//    private IDToken driverIDToken;
    private Subject driverSubject;
    
    public void prepareSecurity() throws Exception {
    	Properties testSetupProperties = new Properties();
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-setup.properties")){
			testSetupProperties.load(inputStream);
		}
        try (InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("keycloak-issuer.json")) {
	    	AuthzClient authzClient = AuthzClient.create(configStream);
	    	AccessTokenResponse rsp = authzClient.obtainAccessToken(testSetupProperties.getProperty("driverUsername"), testSetupProperties.getProperty("driverPassword"));
	    	driverAccessToken = parseToken(rsp.getToken(), AccessToken.class);
	//    	driverIDToken = parseToken(rsp.getIdToken(), IDToken.class);
	    	KeycloakSecurityContext ksc = new KeycloakSecurityContext(rsp.getToken(), driverAccessToken, null, null);
	    	KeycloakPrincipal<KeycloakSecurityContext> kp = new KeycloakPrincipal<>(driverAccessToken.getSubject(), ksc);
	    	Set<Principal> principals = new HashSet<>();
	    	principals.add(kp);
	    	// FIXME Should also add roles.
	    	driverSubject = new Subject(true, principals, Collections.emptySet(), Collections.emptySet());
        }
    }

    public AccessToken getDriverAccessToken() {
		return driverAccessToken;
	}

	public Subject getDriverSubject() {
		return driverSubject;
	}

	// Just decode token without any verifications
    private static <T> T parseToken(String encoded, Class<T> clazz) throws IOException {
        if (encoded == null)
            return null;

        String[] parts = encoded.split("\\.");
        if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("Parsing error");

        byte[] bytes = Base64Url.decode(parts[1]);
        return JsonSerialization.readValue(bytes, clazz);
    }


}
