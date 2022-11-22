package eu.netmobiel.planner.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.planner.model.RelativeDirection;

@Converter(autoApply = true)
public class RelativeDirectionConverter implements AttributeConverter<RelativeDirection, String> {
  
    @Override
    public String convertToDatabaseColumn(RelativeDirection state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public RelativeDirection convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(RelativeDirection.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}