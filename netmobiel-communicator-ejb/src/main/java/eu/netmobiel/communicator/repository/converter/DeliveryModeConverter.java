package eu.netmobiel.communicator.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.communicator.model.DeliveryMode;

@Converter(autoApply = true)
public class DeliveryModeConverter implements AttributeConverter<DeliveryMode, String> {
  
    @Override
    public String convertToDatabaseColumn(DeliveryMode DeliveryMode) {
        if (DeliveryMode == null) {
            return null;
        }
        return DeliveryMode.getCode();
    }
 
    @Override
    public DeliveryMode convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(DeliveryMode.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}