package eu.netmobiel.planner.repository.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.opentripplanner.api.model.Itinerary;
import eu.netmobiel.opentripplanner.api.model.Leg;
import eu.netmobiel.opentripplanner.api.model.Place;
import eu.netmobiel.opentripplanner.api.model.TripPlan;
import eu.netmobiel.opentripplanner.api.model.WalkStep;
import eu.netmobiel.planner.model.Stop;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface TripPlanMapper {
    @Mapping(target = "useDateAsArrivalTime", ignore = true)
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
}
