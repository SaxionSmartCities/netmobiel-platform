package eu.netmobiel.banker.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.banker.model.AccountPurposeType;

@Converter(autoApply = true)
public class AccountPurposeTypeConverter implements AttributeConverter<AccountPurposeType, String> {

	@Override
	public String convertToDatabaseColumn(AccountPurposeType type) {
		return type == null ? null : type.getCode();
	}

	@Override
	public AccountPurposeType convertToEntityAttribute(String code) {
		if (code == null) {
            return null;
        }
		return Stream.of(AccountPurposeType.values())
		          .filter(c -> c.getCode().equals(code))
		          .findFirst()
		          .orElseThrow(IllegalArgumentException::new);
	}
}