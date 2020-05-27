package eu.netmobiel.rideshare.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.api.mapping.annotation.BookingShallow;
import eu.netmobiel.rideshare.api.mapping.annotation.BookingMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.BookingNested;
import eu.netmobiel.rideshare.api.mapping.annotation.BookingNestedMine;
import eu.netmobiel.rideshare.api.mapping.annotation.BookingReference;
import eu.netmobiel.rideshare.api.mapping.annotation.LegDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.LegReference;
import eu.netmobiel.rideshare.model.Booking;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * We have define three levels of details for a Booking:
 * ReferenceOnly: Use BookingReference. Only the URN is listed.
 * Shallow: Use BookingShallow. Booking details are shown, including passenger, but only leg references are listed. 
 * Nested: Use BookingNested. Booking details, including passenger, and leg details are listed.
 * NestedMine: Use BookingNestedMine: Same as BookingNested, but without passenger details.  
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { LegMapper.class, StopMapper.class })
@BookingMapperQualifier
public abstract class BookingMapper {

	// Domain Booking --> API Booking (list my booking)
	@Mapping(target = "passengerRef", ignore = true)
	@Mapping(target = "passenger", ignore = true)
	@Mapping(target = "legs", source = "legs", qualifiedBy = LegDetails.class)
	@BookingNestedMine
	public abstract eu.netmobiel.rideshare.api.model.Booking mapMineInDetail(Booking source);

	
	// Domain Booking --> API Booking
	@Mapping(target = "legs", source = "legs", qualifiedBy = LegDetails.class)
	@BookingNested
	public abstract eu.netmobiel.rideshare.api.model.Booking mapInDetail(Booking source);

	// Domain Booking --> API Booking
	@Mapping(target = "legs", source = "legs", qualifiedBy = LegReference.class)
	@BookingShallow
	public abstract eu.netmobiel.rideshare.api.model.Booking mapByReference(Booking source);

	// API --> Domain Booking (create, update)
	@Mapping(target = "passenger", ignore = true)
	@Mapping(target = "ride", ignore = true)
	@Mapping(target = "legs", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "pickup.point", ignore = true)
	@Mapping(target = "dropOff.point", ignore = true)
	public abstract Booking map(eu.netmobiel.rideshare.api.model.Booking source);

	// Domain -> API booking (list rides)
	@BookingReference
	public List<String> map(List<Booking> source) {
		return source.stream().map(Booking::getBookingRef).collect(Collectors.toList());
	}

    // OffsetDateTime --> Instant 
    public  Instant  map(OffsetDateTime offsetDateTime) {
    	return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
}
