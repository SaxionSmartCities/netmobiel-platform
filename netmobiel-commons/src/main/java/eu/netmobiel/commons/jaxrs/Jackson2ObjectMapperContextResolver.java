package eu.netmobiel.commons.jaxrs;

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
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		// When the following statement is enabled, the complete Page object is omitted in case of an empty response.
		// That is undesirable.
		// Some other values seen at the generation of a client 
//		mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
//	    mapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
//	    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
//	    mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
//	    mapper.setDateFormat(new RFC3339DateFormat());

	}

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}

