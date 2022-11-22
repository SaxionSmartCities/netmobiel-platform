package eu.netmobiel.planner.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.planner.api.mapping.annotation.TripDetails;
import eu.netmobiel.planner.api.mapping.annotation.TripMapperQualifier;
import eu.netmobiel.planner.api.mapping.annotation.TripMyDetails;
import eu.netmobiel.planner.api.mapping.annotation.TripPlanDetails;
import eu.netmobiel.planner.api.mapping.annotation.TripPlanMapperQualifier;
import eu.netmobiel.planner.api.mapping.annotation.TripPlanShallow;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;

/**
 * This mapper defines the mapping from the domain PagedResult to the API PagedResult as defined by OpenAPI.
 * One way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
		uses = { TripPlanMapper.class , TripMapper.class })
public abstract class PageMapper {
	// Domain page with trips --> Api page of trips
	@Mapping(target = "data", source = "data", qualifiedBy = { TripMapperQualifier.class, TripDetails.class } )
	@Mapping(target = "removeDataItem", ignore = true)
	public abstract eu.netmobiel.planner.api.model.Page mapInDetail(PagedResult<Trip> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { TripMapperQualifier.class, TripMyDetails.class } )
	@Mapping(target = "removeDataItem", ignore = true)
	public abstract eu.netmobiel.planner.api.model.Page mapMine(PagedResult<Trip> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { TripPlanMapperQualifier.class, TripPlanDetails.class } )
	@Mapping(target = "removeDataItem", ignore = true)
	public abstract eu.netmobiel.planner.api.model.Page mapPlans(PagedResult<TripPlan> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { TripPlanMapperQualifier.class, TripPlanShallow.class } )
	@Mapping(target = "removeDataItem", ignore = true)
	public abstract eu.netmobiel.planner.api.model.Page mapShoutOutPlans(PagedResult<TripPlan> source);
}
