package eu.netmobiel.profile.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.api.ComplimentsApi;
import eu.netmobiel.profile.api.ProfilesApi;
import eu.netmobiel.profile.api.ReviewsApi;
import eu.netmobiel.profile.api.model.Compliment;
import eu.netmobiel.profile.api.model.ComplimentResponse;
import eu.netmobiel.profile.api.model.FirebaseTokenResponse;
import eu.netmobiel.profile.api.model.Profile;
import eu.netmobiel.profile.api.model.ProfileResponse;
import eu.netmobiel.profile.api.model.Review;
import eu.netmobiel.profile.api.model.ReviewResponse;

@ApplicationScoped
@Logging
public class ProfileClient {
	/**
	 * Start acquiring a new token token when expiration is this near
	 */
	private static final int PROFILE_TIMEOUT_SLACK_SECS = 10;

	@Inject
    private Logger log;

    @Resource(lookup = "java:global/profileService/baseUrl")
    private String profileServiceUrl; 

    /**
     * The path to the service account file for the Profile Service.
     */
    @Resource(lookup = "java:global/profileService/serviceAccountPath")
    private String profileServiceAccountPath;

    private ResteasyClient client;
    private Configuration profileServiceAccount;

    private String profileAccessToken;
    private Instant profileTokenExpiration;
    
    /**
     * Initializes the ResetEasy client and the profile service account credentials. 
     */
    @PostConstruct
    void initialize() {
		client = new ResteasyClientBuilder()
				.connectionPoolSize(200)
				.connectionCheckoutTimeout(5, TimeUnit.SECONDS)
				.maxPooledPerRoute(20)
				.register(new Jackson2ObjectMapperContextResolver())
				.property("resteasy.preferJacksonOverJsonB", true)
				.build();

		try (final InputStream configStream = Files.newInputStream(Paths.get(profileServiceAccountPath))) {
			profileServiceAccount = JsonSerialization.readValue(configStream, Configuration.class);
    	} catch (IOException ex) {
    		throw new SystemException("Unable to read profile service account configuration", ex);
		}
    }

	@PreDestroy
	void cleanup() {
		client.close();
	}
	
	public static class AddAuthHeadersRequestFilter implements ClientRequestFilter {
		private String token;
		
	    public AddAuthHeadersRequestFilter(String token) {
	        this.token = token;
	    }

