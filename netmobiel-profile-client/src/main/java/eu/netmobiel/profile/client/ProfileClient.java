package eu.netmobiel.profile.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.api.ProfilesApi;
import eu.netmobiel.profile.api.model.FirebaseMessagingToken;
import eu.netmobiel.profile.api.model.Profile;
import eu.netmobiel.profile.api.model.ProfileResponse;

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

    private AccessTokenResponse profileAccessTokenResponse;
    private StampedLock lock = new StampedLock();
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
	 * @return a service account  access token
	 */
	protected String getServiceAccountAccessToken() {
		long stamp = lock.readLock();
		try {
			if (profileAccessTokenResponse == null || profileTokenExpiration == null ||
				Instant.now().isAfter(profileTokenExpiration.minusSeconds(PROFILE_TIMEOUT_SLACK_SECS))) {
				stamp = lock.tryConvertToWriteLock(stamp);
	            if (stamp == 0L) {
	                log.warn("Could not convert to write lock");
	                stamp = lock.writeLock();
	            }
	            if (log.isDebugEnabled()) {
	            	log.debug("Acquire service account access token");
	            }
	        	AuthzClient authzClient = AuthzClient.create(profileServiceAccount);
	        	profileAccessTokenResponse = authzClient.obtainAccessToken();
	            profileTokenExpiration = Instant.now().plusSeconds(profileAccessTokenResponse.getExpiresIn());
			}
		} finally {
			lock.unlock(stamp);
		}
		return profileAccessTokenResponse.getToken();
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
		long stamp = lock.writeLock();
		try {
	    	profileAccessTokenResponse = null;
	        profileTokenExpiration = null;
		} finally {
			lock.unlock(stamp);
		}
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
        FirebaseMessagingToken result = null;
		try (Response response =  api.getFcmToken(managedIdentity)) {
			if (response.getStatusInfo() != Response.Status.OK) {
				ExceptionUtil.throwExceptionFromResponse("Error retrieving data from profile service", response);
			}
	        result = response.readEntity(FirebaseMessagingToken.class);
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

	private String createCircleArgument(GeoLocation location, int radiusMeters) {
		return String.format("%f:%f:%f", location.getLatitude(), location.getLongitude(), radiusMeters / 1000.0);
	}
	
	private String createCirclesParameter(final int radius, GeoLocation... locations) {
    	return String.format("[%s]" , Stream.of(locations)
			.map(loc -> createCircleArgument(loc, radius))
			.collect(Collectors.joining(","))
			);
	}
	
	/**
	 * Search for drivers that are eligible to drive a potential passenger to his/her destination.
	 * @param pickup the pickup location of the traveller. 
	 * @param dropOff the drop-off location of the traveller.
	 * @param driverMaxRadiusMeter The radius of the circles that limits the eligibility of the the driver 
	 * 			by demanding his living location to be in the two large circles around the pickup and drop-off location. 
	 * @param driverNeighbouringRadiusMeter The radius of the circles that limits the eligibility of the the driver 
	 * 			by demanding his living location to be in the neighbourhood of the pickup or drop-off location.
	 * @return A list of profiles of potential drivers, possibly empty.
	 * @throws BusinessException In case of trouble.
	 */
    public List<Profile> searchShoutOutProfiles(String accessToken, GeoLocation pickup, GeoLocation dropOff, int driverMaxRadiusMeter, int driverNeighbouringRadiusMeter) throws BusinessException {
    	if (pickup == null || dropOff == null ) {
    		throw new IllegalArgumentException("searchShoutOutProfiles: pickup and dropOff are mandatory parameters");
    	}
    	String withinAllCircles = createCirclesParameter(driverMaxRadiusMeter, pickup, dropOff);
    	String withinAnyCircles = createCirclesParameter(driverNeighbouringRadiusMeter, pickup, dropOff);
    	ResteasyWebTarget target = client.target(profileServiceUrl)
    			.register(new AddAuthHeadersRequestFilter(accessToken));
    	ProfilesApi api = target.proxy(ProfilesApi.class);
        ProfileResponse result = null;
		try (Response response =  api.searchShoutOutDrivers(withinAnyCircles, withinAllCircles)) {
			if (response.getStatusInfo() != Response.Status.OK) {
				ExceptionUtil.throwExceptionFromResponse("Error retrieving data from profile service", response);
			}
	        result = response.readEntity(ProfileResponse.class);
		}
        return result.getProfiles();
    }

    public List<Profile> searchShoutOutProfiles(GeoLocation pickup, GeoLocation dropOff, int driverMaxRadiusMeter, int driverNeighbouringRadiusMeter) throws BusinessException {
    	return searchShoutOutProfiles(getServiceAccountAccessToken(), pickup, dropOff, driverMaxRadiusMeter, driverNeighbouringRadiusMeter);
    }

}
