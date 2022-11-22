package eu.netmobiel.commons.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

/**
 * Enables LocalDate to be used as a QueryParam.
 */
@Provider
public class OffsetDateTimeParamConverterProvider implements ParamConverterProvider {
    @SuppressWarnings("unchecked")
	@Override public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {

        if (!rawType.isAssignableFrom(OffsetDateTime.class)) {
            return null;
        }
		return (ParamConverter<T>) new OffsetDateTimeConverter();
    }
    
    public static class OffsetDateTimeConverter implements ParamConverter<OffsetDateTime> {
    	@Override
		public OffsetDateTime fromString(String dateString) {
    	    return dateString != null ? OffsetDateTime.parse(dateString) : null;
    	}

    	@Override
		public String toString(OffsetDateTime value) {
    	    return value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    	}
    }
}