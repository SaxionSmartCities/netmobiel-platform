package eu.netmobiel.rideshare.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.rideshare.model.Gender;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {
  
    @Override
    public String convertToDatabaseColumn(Gender gender) {
        if (gender == null) {
            return null;
        }
        return gender.getCode();
    }
 
    @Override
    public Gender convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(Gender.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}