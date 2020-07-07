package eu.netmobiel.planner.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.planner.api.mapping.annotation.LegDetails;
import eu.netmobiel.planner.api.mapping.annotation.LegMapperQualifier;
import eu.netmobiel.planner.api.mapping.annotation.LegShallow;
import eu.netmobiel.planner.model.GuideStep;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Stop;

/**
 * This mapper defines the mapping from the domain Leg to the API Leg as defined by OpenAPI and vice versa.
 * Because trip are written to and read from the service, a bi-directional mapping is necessary.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
		uses = { JavaTimeMapper.class, GeometryMapper.class })
@LegMapperQualifier
public interface LegMapper {

    // Domain Stop --> API Stop
    eu.netmobiel.planner.api.model.Stop map(Stop source );

    // Domain Leg --> API Leg
    @LegDetails
    @Mapping(target = "guideSteps", ignore = true)
    eu.netmobiel.planner.api.model.Leg mapDetails(Leg source );

    // Domain Leg --> API Leg
    @Mapping(target = "legGeometry", ignore = true)
    @Mapping(target = "guideSteps", ignore = true)
    @LegShallow
    eu.netmobiel.planner.api.model.Leg mapShallow(Leg source );

    // GuideStep <--> GuideStep
    eu.netmobiel.planner.api.model.GuideStep map(GuideStep source );

}
