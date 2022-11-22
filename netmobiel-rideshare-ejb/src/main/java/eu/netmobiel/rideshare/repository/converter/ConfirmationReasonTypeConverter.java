package eu.netmobiel.rideshare.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.commons.model.ConfirmationReasonType;

@Converter(autoApply = true)
public class ConfirmationReasonTypeConverter implements AttributeConverter<ConfirmationReasonType, String> {
  
    @Override
    public String convertToDatabaseColumn(ConfirmationReasonType state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public ConfirmationReasonType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(ConfirmationReasonType.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}