package eu.netmobiel.rideshare.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.rideshare.model.RideState;

@Converter(autoApply = true)
public class RideStateConverter implements AttributeConverter<RideState, String> {
  
    @Override
    public String convertToDatabaseColumn(RideState state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public RideState convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(RideState.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}