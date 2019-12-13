package eu.netmobiel.commons.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

/**
 * Enables LocalDate to be used as a QueryParam.
 */
@Provider
public class LocalDataParamConverterProvider implements ParamConverterProvider {
    @SuppressWarnings("unchecked")
	@Override public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {

        if (!rawType.isAssignableFrom(LocalDate.class)) {
            return null;
        }
		return (ParamConverter<T>) new LocalDateConverter();
    }
    
    public static class LocalDateConverter implements ParamConverter<LocalDate> {
    	@Override
		public LocalDate fromString(String dateString) {
    	    return dateString != null ? LocalDate.parse(dateString) : null;
    	}

    	@Override
		public String toString(LocalDate value) {
    	    return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
    	}
    }
}