package eu.netmobiel.rideshare.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.model.Booking;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = StopMapper.class)
public interface BookingMapper {

	// Booking <--> Booking
	@Mapping(target = "passengerRef", ignore = true)
	@Mapping(target = "passenger", ignore = true)
	eu.netmobiel.rideshare.api.model.Booking mapMine(Booking source);

	eu.netmobiel.rideshare.api.model.Booking map(Booking source);
	
	@Mapping(target = "passenger", ignore = true)
	@Mapping(target = "ride", ignore = true)
	Booking map(eu.netmobiel.rideshare.api.model.Booking source);

}
