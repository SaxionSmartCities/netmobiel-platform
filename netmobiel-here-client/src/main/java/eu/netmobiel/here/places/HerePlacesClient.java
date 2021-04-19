package eu.netmobiel.here.places;

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
import eu.netmobiel.here.places.model.AutosuggestMediaType;
import eu.netmobiel.here.places.model.ErrorResponse;
/**
 * Old places API
 * @see https://developer.here.com/documentation/places/dev_guide/topics/what-is.html
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
public class HerePlacesClient {
    @Inject
    private Logger log;

    @Resource(lookup = "java:global/geocode/hereApiKey")
    private String hereApiKey;
    
    // This url differs from the one used with legacy appId, appCode!
    private static final String hereAppPlacesUrl = "https://places.ls.hereapi.com/places/v1"; 

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
	 * Returns autosuggestions from the HERE Places Api.
	 * @param query 
	 * @param centre centre point of a circle as latitude and longitude. The circle limits the search area.
	 * @param radius the radius around the centre point.
	 * @param resultTypes Optional. A comma-separated list of the autosuggest result types that should be included in the response. 
	 * 				Possible values are: address, place, query (undocumented). The types category and chain are documented, but no results are 
	 * 				ever returned (for Netherlands).  
	 * 				If this parameter is not set, all autosuggest types are considered for the response. 
	 * @param hls	Optional. The delimiter that should be placed before each matched token in the autosuggest response. 
	 * 				It defaults to &lt;/b&gt;. The delimiters are included in the 'highlightedTitle' and 'highlightedVicinity' fields. 
	 * @param hle  	Optional. The delimiter that should be placed after each matched token in the autosuggest response. 
	 * 				It defaults to &lt;/b&gt;. The delimiters are included in the 'highlightedTitle' and 'highlightedVicinity' fields.
	 * @param maxResults Optional. The maximum number of result items in the collection.
	 * @return
	 * @see https://developer.here.com/documentation/places/dev_guide/topics_api/resource-autosuggest.html
	 */
    public AutosuggestMediaType listAutosuggestions(String query, GeoLocation centre, 
    		Integer radius, String resultTypes, String hls, String hle, Integer maxResults) {
    	if (query == null || query.trim().length() < 1) {
    		throw new IllegalArgumentException("listAutoSuggestions: query is a mandatory parameter, size > 0");
    	}
    	if (centre == null || radius == null) {
    		throw new IllegalArgumentException("listAutoSuggestions: centre and radius are mandatory parameters.");
    	}
		WebTarget target = client.target(hereAppPlacesUrl)
			.path("autosuggest")
			.queryParam("q", query)
			.queryParam("in", String.format((Locale)null, "%f,%f;r=%d", centre.getLatitude(), centre.getLongitude(), radius));
		if (resultTypes != null && !resultTypes.isEmpty()) {
			target = target.queryParam("result_types", resultTypes);
		}
		if (hls != null) {
			target = target.queryParam("hlStart", hls);
		}
		if (hle != null) {
			target = target.queryParam("hlEnd", hle);
		}
		if (maxResults != null) {
			target = target.queryParam("size", maxResults);
		}
		target = target.queryParam("apiKey", hereApiKey);
		
		AutosuggestMediaType result = null;
		if (log.isDebugEnabled()) {
			log.debug("URI: " + target.getUri().toString());
		}
		try (Response response = target.request(MediaType.APPLICATION_JSON).get()) {
			if (response.getStatusInfo() != Response.Status.OK) {
				ErrorResponse ersp = response.readEntity(ErrorResponse.class);
				throw new WebApplicationException(ersp.toString(), ersp.status);
			}
	        result = response.readEntity(AutosuggestMediaType.class);
		}
        return result;
    }

}
