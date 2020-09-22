package eu.netmobiel.banker.repository.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import eu.netmobiel.banker.model.CharityUserRoleType;

@Converter(autoApply = true)
public class CharityUserRoleTypeConverter implements AttributeConverter<CharityUserRoleType, String> {

	@Override
	public String convertToDatabaseColumn(CharityUserRoleType type) {
		return type == null ? null : type.getCode();
	}

	@Override
	public CharityUserRoleType convertToEntityAttribute(String code) {
		if (code == null) {
            return null;
        }
		return Stream.of(CharityUserRoleType.values())
		          .filter(c -> c.getCode().equals(code))
		          .findFirst()
		          .orElseThrow(IllegalArgumentException::new);
	}
}