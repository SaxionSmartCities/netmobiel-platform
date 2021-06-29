package eu.netmobiel.here.search;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.here.search.api.model.ErrorResponse;
import eu.netmobiel.here.search.api.model.OpenSearchAutosuggestResponse;
import eu.netmobiel.here.search.api.model.OpenSearchReverseGeocodeResponse;

@ApplicationScoped
//@Logging
public class HereSearchClient {
    @Inject
    private Logger log;

    @Resource(lookup = "java:global/geocode/hereApiKey")
    private String hereApiKey;

    private static final String hereSearchAutoSuggestUrl = "https://autosuggest.search.hereapi.com/v1/autosuggest"; 
//    private static final String hereSearchBrowseUrl = "https://browse.search.hereapi.com/v1/browse"; 
//    private static final String hereSearchDiscoverUrl = "https://discover.search.hereapi.com/v1/discover"; 
//    private static final String hereSearchGeocodeUrl = "https://geocode.search.hereapi.com/v1/geocode"; 
//    private static final String hereSearchLookupUrl = "https://lookup.search.hereapi.com/v1/lookup"; 
    private static final String hereSearchReverseGeocodeUrl = "https://revgeocode.search.hereapi.com/v1/revgeocode"; 

    
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
	

	/**
	 * Formats the error response a bit different from the generated version.
	 * @param err the error response
	 * @return A string containing the title, cause and action fields.
	 */
	private static String formatErrorResponse(ErrorResponse err) {
		return String.format("HERE: %s caused by %s - %s", err.getTitle(), err.getCause(), err.getAction());
	}

	/**
	 * Returns reverse geocoding result from the HERE Search and Discovery Api.
	 * @param location The location to lookup
	 * @param language Optional. The language to be used for result rendering from a list of BCP 47 compliant language codes. Default: nl-NL.
	 * @return
	 * @see https://developer.here.com/documentation/places/dev_guide/topics_api/resource-autosuggest.html
	 */
    public OpenSearchReverseGeocodeResponse getReverseGeocode(GeoLocation location, String language) {
    	if (language == null) {
    		language = "nl-NL";
    	}
    	if (location == null) {
    		throw new IllegalArgumentException("getReverseGeocode: location is a mandatory parameter");
    	}
		WebTarget target = client.target(hereSearchReverseGeocodeUrl)
			.queryParam("at", String.format((Locale)null,"%f,%f", location.getLatitude(), location.getLongitude()))
			.queryParam("lang", language)
			.queryParam("apiKey", hereApiKey);
		
		OpenSearchReverseGeocodeResponse result = null;
		if (log.isDebugEnabled()) {
			log.debug("Reverse geocode: " + target.getUri().toString());
		}
		try (Response response = target.request(MediaType.APPLICATION_JSON).get()) {
			if (response.getStatusInfo() != Response.Status.OK) {
				ErrorResponse ersp = response.readEntity(ErrorResponse.class);
				String errmsg = ersp.getTitle() != null ? formatErrorResponse(ersp) : response.getStatusInfo().toString(); 
				throw new WebApplicationException(errmsg, response.getStatus());
			}
	        result = response.readEntity(OpenSearchReverseGeocodeResponse.class);
		}
        return result;
    }

    /**
     * Retrieves the postal code 6 (1234XY) of a location. 
     * @param location a GeoLocation object.
     * @return The postal code associated with this location.
     */
    public String getPostalCode6(GeoLocation location) {
    	OpenSearchReverseGeocodeResponse rsp = getReverseGeocode(location, null);
    	String pc = null;
    	if (rsp.getItems().size() > 0) {
    		if (rsp.getItems().size() > 1) {
    			log.warn("Multiple results on reverse geocoding of: " + location.toString());
    		}
    		if (rsp.getItems().get(0).getAddress() != null) {
    			pc = rsp.getItems().get(0).getAddress().getPostalCode();
    			if (pc != null) {
    				pc = pc.replace(" ", "");
    			}
    		}
    	}
    	return pc;
    }

    /* ===================================  AUTOSUGGEST  =================================== */
    
	/**
	 * Returns autosuggestions from the HERE Search Api. The AutoSuggest categoryQuery and chainQuery results are not supported due to issues with 
	 * the mapping of the OpenAPI spec by openapi-generator.
	 * @param query the query string.
	 * @param centre centre point of a circle as latitude and longitude. The circle limits the search area.
	 * @param radius the radius around the centre point.
	 * @param language Optional. The language to be used for result rendering from a list of BCP 47 compliant language codes. Default: nl-NL.
	 * @param details If set to true then include address details in the result.
	 * @param maxResults Optional. The maximum number of result items in the collection.
	 * @return 
	 * @see https://developer.here.com/documentation/places/dev_guide/topics_api/resource-autosuggest.html
	 */
    public OpenSearchAutosuggestResponse listAutosuggestions(String query, GeoLocation centre, Integer radius, String language, Boolean details, Integer maxResults) {
    	if (query == null || query.trim().length() < 1) {
    		throw new IllegalArgumentException("listAutoSuggestions: query is a mandatory parameter, size > 0");
    	}
    	if (centre == null || radius == null) {
    		throw new IllegalArgumentException("listAutoSuggestions: centre and radius are mandatory parameters.");
    	}
    	if (language == null) {
    		language = "nl-NL";
    	}
    	if (maxResults == null) {
    		maxResults = 20;
    	}
		WebTarget target = client.target(hereSearchAutoSuggestUrl)
			.queryParam("q", query)
			.queryParam("in", String.format((Locale)null, "circle:%f,%f;r=%d", centre.getLatitude(), centre.getLongitude(), radius))
			.queryParam("lang", language)
			.queryParam("limit", maxResults)
			.queryParam("apiKey", hereApiKey);
		if (Boolean.TRUE.equals(details)) {
			target = target.queryParam("show", "details");
		}
		OpenSearchAutosuggestResponse result = null;
		if (log.isDebugEnabled()) {
			log.debug("Autosuggest: " + target.getUri().toString());
		}
		try (Response response = target.request(MediaType.APPLICATION_JSON).get()) {
			if (response.getStatusInfo() != Response.Status.OK) {
				ErrorResponse ersp = response.readEntity(ErrorResponse.class);
				String errmsg = ersp.getTitle() != null ? formatErrorResponse(ersp) : response.getStatusInfo().toString(); 
				throw new WebApplicationException(errmsg, response.getStatus());
			}
	        result = response.readEntity(OpenSearchAutosuggestResponse.class);
		}
        return result;
    }
}
