package eu.netmobiel.rideshare.api.mapping;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.api.mapping.annotation.BookingFlat;
import eu.netmobiel.rideshare.api.mapping.annotation.BookingMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.BookingShallow;
import eu.netmobiel.rideshare.api.mapping.annotation.CarBrandModelDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.CarMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.CarMyDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.LegDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.LegMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.RideDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideDetailsForBooking;
import eu.netmobiel.rideshare.api.mapping.annotation.RideMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.RideMyDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideSearchDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.UserSomeDetails;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideTemplate;

/**
 * This mapper defines the mapping from the domain Ride to the API Ride as defined by OpenAPI.
 * A number of variants are available, depending on the context. 
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { CarMapper.class, BookingMapper.class, LegMapper.class, StopMapper.class, UserMapper.class, JavaTimeMapper.class })
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
	@Mapping(target = "allowedLuggage", ignore = true)
	@Mapping(target = "bookings", ignore = true)
	@Mapping(target = "co2Emission", source = "CO2Emission")
	@Mapping(target = "fromPlace", source = "from") 
	@Mapping(target = "legs", ignore = true)
	@Mapping(target = "recurrence", source = "rideTemplate.recurrence") 
	@Mapping(target = "rideRef", source = "urn")
	@Mapping(target = "toPlace", source = "to") 
	@Mapping(target = "car", ignore = true)
	@Mapping(target = "removeBookingsItem", ignore = true)
	@Mapping(target = "removeLegsItem", ignore = true)
	public abstract eu.netmobiel.rideshare.api.model.Ride commonMap(Ride source);

	// Common mapping Api --> domain Ride
	@InheritInverseConfiguration(name = "commonMap")
	@Mapping(target = "bookings", ignore = true)
	@Mapping(target = "cancelReason", ignore = true)
	@Mapping(target = "car", ignore = true)
	@Mapping(target = "reminderCount", ignore = true)
	@Mapping(target = "validationExpirationTime", ignore = true)
	@Mapping(target = "deleted", ignore = true)
	@Mapping(target = "driver", ignore = true)
	@Mapping(target = "from.point", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "legs", ignore = true)
	@Mapping(target = "shareEligibility", ignore = true)
	@Mapping(target = "state", ignore = true)
	@Mapping(target = "stops", ignore = true)
	@Mapping(target = "to.point", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "departurePostalCode", ignore = true)
	@Mapping(target = "arrivalPostalCode", ignore = true)
	@Mapping(target = "activeBookings", ignore = true)
	public abstract Ride commonInverseMap(eu.netmobiel.rideshare.api.model.Ride source);

	
	// Domain Ride --> Api Ride: All details, including car, driver, bookings, legs 
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "bookings", source = "bookings", qualifiedBy = { BookingMapperQualifier.class, BookingShallow.class })
	@Mapping(target = "car", source = "car", 
		qualifiedBy = { CarMapperQualifier.class, CarMyDetails.class } )
	@Mapping(target = "legs", source = "legs", qualifiedBy = { LegMapperQualifier.class, LegDetails.class })
	@RideDetails
	public abstract eu.netmobiel.rideshare.api.model.Ride mapDetailed(Ride source);

	// Domain Ride --> Api Ride: Some details, like car brand and model. No driver. Include bookings, but no legs 
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "bookings", source = "bookings", qualifiedBy = { BookingMapperQualifier.class, BookingFlat.class })
	@Mapping(target = "car", source = "car", 
	qualifiedBy = { CarMapperQualifier.class, CarBrandModelDetails.class } )
	@Mapping(target = "driver", ignore = true)
	@Mapping(target = "driverRef", ignore = true)
	@RideMyDetails
	public abstract eu.netmobiel.rideshare.api.model.Ride mapMine(Ride source);

	// Domain Ride --> Api Ride: Some details, like car brand and model. Driver name. No bookings. No legs.
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "car", source = "car", 
		qualifiedBy = { CarMapperQualifier.class, CarBrandModelDetails.class } )
	@Mapping(target = "driver",	source = "driver",
		qualifiedBy = { UserMapperQualifier.class, UserSomeDetails.class } )
	@Mapping(target = "recurrence", ignore = true) 
	@RideSearchDetails
	public abstract eu.netmobiel.rideshare.api.model.Ride mapSearch(Ride source);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "car", source = "car", qualifiedBy = { CarMapperQualifier.class, CarBrandModelDetails.class } )
	@Mapping(target = "driver",	source = "driver", qualifiedBy = { UserMapperQualifier.class, UserSomeDetails.class } )
	@Mapping(target = "rideRef", source = "urn")
	@RideDetailsForBooking
	public abstract eu.netmobiel.rideshare.api.model.Ride mapDetailsForBooking(Ride source);
	
	@InheritConfiguration(name = "commonInverseMap")
	public abstract Ride map(eu.netmobiel.rideshare.api.model.Ride source);

    @AfterMapping
    public Ride fixTemplate(@MappingTarget Ride ride) {
    	RideTemplate rt = ride.getRideTemplate();
    	if (rt != null && (rt.getRecurrence() == null || rt.getRecurrence().getInterval() == null)) {
    		ride.setRideTemplate(null);
    	}
    	return ride;
    }

}
