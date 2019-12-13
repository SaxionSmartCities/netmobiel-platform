package eu.netmobiel.rideshare.api.mapping;

import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import com.github.dozermapper.core.loader.api.FieldsMappingOptions;
import com.github.dozermapper.core.loader.api.TypeMappingBuilder;
import com.github.dozermapper.core.loader.api.TypeMappingOptions;

import eu.netmobiel.rideshare.model.Ride;

public class RideMappingBuilder extends BeanMappingBuilder {
	
	private TypeMappingBuilder addCommonDefinitions(TypeMappingBuilder mapping) {
		return mapping
				.fields("rideTemplate.carRef", "carRef")
				.fields("rideTemplate.fromPlace", "fromPlace")
				.fields("rideTemplate.toPlace", "toPlace")
				.fields("rideTemplate.nrSeatsAvailable", "nrSeatsAvailable")
				.fields("rideTemplate.remarks", "remarks")
				.fields("rideTemplate.recurrence", "recurrence")
				.fields("rideTemplate.estimatedDistance", "estimatedDistance")
				.fields("rideTemplate.estimatedDrivingTime", "estimatedDrivingTime")
				.fields("rideTemplate.estimatedCO2Emission", "estimatedCO2Emission")
				.fields("rideTemplate.maxDetourMeters", "maxDetourMeters")
				.fields("rideTemplate.maxDetourSeconds", "maxDetourSeconds")
				.fields("rideTemplate.carthesianDistance", "carthesianDistance")
				.fields("rideTemplate.carthesianBearing", "carthesianBearing")
				.fields("departureTime", "departureTime", FieldsMappingOptions.customConverter(LocalDateTimeConverter.class))
				.fields("estimatedArrivalTime", "estimatedArrivalTime", FieldsMappingOptions.customConverter(LocalDateTimeConverter.class))
				;
	}
	
	@Override
	protected void configure() {
		addCommonDefinitions(mapping(Ride.class, eu.netmobiel.rideshare.api.model.Ride.class, TypeMappingOptions.mapId("detail")))
		.exclude("rideTemplate.shareEligibility")
		.exclude("rideRef")
		.fields("rideTemplate.driver", "driver")
		.fields("rideTemplate.driverRef", "driverRef")
		.fields("rideTemplate.car", "car")
		.fields("bookings", "bookings", FieldsMappingOptions.useMapId("detail"));
		
		// A list of my rides
		addCommonDefinitions(mapping(Ride.class, eu.netmobiel.rideshare.api.model.Ride.class, TypeMappingOptions.mapId("my-details")))
		.exclude("rideTemplate.shareEligibility")
		.exclude("rideTemplate.driver")
		.exclude("rideTemplate.driverRef")
		.exclude("rideRef")
		.fields("bookings", "bookings", FieldsMappingOptions.useMapId("detail"))
		.fields("rideTemplate.car", "car", FieldsMappingOptions.useMapId("brand-model"));

		addCommonDefinitions(mapping(Ride.class, eu.netmobiel.rideshare.api.model.Ride.class, TypeMappingOptions.mapId("default")))
		.exclude("rideTemplate.shareEligibility")
		.exclude("rideTemplate.car")
		.exclude("rideTemplate.driver")
		.exclude("rideTemplate.driverRef")
		.exclude("rideRef")
		.exclude("bookings");
		
		// Search ride result mapping 
		addCommonDefinitions(mapping(Ride.class, eu.netmobiel.rideshare.api.model.Ride.class, TypeMappingOptions.mapId("search")))
		.exclude("rideTemplate.shareEligibility")
		.exclude("bookings")
		.fields("rideTemplate.car", "car", FieldsMappingOptions.useMapId("brand-model"))
		.fields("rideTemplate.driver", "driver", FieldsMappingOptions.useMapId("name"));
	}
	

}
