package eu.netmobiel.planner.api.mapping;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.planner.api.mapping.annotation.ItineraryMapperQualifier;
import eu.netmobiel.planner.api.mapping.annotation.TripPlanDetails;
import eu.netmobiel.planner.api.mapping.annotation.TripPlanMapperQualifier;
import eu.netmobiel.planner.model.TripPlan;

/**
 * Mapping of the domain TripPLan to API TripPlan.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
uses = { ItineraryMapper.class, JavaTimeMapper.class, GeometryMapper.class })
@TripPlanMapperQualifier
public interface TripPlanMapper {
    @Mapping(target = "modalities", source = "traverseModes")
    @Mapping(target = "firstLegRideshare", source = "firstLegRideshareAllowed")
    @Mapping(target = "lastLegRideshare", source = "lastLegRideshareAllowed")
    @Mapping(target = "itineraries", source = "itineraries", qualifiedBy = { ItineraryMapperQualifier.class })
    @TripPlanDetails
    eu.netmobiel.planner.api.model.TripPlan map(TripPlan source );

    @InheritInverseConfiguration
    @Mapping(target = "itineraries", ignore = true)
    @Mapping(target = "plannerReports", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "traveller", ignore = true)
    TripPlan map(eu.netmobiel.planner.api.model.TripPlan source );
}
