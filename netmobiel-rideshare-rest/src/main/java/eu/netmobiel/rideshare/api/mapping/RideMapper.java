package eu.netmobiel.rideshare.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.api.mapping.annotation.BookingShallow;
import eu.netmobiel.rideshare.api.mapping.annotation.CarBrandModelDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.CarMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.CarMyDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.LegDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.RideMyDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideSearchDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.UserSomeDetails;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;

/**
 * This mapper defines the mapping from the domain Ride to the API Ride as defined by OpenAPI.
 * A number of variants are available, depending on the context. 
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { CarMapper.class, BookingMapper.class, LegMapper.class, StopMapper.class, UserMapper.class })
@RideMapperQualifier
public abstract class RideMapper {

	// Domain --> API Recurrence
	@Mapping(target = "horizon", source = "localHorizon") 
	public abstract eu.netmobiel.rideshare.api.model.Recurrence map(Recurrence source);

	// API --> Domain Recurrence
	@InheritInverseConfiguration 
	@Mapping(target = "horizon", ignore = true) 
	public abstract Recurrence map(eu.netmobiel.rideshare.api.model.Recurrence source);

	// Default mapping domain Ride --> Api Ride (common)
	@Mapping(target = "fromPlace", source = "from") 
	@Mapping(target = "recurrence", source = "rideTemplate.recurrence") 
	@Mapping(target = "toPlace", source = "to") 
	@Mapping(target = "allowedLuggage", ignore = true)
	@Mapping(target = "co2Emission", source = "CO2Emission")
	@Mapping(target = "bookings", ignore = true)
	@Mapping(target = "legs", ignore = true)
	public abstract eu.netmobiel.rideshare.api.model.Ride commonMap(Ride source);

	// Common mapping Api --> domain Ride
	@InheritInverseConfiguration(name = "commonMap")
	@Mapping(target = "from.point", ignore = true)
	@Mapping(target = "to.point", ignore = true)
	@Mapping(target = "shareEligibility", ignore = true)
	@Mapping(target = "cancelReason", ignore = true)
	@Mapping(target = "bookings", ignore = true)
	@Mapping(target = "deleted", ignore = true)
	@Mapping(target = "car", ignore = true)
	@Mapping(target = "driver", ignore = true)
	@Mapping(target = "stops", ignore = true)
	@Mapping(target = "legs", ignore = true)
	public abstract Ride commonInverseMap(eu.netmobiel.rideshare.api.model.Ride source);

	
	// Domain Ride --> Api Ride: All details, including car, driver, bookings 
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "car", source = "rideTemplate.car", 
		qualifiedBy = { CarMapperQualifier.class, CarMyDetails.class } )
	@Mapping(target = "bookings", source = "bookings", qualifiedBy = BookingShallow.class)
	@Mapping(target = "legs", source = "legs", qualifiedBy = LegDetails.class)
	@RideDetails
	public abstract eu.netmobiel.rideshare.api.model.Ride mapDetailed(Ride source);

	// Domain Ride --> Api Ride: Some details, like car brand and model. No driver. Include bookings. 
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "driver", ignore = true)
	@Mapping(target = "driverRef", ignore = true)
	@Mapping(target = "car", source = "rideTemplate.car", 
		qualifiedBy = { CarMapperQualifier.class, CarBrandModelDetails.class } )
	@Mapping(target = "bookings", source = "bookings", qualifiedBy = BookingShallow.class)
	@Mapping(target = "legs", source = "legs", qualifiedBy = LegDetails.class)
	@RideMyDetails
	public abstract eu.netmobiel.rideshare.api.model.Ride mapMine(Ride source);

	// Domain Ride --> Api Ride: Some details, like car brand and model. Driver name. No bookings. No legs.
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "car", source = "rideTemplate.car", 
		qualifiedBy = { CarMapperQualifier.class, CarBrandModelDetails.class } )
	@Mapping(target = "driver",	source = "rideTemplate.driver",
		qualifiedBy = { UserMapperQualifier.class, UserSomeDetails.class } )
	@RideSearchDetails
	public abstract eu.netmobiel.rideshare.api.model.Ride mapSearch(Ride source);

	
	@InheritConfiguration(name = "commonInverseMap")
	public abstract Ride map(eu.netmobiel.rideshare.api.model.Ride source);

    // OffsetDateTime --> Instant 
    public  Instant  map(OffsetDateTime offsetDateTime) {
    	return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }


}
