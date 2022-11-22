package eu.netmobiel.profile.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.profile.model.UserRole;

@Converter(autoApply = true)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {
  
    @Override
    public String convertToDatabaseColumn(UserRole state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public UserRole convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(UserRole.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}