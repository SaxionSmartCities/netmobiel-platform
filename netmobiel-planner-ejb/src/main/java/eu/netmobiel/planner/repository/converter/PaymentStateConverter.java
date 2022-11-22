package eu.netmobiel.planner.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.commons.model.PaymentState;

@Converter(autoApply = true)
public class PaymentStateConverter implements AttributeConverter<PaymentState, String> {
  
    @Override
    public String convertToDatabaseColumn(PaymentState state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public PaymentState convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(PaymentState.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}