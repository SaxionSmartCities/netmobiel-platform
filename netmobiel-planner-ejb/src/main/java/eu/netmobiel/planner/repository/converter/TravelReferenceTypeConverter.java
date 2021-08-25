package eu.netmobiel.planner.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.planner.model.TravelReferenceType;

@Converter(autoApply = true)
public class TravelReferenceTypeConverter implements AttributeConverter<TravelReferenceType, String> {
  
    @Override
    public String convertToDatabaseColumn(TravelReferenceType state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public eu.netmobiel.planner.model.TravelReferenceType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(TravelReferenceType.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}