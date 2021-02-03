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
import eu.netmobiel.here.search.model.ErrorResponse;
import eu.netmobiel.here.search.model.OpenSearchReverseGeocodeResponse;

@ApplicationScoped
//@Logging
public class HereSearchClient {
    @Inject
    private Logger log;

    @Resource(lookup = "java:global/geocode/hereApiKey")
    private String hereApiKey;

//    private static final String hereSearchAutoSuggestUrl = "https://autosuggest.search.hereapi.com/v1/autosuggest"; 
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
				ErrorResponse rsp = response.readEntity(ErrorResponse.class);
				throw new WebApplicationException(rsp.toString(), rsp.status);
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
    	if (rsp.items.length > 0) {
    		if (rsp.items.length > 1) {
    			log.warn("Multiple results on reverse geocoding of: " + location.toString());
    		}
    		if (rsp.items[0].address != null) {
    			pc = rsp.items[0].address.postalCode;
    			if (pc != null) {
    				pc = pc.replaceAll(" ", "");
    			}
    		}
    	}
    	return pc;
    }

}
