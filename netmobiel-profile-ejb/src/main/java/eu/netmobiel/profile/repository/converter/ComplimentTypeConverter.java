package eu.netmobiel.profile.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.profile.model.ComplimentType;

@Converter(autoApply = true)
public class ComplimentTypeConverter implements AttributeConverter<ComplimentType, String> {
  
    @Override
    public String convertToDatabaseColumn(ComplimentType state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public ComplimentType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(ComplimentType.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}