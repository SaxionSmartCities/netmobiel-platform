package eu.netmobiel.planner.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

@ApplicationScoped
public class GeocodeService {
    @Inject
    private Logger log;

    @Resource(lookup = "java:global/geocode/hereAppId")
    private String hereAppId;
    @Resource(lookup = "java:global/geocode/hereAppCode")
    private String hereAppCode;
    @Resource(lookup = "java:global/geocode/hereAppAutocompleteUrl")
    private String hereAppAutocompleteUrl; 
    @Resource(lookup = "java:global/geocode/hereAppGeocodeUrl")
    private String hereAppGeocodeUrl; 

    @SuppressWarnings("el-syntax")
    public String listSuggestions(String query) {
		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("HERE_APP_ID", hereAppId);
		valuesMap.put("HERE_APP_CODE", hereAppCode);
		valuesMap.put("QUERY", query);
		StringSubstitutor substitutor = new StringSubstitutor(valuesMap, "#{", "}");
		String url = substitutor.replace(hereAppAutocompleteUrl);
		Client client = ClientBuilder.newBuilder().build();
		WebTarget target = client.target(url);
		String result = null;
		log.debug(String.format("Geocode suggestions for %s", query));
		try (Response response = target.request().get()) {
	        result = response.readEntity(String.class);
		}
        return result;
    }

    @SuppressWarnings("el-syntax")
    public String getLocation(String locationId) {
		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("HERE_APP_ID", hereAppId);
		valuesMap.put("HERE_APP_CODE", hereAppCode);
		valuesMap.put("LOCATION_ID", locationId);
		StringSubstitutor substitutor = new StringSubstitutor(valuesMap, "#{", "}");
		String url = substitutor.replace(hereAppGeocodeUrl);
		Client client = ClientBuilder.newBuilder().build();
		WebTarget target = client.target(url);
		String result = null;
		log.debug(String.format("Geocode %s", locationId));
		try (Response response = target.request().get()) {
	        result = response.readEntity(String.class);
		}
        return result;
    }

}
