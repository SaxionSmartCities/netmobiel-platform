package eu.netmobiel.rideshare.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.api.mapping.annotation.CarBrandModelDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.CarMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.CarMyDetails;
import eu.netmobiel.rideshare.model.Car;

/**
 * This mapper defines the mapping from the domain Car to the API Car as defined by OpenAPI.
 * Because cars are written to and read from the service, a bi-directional mapping is necessary.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
@CarMapperQualifier
public interface CarMapper {

	// domain Car --> API Car
	// For my cars, the driver reference is not needed
	@Mapping(target = "driverRef", ignore = true)
	@Mapping(target = "carRef", source = "urn")
	@CarMyDetails
	eu.netmobiel.rideshare.api.model.Car mapMyCar(Car source);

	// domain Car --> API Car
	@Mapping(target = "carRef", source = "urn")
	eu.netmobiel.rideshare.api.model.Car map(Car source);

	// domain Car --> API Car Brand and model only
	@Mapping(target = "co2Emission", ignore = true) 
	@Mapping(target = "licensePlate", ignore = true) 
	@Mapping(target = "registrationCountry", ignore = true) 
	@Mapping(target = "registrationYear", ignore = true) 
	@Mapping(target = "type", ignore = true) 
	@Mapping(target = "color", ignore = true) 
	@Mapping(target = "color2", ignore = true) 
	@Mapping(target = "id", ignore = true) 
	@Mapping(target = "typeRegistrationId", ignore = true) 
	@Mapping(target = "driverRef", ignore = true) 
	@Mapping(target = "nrDoors", ignore = true) 
	@Mapping(target = "nrSeats", ignore = true) 
	@Mapping(target = "deleted", ignore = true) 
	@Mapping(target = "carRef", source = "urn")
	@CarBrandModelDetails
	eu.netmobiel.rideshare.api.model.Car mapBrandModel(Car source);

	// Api Car -> Domain Car
	@Mapping(target = "driver", ignore = true)
	Car map(eu.netmobiel.rideshare.api.model.Car source);

}
