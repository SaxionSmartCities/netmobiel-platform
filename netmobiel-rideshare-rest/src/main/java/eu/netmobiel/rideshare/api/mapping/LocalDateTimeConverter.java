package eu.netmobiel.rideshare.api.mapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.github.dozermapper.core.DozerConverter;

public class LocalDateTimeConverter extends DozerConverter<String, LocalDateTime> {

    public LocalDateTimeConverter() {
        super(String.class, LocalDateTime.class);
    }

    @Override
    public LocalDateTime convertTo(String source, LocalDateTime destination) {
    	return source == null ? null : LocalDateTime.parse(source);
    }

    @Override
    public String convertFrom(LocalDateTime source, String destination) {
    	return source == null ? null : source.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}