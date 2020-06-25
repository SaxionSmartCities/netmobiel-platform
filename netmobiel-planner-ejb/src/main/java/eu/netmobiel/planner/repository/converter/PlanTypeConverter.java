package eu.netmobiel.planner.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.planner.model.PlanType;

@Converter(autoApply = true)
public class PlanTypeConverter implements AttributeConverter<PlanType, String> {
  
    @Override
    public String convertToDatabaseColumn(PlanType state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public PlanType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(PlanType.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}