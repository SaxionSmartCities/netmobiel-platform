package eu.netmobiel.rideshare.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.rideshare.model.BookingState;

@Converter(autoApply = true)
public class BookingStateConverter implements AttributeConverter<BookingState, String> {
  
    @Override
    public String convertToDatabaseColumn(BookingState state) {
        if (state == null) {
            return null;
        }
        return state.getCode();
    }
 
    @Override
    public BookingState convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
 
        return Stream.of(BookingState.values())
          .filter(c -> c.getCode().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}