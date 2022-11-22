package eu.netmobiel.rideshare.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.rideshare.model.RideScope;

@Converter(autoApply = true)
public class RideScopeConverter implements AttributeConverter<RideScope, String> {
  
    @Override
    public String convertToDatabaseColumn(RideScope carType) {
        if (carType == null) {
            return null;
        }
        return carType.getCode();
    }
 
    @Override
    public RideScope convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(RideScope.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("No such scope: " + code));
    }
}