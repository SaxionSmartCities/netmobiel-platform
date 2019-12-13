package eu.netmobiel.rideshare.api.mapping;

import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import com.github.dozermapper.core.loader.api.FieldsMappingOptions;
import com.github.dozermapper.core.loader.api.TypeMappingOptions;

import eu.netmobiel.rideshare.model.Booking;

public class BookingMappingBuilder extends BeanMappingBuilder {
	@Override
	protected void configure() {
		mapping(Booking.class, eu.netmobiel.rideshare.api.model.Booking.class, 
				TypeMappingOptions.mapId("default")
		)
		.exclude("passenger")
		.exclude("ride");

		mapping(Booking.class, eu.netmobiel.rideshare.api.model.Booking.class, 
				TypeMappingOptions.mapId("detail")
		)
		.exclude("ride")
		.fields("passenger", "passenger", FieldsMappingOptions.useMapId("name"));

		mapping(Booking.class, eu.netmobiel.rideshare.api.model.Booking.class, 
				TypeMappingOptions.mapId("passenger-view")
		)
		.exclude("passenger")
		.exclude("ride");
	}
}
