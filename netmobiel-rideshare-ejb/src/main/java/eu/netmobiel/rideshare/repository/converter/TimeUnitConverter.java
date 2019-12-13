package eu.netmobiel.rideshare.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.rideshare.model.TimeUnit;

@Converter(autoApply = true)
public class TimeUnitConverter implements AttributeConverter<TimeUnit, String> {
  
    @Override
    public String convertToDatabaseColumn(TimeUnit unit) {
        if (unit == null) {
            return null;
        }
        return unit.getCode();
    }
 
    @Override
    public TimeUnit convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(TimeUnit.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}