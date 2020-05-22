package eu.netmobiel.rideshare.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.keycloak.adapters.jaas.AbstractKeycloakLoginModule;
import org.keycloak.adapters.jaas.BearerTokenLoginModule;
import org.keycloak.adapters.jaas.DirectAccessGrantsLoginModule;

public class LoginContextFactory {

    private File keycloakConfigFile;

    public LoginContextFactory(String name) {
    	try {
//	    	InputStream resource = MethodHandles.lookup().lookupClass().getResourceAsStream(name);
	    	InputStream resource  = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	    	keycloakConfigFile = File.createTempFile("keycloak", null);
	    	keycloakConfigFile.deleteOnExit();
	    	copyContent(resource, keycloakConfigFile);
//	    	URL resource  = Thread.currentThread().getContextClassLoader().getResource(name);
//	    	keycloakConfigFile = new File(resource.toURI());
    	} catch (Exception ex) {
    		throw new RuntimeException(ex);
    	}
    }
    
    private static void copyContent(InputStream input, File output) throws IOException {
        try (InputStream inputStream = input) {
            try (FileOutputStream outputStream = new FileOutputStream(output)) {
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                outputStream.write(buffer);
            }
        }
    }

    public LoginContext createDirectGrantLoginContext(String username, String password, String scope) throws LoginException {
        return new LoginContext("other", new Subject(),
                createJaasCallbackHandler(username, password),
                createJaasConfigurationForDirectGrant(scope));
    }

    public LoginContext createBearerLoginContext(String accesstToken) throws LoginException {
        return new LoginContext("does-not-matter", null,
                createJaasCallbackHandler("does-not-matter", accesstToken),
                createJaasConfigurationForBearer());
    }
    
    private CallbackHandler createJaasCallbackHandler(final String principal, final String password) {
        return new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        ((NameCallback) callback).setName(principal);
                    } else if (callback instanceof PasswordCallback) {
                        ((PasswordCallback) callback).setPassword(password.toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(callback, "Unsupported callback: " + callback.getClass().getCanonicalName());
                    }
                }
            }
        };
    }


    private Configuration createJaasConfigurationForDirectGrant(String scope) {
        return new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, Object> options = new HashMap<>();
                options.put(AbstractKeycloakLoginModule.KEYCLOAK_CONFIG_FILE_OPTION, keycloakConfigFile.getAbsolutePath());
                if (scope != null) {
                    options.put(DirectAccessGrantsLoginModule.SCOPE_OPTION, scope);
                }
                AppConfigurationEntry LMConfiguration = new AppConfigurationEntry(DirectAccessGrantsLoginModule.class.getName(), 
                		AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                return new AppConfigurationEntry[] { LMConfiguration };
            }
        };
    }


    private Configuration createJaasConfigurationForBearer() {
        return new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, Object> options = new HashMap<>();
                options.put(AbstractKeycloakLoginModule.KEYCLOAK_CONFIG_FILE_OPTION, keycloakConfigFile.getAbsolutePath());

                AppConfigurationEntry LMConfiguration = new AppConfigurationEntry(BearerTokenLoginModule.class.getName(), 
                		AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                return new AppConfigurationEntry[] { LMConfiguration };
            }
        };
    }

}
