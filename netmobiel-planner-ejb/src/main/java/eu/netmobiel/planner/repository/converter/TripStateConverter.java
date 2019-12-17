package eu.netmobiel.planner.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.planner.model.TripState;

@Converter(autoApply = true)
public class TripStateConverter implements AttributeConverter<TripState, String> {
  
    @Override
    public String convertToDatabaseColumn(TripState state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public TripState convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(TripState.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}