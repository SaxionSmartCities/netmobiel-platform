package eu.netmobiel.rideshare.api.mapping;

import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import com.github.dozermapper.core.loader.api.TypeMappingOptions;

import eu.netmobiel.rideshare.model.Car;

public class CarMappingBuilder extends BeanMappingBuilder {
	@Override
	protected void configure() {
		mapping(Car.class, eu.netmobiel.rideshare.api.model.Car.class, 
				TypeMappingOptions.mapId("driver")
		);

		mapping(Car.class, eu.netmobiel.rideshare.api.model.Car.class, 
				TypeMappingOptions.mapId("default")
		)
		.exclude("driverRef");
		
		mapping(Car.class, eu.netmobiel.rideshare.api.model.Car.class, 
				TypeMappingOptions.mapId("brand-model"),
				TypeMappingOptions.wildcard(false)
		)
		.fields("brand", "brand")
		.fields("model", "model");
	}
}
