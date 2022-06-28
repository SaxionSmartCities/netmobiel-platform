package eu.netmobiel.rideshare.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.rideshare.model.ToolType;

@Converter(autoApply = true)
public class ToolTypeConverter implements AttributeConverter<ToolType, String> {
  
    @Override
    public String convertToDatabaseColumn(ToolType state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public ToolType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(ToolType.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}