package eu.netmobiel.banker.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.banker.model.PaymentStatus;

@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

	@Override
	public String convertToDatabaseColumn(PaymentStatus type) {
		return type == null ? null : type.getCode();
	}

	@Override
	public PaymentStatus convertToEntityAttribute(String code) {
		if (code == null) {
            return null;
        }
		return Stream.of(PaymentStatus.values())
		          .filter(c -> c.getCode().equals(code))
		          .findFirst()
		          .orElseThrow(IllegalArgumentException::new);
	}
}