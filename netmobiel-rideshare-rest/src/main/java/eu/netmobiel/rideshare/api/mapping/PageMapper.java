package eu.netmobiel.rideshare.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.rideshare.api.mapping.annotation.BookingMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.BookingNestedMine;
import eu.netmobiel.rideshare.api.mapping.annotation.RideDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.RideMyDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.RideSearchDetails;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;

/**
 * This mapper defines the mapping from the domain PagedResult to the API PagedResult as defined by OpenAPI.
 * One way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
		uses = { RideMapper.class, BookingMapper.class })
public abstract class PageMapper {
	// Domain page with rides --> Api page of rides
	@Mapping(target = "data", source = "data", qualifiedBy = { RideMapperQualifier.class, RideDetails.class } )
	public abstract eu.netmobiel.rideshare.api.model.Page mapInDetail(PagedResult<Ride> source);

	// Domain page with rides --> Api page of rides
	@Mapping(target = "data", source = "data", qualifiedBy = { RideMapperQualifier.class, RideMyDetails.class } )
	public abstract eu.netmobiel.rideshare.api.model.Page mapMine(PagedResult<Ride> source);

	// Domain page with rides --> Api page of rides
	@Mapping(target = "data", source = "data", qualifiedBy = { RideMapperQualifier.class, RideSearchDetails.class } )
	public abstract eu.netmobiel.rideshare.api.model.Page mapSearch(PagedResult<Ride> source);

	// Domain page with rides --> Api page of rides
	@Mapping(target = "data", source = "data", qualifiedBy = { BookingMapperQualifier.class, BookingNestedMine.class } )
	public abstract eu.netmobiel.rideshare.api.model.Page mapMyBookings(PagedResult<Booking> source);
}
