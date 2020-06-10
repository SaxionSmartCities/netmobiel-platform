package eu.netmobiel.planner.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;
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
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
@LegMapperQualifier
public abstract class LegMapper {

    // Location <--> GeoLocation
    @Mapping(target = "point", ignore = true)
    public abstract GeoLocation map(eu.netmobiel.planner.api.model.Location source);

    // Stop <-->
    public abstract eu.netmobiel.planner.api.model.Stop map(Stop source );

    @InheritInverseConfiguration
    @Mapping(target = "location", ignore = true)
    public abstract Stop map(eu.netmobiel.planner.api.model.Stop source );

    // Domain Leg --> API Leg
    @Mapping(target = "legGeometry", source = "legGeometryEncoded")
    public abstract eu.netmobiel.planner.api.model.Leg mapDetails(Leg source );

    // Domain Leg --> API Leg
    @Mapping(target = "legGeometry", ignore = true)
    @Mapping(target = "guideSteps", ignore = true)
    @LegShallow
    public abstract eu.netmobiel.planner.api.model.Leg mapShallow(Leg source );

    // API Leg --> Domain Leg
//    @InheritInverseConfiguration
    @Mapping(target = "intermediateStops", ignore = true)
    @Mapping(target = "legGeometry", ignore = true)
    @Mapping(target = "legGeometryEncoded", source = "legGeometry")
    public abstract Leg map(eu.netmobiel.planner.api.model.Leg source );

    // GuideStep <--> GuideStep
    public abstract eu.netmobiel.planner.api.model.GuideStep map(GuideStep source );

    // EncodedPolylineBean --> MultiPoint
//    public MultiPoint map(EncodedPolylineBean encodedPolylineBean) {
//    	MultiPoint result = null;
//    	if (encodedPolylineBean != null) {
//        	List<Coordinate> coords = PolylineEncoder.decode(encodedPolylineBean);
//        	result = GeometryHelper.createMultiPoint(coords.toArray(new Coordinate[coords.size()]));
//    	}
//    	return result;
//    }

    // Instant --> OffsetDateTime
    public  OffsetDateTime map(Instant instant) {
    	return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
    
    // OffsetDateTime --> Instant 
    public  Instant  map(OffsetDateTime offsetDateTime) {
    	return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
    
}
