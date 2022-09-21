package eu.netmobiel.planner.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

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
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.TransportOperator;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.mapping.TompBookingMapper;
import eu.netmobiel.tomp.api.model.AssetType;
import eu.netmobiel.tomp.api.model.Coordinates;
import eu.netmobiel.tomp.api.model.Place;
import eu.netmobiel.tomp.api.model.Planning;
import eu.netmobiel.tomp.api.model.PlanningRequest;
import eu.netmobiel.tomp.api.model.Traveler;
import eu.netmobiel.tomp.client.ApiClient;
import eu.netmobiel.tomp.client.ApiException;
import eu.netmobiel.tomp.client.impl.OperatorInformationApi;
import eu.netmobiel.tomp.client.impl.PlanningApi;

/**
 * Access class for the TOMP Transport Operator. This Dao can target multiple transport operators.   
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
public class TransportOperatorApiDao {
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
    
    /**
     * The path to the service account file. Abuse the profiel service account for this. We look at some time in the future for a real solution.
     */
    @Resource(lookup = "java:global/profileService/serviceAccountPath")
    private String serviceAccountPath;

    private Configuration keycloakServiceAccount;

    private String keycloakAccessToken;
    private Instant keycloakTokenExpiration;

    @Inject
    private TompBookingMapper mapper;
    
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
	
	public Instant getProfileTokenExpiration() {
		return keycloakTokenExpiration;
	}

	/**
	 * For testing purposes: Clears the profile service token. 
	 */
	protected void clearToken() {
    	keycloakAccessToken = null;
        keycloakTokenExpiration = null;
	}

	private static Exception createException(ApiException ex) {
		Exception exception;
		if (ex.getCode() == 400) {
			exception = new BadRequestException(ex);
		} else if (ex.getCode() == 403) {
			exception = new SecurityException(ex);
		} else if (ex.getCode() == 404) {
			exception = new NotFoundException(ex);
		} else {
			exception = new SystemException(ex);
		}
		return exception; 
	}
	
	private void prepareApiClient(TransportOperator operator, ApiClient client) {
		client.setBasePath(operator.getBaseUrl());
		client.setAccessToken(getServiceAccountAccessToken());
		client.setUserAgent(USER_AGENT);
		
	}
	
	public List<AssetType> getAvailableAssets(TransportOperator operator) throws Exception {
		OperatorInformationApi api = new OperatorInformationApi();
		prepareApiClient(operator, api.getApiClient());
		try {
			return api.operatorAvailableAssetsGet(TOMP_LANGUAGE, TOMP_API, TOMP_API_VERSION, TOMP_MY_MAAS_ID, null, null, null, null, null);
		} catch (ApiException e) {
			throw createException(e);
		}
	}

	private static Place createPlace(GeoLocation loc) {
		Place p = new Place();
		p.setName(loc.getLabel());
		p.setCoordinates(new Coordinates());
		p.getCoordinates().setLat(loc.getLatitude().floatValue());
		p.getCoordinates().setLng(loc.getLongitude().floatValue());
		return p;
	}
	
	public List<Itinerary> requestPlanningInquiry(TransportOperator operator, TripPlan plan, GeoLocation from, GeoLocation to) throws Exception {
		PlanningApi api = new PlanningApi();
		prepareApiClient(operator, api.getApiClient());
		PlanningRequest pr = new PlanningRequest();
		pr.setDepartureTime(plan.getEarliestDepartureTime().atOffset(ZoneOffset.UTC));
		pr.setArrivalTime(plan.getLatestArrivalTime().atOffset(ZoneOffset.UTC));
		pr.setNrOfTravelers(plan.getNrSeats());
		pr.setFrom(createPlace(plan.getFrom()));
		pr.setTo(createPlace(plan.getTo()));
		pr.setRadius(plan.getMaxWalkDistance());
		// The traveller is added to filter the rides. For booking it is not yet required.
		Traveler trav = new Traveler();
		trav.setIsValidated(Boolean.TRUE);
		trav.setKnownIdentifier(plan.getTraveller().getManagedIdentity());
		trav.setKnownIdentifierProvider("Netmobiel Keycloak");
		pr.addTravelersItem(trav);
		try {
			Planning planning = api.planningInquiriesPost(TOMP_LANGUAGE, TOMP_API, TOMP_API_VERSION, TOMP_MY_MAAS_ID, pr, null);
			return mapper.mapToItineraries(planning.getOptions(), operator, plan);
			
		} catch (ApiException e) {
			throw createException(e);
		}
	}

}
