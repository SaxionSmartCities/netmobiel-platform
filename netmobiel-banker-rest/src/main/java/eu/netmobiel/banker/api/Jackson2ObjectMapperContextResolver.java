package eu.netmobiel.banker.api;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Provider
public class Jackson2ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

	public Jackson2ObjectMapperContextResolver() {
		mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());						// Add support for Instant etc. objects
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);		// No numeric timestamps
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);	// Do not fail
		// For now emit all values, also null and defaults (e.g. a 0).
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);				
//		mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
	}

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}

