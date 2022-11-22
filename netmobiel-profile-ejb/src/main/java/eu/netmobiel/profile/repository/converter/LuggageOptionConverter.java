package eu.netmobiel.profile.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.profile.model.LuggageOption;

@Converter(autoApply = true)
public class LuggageOptionConverter implements AttributeConverter<LuggageOption, String> {
  
    @Override
    public String convertToDatabaseColumn(LuggageOption state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public LuggageOption convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(LuggageOption.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}