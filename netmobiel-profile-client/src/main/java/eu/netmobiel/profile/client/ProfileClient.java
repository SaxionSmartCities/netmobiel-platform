package eu.netmobiel.profile.client;

import java.io.IOException;
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
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.api.ProfilesApi;
import eu.netmobiel.profile.api.model.FirebaseMessagingToken;
import eu.netmobiel.profile.api.model.Profile;
import eu.netmobiel.profile.api.model.ProfileResponse;

@ApplicationScoped
@Logging
public class ProfileClient {
    @SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Resource(lookup = "java:global/profileService/baseUrl")
    private String profileServiceUrl; 

    private ResteasyClient client;
    
	@PostConstruct
	public void createClient() {
		client = new ResteasyClientBuilder()
				.connectionPoolSize(200)
				.connectionCheckoutTimeout(5, TimeUnit.SECONDS)
				.maxPooledPerRoute(20)
				.register(new Jackson2ObjectMapperContextResolver())
				.property("resteasy.preferJacksonOverJsonB", true)
				.build();
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
	        requestContext.getHeaders().add("Authorization", "Bearer " + token);
	    }
	}

	public String getFirebaseToken(String accessToken, String managedIdentity) throws ApplicationException {
    	if (accessToken == null || accessToken.trim().length() < 1) {
    		throw new IllegalArgumentException("getFirebaseToken: accessToken is a mandatory parameter");
    	}
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

    public Profile getProfile(String accessToken, String managedIdentity) throws ApplicationException {
    	if (accessToken == null || accessToken.trim().length() < 1) {
    		throw new IllegalArgumentException("getFirebaseToken: accessToken is a mandatory parameter");
    	}
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
}
