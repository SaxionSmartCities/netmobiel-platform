package eu.netmobiel.rideshare.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.rideshare.model.CarType;

@Converter(autoApply = true)
public class CarTypeConverter implements AttributeConverter<CarType, String> {
  
    @Override
    public String convertToDatabaseColumn(CarType carType) {
        if (carType == null) {
            return null;
        }
        return carType.getCode();
    }
 
    @Override
    public CarType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(CarType.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}