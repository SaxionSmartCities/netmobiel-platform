package eu.netmobiel.planner.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.planner.model.TransportOperator;
import eu.netmobiel.tomp.api.model.Planning;
import eu.netmobiel.tomp.api.model.PlanningRequest;
import eu.netmobiel.tomp.client.ApiClient;
import eu.netmobiel.tomp.client.ApiException;
import eu.netmobiel.tomp.client.impl.PlanningApi;

/**
 * Access class for the TOMP Transport Operator. This Dao can target multiple transport operators.   
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
public class TransportOperatorDao {
	/**
	 * Start acquiring a new token token when expiration is this near
	 */
	private static final int ACCESS_TOKEN_TIMEOUT_SLACK_SECS = 10;
	private static final String TOMP_LANGUAGE = "nl-NL";
	private static final String TOMP_API = "Netmobiel-TOMP";
	private static final String TOMP_API_VERSION = "1.3.0";
	private static final String TOMP_MY_MAAS_ID = "Saxion Netmobiel";
	private static final String USER_AGENT = "Saxion Netmobiel Planner";

	@Inject
    private Logger log;
    
    @Resource(lookup = "java:global/TOMP/TO/rideshare/baseUrl")
    private String rideshareTOUrl; 

    /**
     * The path to the service account file for the Profile Service.
     */
    @Resource(lookup = "java:global/TOMP/TO/serviceAccountPath")
    private String serviceAccountPath;

    private Configuration keycloakServiceAccount;

    private String keycloakAccessToken;
    private Instant keycloakTokenExpiration;

    private 
    /**
     * Initializes the API client and the service account credentials. 
     */
    @PostConstruct
    void initialize() {
		try (final InputStream configStream = Files.newInputStream(Paths.get(serviceAccountPath))) {
			keycloakServiceAccount = JsonSerialization.readValue(configStream, Configuration.class);
    	} catch (IOException ex) {
    		throw new SystemException("Unable to read service account configuration", ex);
		}
    }

	/**
	 * Acquires a profile service account token, but only when the current access token is near expiration.
	 * This method may be accessed concurrently, we don't mind.
	 * @return a service account  access token
	 */
	protected String getServiceAccountAccessToken() {
		// Get local copy to protect against concurrent access.
		// Could also use synchronize, not sure how EJB reacts. StampedLock behaved badly. 
		String token = keycloakAccessToken;
		Instant expiration = keycloakTokenExpiration;
		if (token == null || expiration == null ||
			Instant.now().isAfter(expiration.minusSeconds(ACCESS_TOKEN_TIMEOUT_SLACK_SECS))) {
            if (log.isDebugEnabled()) {
            	log.debug("Acquire service account access token");
            }
        	AuthzClient authzClient = AuthzClient.create(keycloakServiceAccount);
        	AccessTokenResponse rsp = authzClient.obtainAccessToken();
        	token = rsp.getToken();
            expiration = Instant.now().plusSeconds(rsp.getExpiresIn());
        	keycloakAccessToken = token;
            keycloakTokenExpiration = expiration; 
		}
		return token;
	}

	/**
	 * Helper method for obtaining an access token for the profile service.
	 * @param user the user name
	 * @param password the password
	 * @return An access token
	 */
	public AccessTokenResponse getAccessToken(String user, String password) {
    	AuthzClient authzClient = AuthzClient.create(keycloakServiceAccount);
    	return authzClient.obtainAccessToken(user, password);
	}
	
	/**
	 * For testing purposes: Clears the profile service token. 
	 */
	protected void clearToken() {
    	keycloakAccessToken = null;
        keycloakTokenExpiration = null;
	}

	private static void handleApiException(ApiException ex) throws BusinessException {
		if (ex.getCode() == 400) {
			throw new BadRequestException(ex);
		}
		if (ex.getCode() == 403) {
			throw new SecurityException(ex);
		}
		if (ex.getCode() == 404) {
			throw new NotFoundException(ex);
		}
		throw new SystemException(ex);
	}
	
	private void prepareApiClient(TransportOperator operator, ApiClient client) {
		client.setBasePath(operator.getBaseUrl());
		client.setAccessToken(getServiceAccountAccessToken());
		client.setUserAgent(USER_AGENT);
		
	}
	public Planning searchInquiry(TransportOperator operator, PlanningRequest body) throws BusinessException {
		PlanningApi api = new PlanningApi();
		prepareApiClient(operator, api.getApiClient());
		Planning planning = null;
		try {
			planning = api.planningInquiriesPost(TOMP_LANGUAGE, TOMP_API, TOMP_API_VERSION, TOMP_MY_MAAS_ID, body, null);
		} catch (ApiException e) {
			handleApiException(e);
		}
		return planning;
	}

	public Instant getProfileTokenExpiration() {
		return keycloakTokenExpiration;
	}

}