	    @Override
	    public void filter(ClientRequestContext requestContext) throws IOException {
	    	if (token != null) {
	    		requestContext.getHeaders().add("Authorization", "Bearer " + token);
	    	}
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
		String token = profileAccessToken;
		Instant expiration = profileTokenExpiration;
		if (token == null || expiration == null ||
			Instant.now().isAfter(expiration.minusSeconds(PROFILE_TIMEOUT_SLACK_SECS))) {
            if (log.isDebugEnabled()) {
            	log.debug("Acquire service account access token");
            }
        	AuthzClient authzClient = AuthzClient.create(profileServiceAccount);
        	AccessTokenResponse rsp = authzClient.obtainAccessToken();
        	token = rsp.getToken();
            expiration = Instant.now().plusSeconds(rsp.getExpiresIn());
        	profileAccessToken = token;
            profileTokenExpiration = expiration; 
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
    	AuthzClient authzClient = AuthzClient.create(profileServiceAccount);
    	return authzClient.obtainAccessToken(user, password);
	}
	
	/**
	 * For testing purposes: Clears the profile service token. 
	 */
	protected void clearToken() {
    	profileAccessToken = null;
        profileTokenExpiration = null;
	}

	public String getFirebaseToken(String managedIdentity) throws BusinessException {
		return getFirebaseToken(getServiceAccountAccessToken(), managedIdentity);
	}
	
	public String getFirebaseToken(String accessToken, String managedIdentity) throws BusinessException {
    	if (managedIdentity == null || managedIdentity.trim().length() < 1) {
    		throw new IllegalArgumentException("getFirebaseToken: managedIdentity is a mandatory parameter");
    	}
    	ResteasyWebTarget target = client.target(profileServiceUrl)
    			.register(new AddAuthHeadersRequestFilter(accessToken));
        ProfilesApi api = target.proxy(ProfilesApi.class);
        FirebaseTokenResponse result = null;
		try (Response response =  api.getFcmToken(managedIdentity)) {
			if (response.getStatusInfo() != Response.Status.OK) {
				ExceptionUtil.throwExceptionFromResponse("Error retrieving data from profile service", response);
			}
	        result = response.readEntity(FirebaseTokenResponse.class);
	        if (result.getFcmToken() == null || result.getFcmToken().isEmpty()) {
	        	throw new NotFoundException("Profile has no FCM token");
	        }
		}
        return result.getFcmToken();
    }

    public Profile getProfile(String managedIdentity) throws BusinessException {
    	return getProfile(getServiceAccountAccessToken(), managedIdentity);
    }

    public Profile getProfile(String accessToken, String managedIdentity) throws BusinessException {
    	if (managedIdentity == null || managedIdentity.trim().length() < 1) {
    		throw new IllegalArgumentException("getProfile: managedIdentity is a mandatory parameter");
    	}
    	ResteasyWebTarget target = client.target(profileServiceUrl)
    			.register(new AddAuthHeadersRequestFilter(accessToken));
        ProfilesApi api = target.proxy(ProfilesApi.class);
        ProfileResponse result = null;
        Profile profile= null;
		try (Response response =  api.getProfile(managedIdentity)) {
			if (response.getStatusInfo() != Response.Status.OK) {
				ExceptionUtil.throwExceptionFromResponse("Error retrieving data from profile service", response);
			}
	        result = response.readEntity(ProfileResponse.class);
	        if (result.getProfiles().isEmpty()) {
	        	throw new NotFoundException("Profile does not exist");
	        }
	        profile = result.getProfiles().get(0);
		}
        return profile;
    }

	public Instant getProfileTokenExpiration() {
		return profileTokenExpiration;
	}

    /**
     * Get all compliments.
     * @param accessToken The token to use.
     * @return A list of compliments.
     * @throws BusinessException
     */
    public List<Compliment> getAllCompliments(String accessToken) throws BusinessException {
    	ResteasyWebTarget target = client.target(profileServiceUrl)
    			.register(new AddAuthHeadersRequestFilter(accessToken));
        ComplimentsApi api = target.proxy(ComplimentsApi.class);
        ComplimentResponse result = null;
		try (Response response =  api.getCompliments(null, null)) {
			if (response.getStatusInfo() != Response.Status.OK) {
				ExceptionUtil.throwExceptionFromResponse("Error retrieving data from profile service", response);
			}
	        result = response.readEntity(ComplimentResponse.class);
		}
        return result.getCompliments();
    }

    public List<Compliment> getAllCompliments() throws BusinessException {
    	return getAllCompliments(getServiceAccountAccessToken());
    }

    /**
     * Get all compliments.
     * @param accessToken The token to use.
     * @return A list of compliments.
     * @throws BusinessException
     */
    public List<Review> getAllReviews(String accessToken) throws BusinessException {
    	ResteasyWebTarget target = client.target(profileServiceUrl)
    			.register(new AddAuthHeadersRequestFilter(accessToken));
        ReviewsApi api = target.proxy(ReviewsApi.class);
        ReviewResponse result = null;
		try (Response response =  api.getReviews(null, null)) {
			if (response.getStatusInfo() != Response.Status.OK) {
				ExceptionUtil.throwExceptionFromResponse("Error retrieving data from profile service", response);
			}
	        result = response.readEntity(ReviewResponse.class);
		}
        return result.getReviews();
    }

    public List<Review> getAllReviews() throws BusinessException {
    	return getAllReviews(getServiceAccountAccessToken());
    }
}
