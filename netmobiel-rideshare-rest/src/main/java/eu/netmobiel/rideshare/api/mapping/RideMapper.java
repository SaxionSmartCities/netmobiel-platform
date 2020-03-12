package eu.netmobiel.rideshare.api.mapping;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.api.mapping.annotation.CarBrandModelDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.CarMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.CarMyDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.RideMyDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideSearchDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.UserSomeDetails;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;

/**
 * This mapper defines the mapping from the domain Ride to the API Ride as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { StopMapper.class, CarMapper.class, UserMapper.class })
@RideMapperQualifier
public interface RideMapper {
	// Default mapping domain Ride --> Api Ride
	@Mapping(target = "car", source = "rideTemplate.car") 
	@Mapping(target = "carRef", source = "rideTemplate.carRef") 
	@Mapping(target = "carthesianBearing", source = "rideTemplate.carthesianBearing") 
	@Mapping(target = "carthesianDistance", source = "rideTemplate.carthesianDistance") 
	@Mapping(target = "driver", source = "rideTemplate.driver") 
	@Mapping(target = "driverRef", source = "rideTemplate.driverRef") 
	@Mapping(target = "estimatedDistance", source = "rideTemplate.estimatedDistance") 
	@Mapping(target = "estimatedDrivingTime", source = "rideTemplate.estimatedDrivingTime") 
	@Mapping(target = "estimatedCO2Emission", source = "rideTemplate.estimatedCO2Emission") 
	@Mapping(target = "fromPlace", source = "rideTemplate.fromPlace") 
	@Mapping(target = "nrSeatsAvailable", source = "rideTemplate.nrSeatsAvailable") 
	@Mapping(target = "recurrence", source = "rideTemplate.recurrence") 
	@Mapping(target = "remarks", source = "rideTemplate.remarks") 
	@Mapping(target = "maxDetourMeters", source = "rideTemplate.maxDetourMeters") 
	@Mapping(target = "maxDetourSeconds", source = "rideTemplate.maxDetourSeconds") 
	@Mapping(target = "toPlace", source = "rideTemplate.toPlace") 
	@Mapping(target = "allowedLuggage", ignore = true)
	eu.netmobiel.rideshare.api.model.Ride commonMap(Ride source);

	// Default mapping Api Ride --> domain Ride
	@InheritInverseConfiguration(name = "commonMap")
	@Mapping(target = "bookings", ignore = true)
	@Mapping(target = "deleted", ignore = true)
	@Mapping(target = "rideTemplate.car", ignore = true)
	@Mapping(target = "rideTemplate.driver", ignore = true)
	@Mapping(target = "stops", ignore = true)
	Ride commonInverseMap(eu.netmobiel.rideshare.api.model.Ride source);

	
	// Domain Ride --> Api Ride: All details, including car, driver, bookings 
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "car", source = "rideTemplate.car", 
		qualifiedBy = { CarMapperQualifier.class, CarMyDetails.class } )
	@RideDetails
	eu.netmobiel.rideshare.api.model.Ride mapDetailed(Ride source);

	// Domain Ride --> Api Ride: Some details, like car brand and model. No driver. Include bookings. 
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "rideRef", ignore = true)
	@Mapping(target = "driver", ignore = true)
	@Mapping(target = "driverRef", ignore = true)
	@Mapping(target = "car", source = "rideTemplate.car", 
		qualifiedBy = { CarMapperQualifier.class, CarBrandModelDetails.class } )
	@RideMyDetails
	eu.netmobiel.rideshare.api.model.Ride mapMine(Ride source);

	// Domain Ride --> Api Ride: Some details, like car brand and model. Driver name. No bookings. 
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "bookings", ignore = true)
	@Mapping(target = "car", source = "rideTemplate.car", 
		qualifiedBy = { CarMapperQualifier.class, CarBrandModelDetails.class } )
	@Mapping(target = "driver",	source = "rideTemplate.driver",
		qualifiedBy = { UserMapperQualifier.class, UserSomeDetails.class } )
	@RideSearchDetails
	eu.netmobiel.rideshare.api.model.Ride mapSearch(Ride source);

	
	@InheritConfiguration(name = "commonInverseMap")
	Ride map(eu.netmobiel.rideshare.api.model.Ride source);

	eu.netmobiel.rideshare.api.model.Booking map(Booking source);

//	@Mapping(target = "id", ignore = true) 
//	@Mapping(target = "email", ignore = true) 
//	@Mapping(target = "gender", ignore = true) 
//	@Mapping(target = "yearOfBirth", ignore = true) 
//	@Mapping(target = "managedIdentity", ignore = true) 
//	eu.netmobiel.rideshare.api.model.User map(User source);
	
//	@Mapping(target = "co2Emission", ignore = true) 
//	@Mapping(target = "licensePlate", ignore = true) 
//	@Mapping(target = "registrationCountry", ignore = true) 
//	@Mapping(target = "registrationYear", ignore = true) 
//	@Mapping(target = "type", ignore = true) 
//	@Mapping(target = "color", ignore = true) 
//	@Mapping(target = "color2", ignore = true) 
//	@Mapping(target = "id", ignore = true) 
//	@Mapping(target = "typeRegistrationId", ignore = true) 
//	@Mapping(target = "driverRef", ignore = true) 
//	@Mapping(target = "nrDoors", ignore = true) 
//	@Mapping(target = "nrSeats", ignore = true) 
//	@Mapping(target = "deleted", ignore = true) 
//	eu.netmobiel.rideshare.api.model.Car map(Car source);
}
