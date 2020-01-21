package eu.netmobiel.planner.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;

import eu.netmobiel.commons.api.EncodedPolylineBean;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.commons.util.PolylineEncoder;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Stop;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.GuideStep;

/**
 * This mapper defines the mapping from the domain TripPlan to the API tripPlan as defined by OpenAPI
 * A reverse mapping is not needed because it is one way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface TripPlanMapper {
    @Mapping(target = "modalities", source = "traverseModes")
    eu.netmobiel.planner.api.model.TripPlan map(TripPlan source );

    eu.netmobiel.planner.api.model.Itinerary map(Itinerary source );
    
    @Mapping(target = "legGeometry", source = "legGeometryEncoded")
    eu.netmobiel.planner.api.model.Leg map(Leg source );

    eu.netmobiel.planner.api.model.Stop map(Stop source );

    eu.netmobiel.planner.api.model.GuideStep map(GuideStep source );

    default MultiPoint map(EncodedPolylineBean encodedPolylineBean) {
    	MultiPoint result = null;
    	if (encodedPolylineBean != null) {
        	List<Coordinate> coords = PolylineEncoder.decode(encodedPolylineBean);
        	result = GeometryHelper.createMultiPoint(coords.toArray(new Coordinate[coords.size()]));
    	}
    	return result;
    }
    default OffsetDateTime map(Instant instant) {
    	return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
