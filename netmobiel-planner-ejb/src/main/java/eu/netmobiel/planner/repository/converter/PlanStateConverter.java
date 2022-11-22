package eu.netmobiel.planner.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.planner.model.PlanState;

@Converter(autoApply = true)
public class PlanStateConverter implements AttributeConverter<PlanState, String> {
  
    @Override
    public String convertToDatabaseColumn(PlanState state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public PlanState convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(PlanState.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}