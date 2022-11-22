package eu.netmobiel.geoservice.api.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.geoservice.api.SuggestionsApi;
import eu.netmobiel.geoservice.api.mapping.SuggestionMapper;
import eu.netmobiel.here.search.HereSearchClient;
import eu.netmobiel.here.search.api.model.OpenSearchAutosuggestResponse;

@ApplicationScoped
public class SuggestionsResource implements SuggestionsApi {

	@Inject
	private SuggestionMapper mapper;

    @Inject
    private HereSearchClient hereClient;


	@Override
	public Response getSuggestions(String query, String center, Integer radius, 
			String lang, Boolean details, Integer maxResults) {
		Response rsp = null;
		try {
			GeoLocation geoCenter = GeoLocation.fromString(center); 
	    	OpenSearchAutosuggestResponse searchResponse = hereClient.listAutosuggestions(query, geoCenter, radius, lang, details, maxResults);
			rsp = Response.ok(mapper.map(searchResponse)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
		return rsp;
	}

}
