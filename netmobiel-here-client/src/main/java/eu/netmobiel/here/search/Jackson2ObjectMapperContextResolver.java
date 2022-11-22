package eu.netmobiel.here.search;

import java.io.IOException;

import javax.ws.rs.ext.ContextResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

import eu.netmobiel.here.search.api.model.OneOfOpenSearchAutosuggestResponseItemsItems;

public class Jackson2ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

	public Jackson2ObjectMapperContextResolver() {
		mapper = new ObjectMapper();
//		mapper.registerModule(new JavaTimeModule());
//		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//		mapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
		mapper.addHandler(new ProblemHandler());
	}

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
    /**
     * Handles the issue that OneOfOpenSearchAutosuggestResponseItemsItems Does not use a discriminator (as should). 
     * Because we only use AutosuggestEntityResultItem for our purpose, always select that type.
     * To do so, we must use a DeserializationProblemHandler. 
     * 
     * @author Jaap Reitsma
     *
     */
    public static class ProblemHandler extends DeserializationProblemHandler {
        private static final Logger log = LoggerFactory.getLogger(ProblemHandler.class);
		
		@Override
		public JavaType handleMissingTypeId(DeserializationContext ctxt, JavaType baseType, TypeIdResolver idResolver,
				String failureMsg) throws IOException {
			if (baseType.getRawClass() == OneOfOpenSearchAutosuggestResponseItemsItems.class) {
				// Always use this type, the other one is not used by us
				return idResolver.typeFromId(ctxt, "AutosuggestEntityResultItem");
			}
			log.error("*********************  Error resolving the type! ************************");
			return super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg);
		}

	}
}

