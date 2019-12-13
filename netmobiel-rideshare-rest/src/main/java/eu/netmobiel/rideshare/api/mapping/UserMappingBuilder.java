package eu.netmobiel.rideshare.api.mapping;

import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import com.github.dozermapper.core.loader.api.TypeMappingOptions;

import eu.netmobiel.rideshare.model.User;

public class UserMappingBuilder extends BeanMappingBuilder {
	@Override
	protected void configure() {
		mapping(User.class, eu.netmobiel.rideshare.api.model.User.class, 
				TypeMappingOptions.mapId("default")
		);
		mapping(User.class, eu.netmobiel.rideshare.api.model.User.class, 
				TypeMappingOptions.mapId("name"),
				TypeMappingOptions.wildcard(false)
		)
		.fields("givenName", "givenName")
		.fields("familyName", "familyName");
	}
}
