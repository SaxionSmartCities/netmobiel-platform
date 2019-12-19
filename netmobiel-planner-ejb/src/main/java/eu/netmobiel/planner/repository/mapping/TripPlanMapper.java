package eu.netmobiel.planner.repository.mapping;

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
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.commons.util.PolylineEncoder;
import eu.netmobiel.opentripplanner.api.model.Itinerary;
import eu.netmobiel.opentripplanner.api.model.Leg;
import eu.netmobiel.opentripplanner.api.model.Place;
import eu.netmobiel.opentripplanner.api.model.TripPlan;
import eu.netmobiel.opentripplanner.api.model.WalkStep;
import eu.netmobiel.planner.model.Stop;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface TripPlanMapper {
    @Mapping(target = "requestedDepartureTime", source = "date")
    @Mapping(target = "requestedArrivalTime", ignore = true)
    eu.netmobiel.planner.model.TripPlan map(TripPlan source );

    @Mapping(target = "label", source = "name")
    @Mapping(target = "latitude", source = "lat")
    @Mapping(target = "longitude", source = "lon")
    @Mapping(target = "point", ignore = true)
    GeoLocation placeToGeoLocation(Place source );

    @Mapping(target = "label", source = "name")
    @Mapping(target = "latitude", source = "lat")
    @Mapping(target = "longitude", source = "lon")
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "arrivalTime", source = "arrival")
    @Mapping(target = "departureTime", source = "departure")
    Stop placeToStop(Place source);

    @Mapping(target = "arrivalTime", source = "endTime")
    @Mapping(target = "departureTime", source = "startTime")
    @Mapping(target = "score", ignore = true)
    eu.netmobiel.planner.model.Itinerary itineraryToItinerary(Itinerary itinerary);

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driverId", ignore = true)
    @Mapping(target = "driverName", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "traverseMode", source = "mode")
    @Mapping(target = "vehicleId", ignore = true)
    @Mapping(target = "vehicleName", ignore = true)
    @Mapping(target = "vehicleLicensePlate", ignore = true)
    eu.netmobiel.planner.model.Leg legToLeg(Leg leg);
    
    @Mapping(target = "name", source = "streetName")
    @Mapping(target = "latitude", source = "lat")
    @Mapping(target = "longitude", source = "lon")
    eu.netmobiel.planner.model.WalkStep walkStepToWalkStep(WalkStep step);
    
    default MultiPoint map(EncodedPolylineBean encodedPolylineBean) {
    	MultiPoint result = null;
    	if (encodedPolylineBean != null) {
        	List<Coordinate> coords = PolylineEncoder.decode(encodedPolylineBean);
        	result = GeometryHelper.createMultiPoint(coords.toArray(new Coordinate[coords.size()]));
    	}
    	return result;
    }
    default OffsetDateTime map(Instant instant) {
    	return instant.atOffset(ZoneOffset.UTC);
    }
}
