package eu.netmobiel.profile.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.profile.model.UserEventType;

@Converter(autoApply = true)
public class UserEventTypeConverter implements AttributeConverter<UserEventType, String> {
  
    @Override
    public String convertToDatabaseColumn(UserEventType state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public UserEventType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(UserEventType.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}