package eu.netmobiel.planner.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.planner.api.mapping.annotation.ItineraryMapperQualifier;
import eu.netmobiel.planner.api.mapping.annotation.LegDetails;
import eu.netmobiel.planner.api.mapping.annotation.LegMapperQualifier;
import eu.netmobiel.planner.model.Itinerary;

/**
 * Mapper for Itinerary objects
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
		uses = { LegMapper.class, JavaTimeMapper.class })
@ItineraryMapperQualifier
public interface ItineraryMapper {
	
	@Mapping(target = "legs", source = "legs", qualifiedBy = { LegMapperQualifier.class, LegDetails.class })
	@Mapping(target = "removeLegsItem", ignore = true)
    eu.netmobiel.planner.api.model.Itinerary mapDetails(Itinerary source );
}
