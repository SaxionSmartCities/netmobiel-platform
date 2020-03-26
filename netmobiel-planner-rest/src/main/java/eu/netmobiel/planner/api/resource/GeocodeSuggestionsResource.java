package eu.netmobiel.planner.api.resource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.here.places.HerePlacesClient;
import eu.netmobiel.here.places.model.AutosuggestMediaType;
import eu.netmobiel.planner.api.GeocodeSuggestionsApi;
import eu.netmobiel.planner.api.mapping.AutosuggestPlaceMapper;
import eu.netmobiel.planner.api.model.GeocodeSuggestion;

@ApplicationScoped
public class GeocodeSuggestionsResource implements GeocodeSuggestionsApi {
	private static final Integer DEFAULT_RESULTS = 10;
	private static final Integer DEFAULT_RADIUS = 50000;
	private static final String DEFAULT_RESULT_TYPES= "place,address";
	
	@SuppressWarnings("unused")
	@Inject
    private Logger log;
 
    @Inject
    private AutosuggestPlaceMapper mapper;

    @Inject
    private HerePlacesClient herePlacesClient;


	@Override
    public Response getGeocodeSuggestions(String query, String center, Integer radius, String resultTypes, String hls, String hle, Integer maxResults, Integer offset) {
    	Response rsp = null;
    	if (query == null || query.isEmpty() || center== null) {
    		throw new BadRequestException("Missing one or more mandatory parameters: query, center");
    	}
    	if (radius == null) {
    		radius = DEFAULT_RADIUS;
    	}
    	if (resultTypes == null) {
    		resultTypes = DEFAULT_RESULT_TYPES;
    	}
    	if (resultTypes.trim().isEmpty()) {
    		resultTypes = null;
    	}
    	if (maxResults == null) {
    		maxResults = DEFAULT_RESULTS;
    	}
		try {
			AutosuggestMediaType result = herePlacesClient.listAutosuggestions(query, GeoLocation.fromString(center),
					radius, resultTypes, hls, hle, maxResults); 
			List<GeocodeSuggestion> suggestions = Arrays.stream(result.results)
					.map(amt -> mapper.map(amt))
					.collect(Collectors.toList());
			PagedResult<GeocodeSuggestion> page = new PagedResult<>(suggestions, maxResults, 0, (long) suggestions.size());
			rsp = Response.ok(page).build();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new InternalServerErrorException(e);
		}
    	return rsp;
	}


}
