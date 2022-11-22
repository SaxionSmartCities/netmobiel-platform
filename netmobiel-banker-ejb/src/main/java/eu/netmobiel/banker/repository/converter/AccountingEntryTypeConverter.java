package eu.netmobiel.banker.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.banker.model.AccountingEntryType;

@Converter(autoApply = true)
public class AccountingEntryTypeConverter implements AttributeConverter<AccountingEntryType, String> {

	@Override
	public String convertToDatabaseColumn(AccountingEntryType type) {
		return type == null ? null : type.getCode();
	}

	@Override
	public AccountingEntryType convertToEntityAttribute(String code) {
		if (code == null) {
            return null;
        }
		return Stream.of(AccountingEntryType.values())
		          .filter(c -> c.getCode().equals(code))
		          .findFirst()
		          .orElseThrow(IllegalArgumentException::new);
	}
}