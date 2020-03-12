package eu.netmobiel.planner.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.planner.api.mapping.annotation.TripDetails;
import eu.netmobiel.planner.api.mapping.annotation.TripMapperQualifier;
import eu.netmobiel.planner.api.mapping.annotation.TripMyDetails;
import eu.netmobiel.planner.model.Trip;

/**
 * This mapper defines the mapping from the domain PagedResult to the API PagedResult as defined by OpenAPI.
 * One way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
		uses = { TripMapper.class })
public abstract class PageMapper {
	// Domain page with trips --> Api page of trips
	@Mapping(target = "data", source = "data", qualifiedBy = { TripMapperQualifier.class, TripDetails.class } )
	public abstract eu.netmobiel.planner.api.model.Page mapInDetail(PagedResult<Trip> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { TripMapperQualifier.class, TripMyDetails.class } )
	public abstract eu.netmobiel.planner.api.model.Page mapMine(PagedResult<Trip> source);

}
