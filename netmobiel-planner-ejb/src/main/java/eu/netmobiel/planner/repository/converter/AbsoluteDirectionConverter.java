package eu.netmobiel.planner.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.planner.model.AbsoluteDirection;

@Converter(autoApply = true)
public class AbsoluteDirectionConverter implements AttributeConverter<AbsoluteDirection, String> {
  
    @Override
    public String convertToDatabaseColumn(AbsoluteDirection state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public AbsoluteDirection convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(AbsoluteDirection.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}