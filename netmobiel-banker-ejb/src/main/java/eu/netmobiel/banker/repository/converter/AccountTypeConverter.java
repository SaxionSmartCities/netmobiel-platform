package eu.netmobiel.banker.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.banker.model.AccountType;

@Converter(autoApply = true)
public class AccountTypeConverter implements AttributeConverter<AccountType, String> {

	@Override
	public String convertToDatabaseColumn(AccountType type) {
		return type == null ? null : type.getCode();
	}

	@Override
	public AccountType convertToEntityAttribute(String code) {
		if (code == null) {
            return null;
        }
		return Stream.of(AccountType.values())
		          .filter(c -> c.getCode().equals(code))
		          .findFirst()
		          .orElseThrow(IllegalArgumentException::new);
	}
}