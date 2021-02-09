package eu.netmobiel.profile.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.profile.model.TraverseMode;

@Converter(autoApply = true)
public class TraverseModeConverter implements AttributeConverter<TraverseMode, String> {
  
    @Override
    public String convertToDatabaseColumn(TraverseMode state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public TraverseMode convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(TraverseMode.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}