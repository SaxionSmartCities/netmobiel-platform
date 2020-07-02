package eu.netmobiel.planner.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.planner.api.mapping.annotation.ItineraryMapperQualifier;
import eu.netmobiel.planner.api.mapping.annotation.TripDetails;
import eu.netmobiel.planner.api.mapping.annotation.TripMapperQualifier;
import eu.netmobiel.planner.api.mapping.annotation.TripMyDetails;
import eu.netmobiel.planner.model.Trip;

/**
 * This mapper defines the mapping from the domain Trip to the API Trip as defined by OpenAPI
 * Becasue trip are written to and read from the service, a bi-directional mapping is necessary.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { ItineraryMapper.class, JavaTimeMapper.class, GeometryMapper.class })
@TripMapperQualifier
public interface TripMapper {

	// Domain trip --> Api Trip in full detail
	@Mapping(target = "travellerRef", source = "traveller.userRef")
	@Mapping(target = "itinerary", source = "itinerary", qualifiedBy = { ItineraryMapperQualifier.class })
	@TripDetails
	eu.netmobiel.planner.api.model.Trip mapInDetail(Trip source );

	// Domain trip --> Api Trip but without traveller, because these are mine
	@Mapping(target = "traveller", ignore = true)
	@Mapping(target = "travellerRef", ignore = true)
	@Mapping(target = "itinerary", source = "itinerary", qualifiedBy = { ItineraryMapperQualifier.class })
	@TripMyDetails
	eu.netmobiel.planner.api.model.Trip mapMine(Trip source );

	// Api Trip --> Domain trip. Ignore everything that should be set by the backend from the itinerary 
    @Mapping(target = "arrivalTimeIsPinned", ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "from", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "itinerary", ignore = true)
    @Mapping(target = "nrSeats", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "to", ignore = true)
    @Mapping(target = "traveller", ignore = true)
    @Mapping(target = "tripRef", ignore = true)
    Trip map(eu.netmobiel.planner.api.model.Trip source );

}
