package eu.netmobiel.planner.repository.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import javax.inject.Inject;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.opentripplanner.api.model.Itinerary;
import eu.netmobiel.opentripplanner.api.model.Leg;
import eu.netmobiel.opentripplanner.api.model.Place;
import eu.netmobiel.opentripplanner.api.model.TripPlan;
import eu.netmobiel.opentripplanner.api.model.WalkStep;
import eu.netmobiel.planner.model.Stop;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class TripPlanMapper {
	@Inject
	private Logger log;

	@Mapping(target = "earliestDepartureTime", ignore = true)
    @Mapping(target = "latestArrivalTime", ignore = true)
    @Mapping(target = "maxWalkDistance", ignore = true)
    @Mapping(target = "nrSeats", ignore = true)
    @Mapping(target = "traverseModes", ignore = true)
    @Mapping(target = "maxTransfers", ignore = true)
    @Mapping(target = "firstLegRideshareAllowed", ignore = true)
    @Mapping(target = "lastLegRideshareAllowed", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "planType", ignore = true)
    @Mapping(target = "requestTime", ignore = true)
    @Mapping(target = "requestDuration", ignore = true)
    @Mapping(target = "travelTime", source = "date")
    @Mapping(target = "traveller", ignore = true)
    @Mapping(target = "plannerReports", ignore = true)
    @Mapping(target = "useAsArrivalTime", ignore = true)
    public abstract eu.netmobiel.planner.model.TripPlan map(TripPlan source );

    @Mapping(target = "label", source = "name")
    @Mapping(target = "latitude", source = "lat")
    @Mapping(target = "longitude", source = "lon")
    @Mapping(target = "point", ignore = true)
    public abstract GeoLocation placeToGeoLocation(Place source );

    @Mapping(target = "label", source = "name")
    @Mapping(target = "latitude", source = "lat")
    @Mapping(target = "longitude", source = "lon")
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "arrivalTime", source = "arrival")
    @Mapping(target = "departureTime", source = "departure")
    public abstract Stop placeToStop(Place source);

    @Mapping(target = "arrivalTime", source = "endTime")
    @Mapping(target = "departureTime", source = "startTime")
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tripPlan", ignore = true)
    public abstract eu.netmobiel.planner.model.Itinerary itineraryToItinerary(Itinerary itinerary);

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driverId", ignore = true)
    @Mapping(target = "driverName", ignore = true)
    @Mapping(target = "legGeometry", ignore = true)
    @Mapping(target = "legGeometryEncoded", source = "legGeometry")
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "traverseMode", source = "mode")
    @Mapping(target = "vehicleId", ignore = true)
    @Mapping(target = "vehicleName", ignore = true)
    @Mapping(target = "vehicleLicensePlate", ignore = true)
    @Mapping(target = "guideSteps", source = "walkSteps")
    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "bookingRequired", ignore = true)
    @Mapping(target = "plannerReport", ignore = true)
    public abstract eu.netmobiel.planner.model.Leg legToLeg(Leg leg);
    
    @Mapping(target = "name", source = "streetName")
    @Mapping(target = "latitude", source = "lat")
    @Mapping(target = "longitude", source = "lon")
    public abstract eu.netmobiel.planner.model.GuideStep guideStepToGuideStep(WalkStep step);
    
    public OffsetDateTime map(Instant instant) {
    	return instant.atOffset(ZoneOffset.UTC);
    }

    @AfterMapping
    // Replace the leg list structure with a linear graph
    public eu.netmobiel.planner.model.TripPlan transformIntoLinearGraph(@MappingTarget eu.netmobiel.planner.model.TripPlan plan) {
    	plan.getItineraries().forEach(it -> transformIntoLinearGraph(it));
//    	log.info("Completed the mapping from OTP");
    	return plan;
    }

    private void transformIntoLinearGraph(eu.netmobiel.planner.model.Itinerary itinerary) {
    	Stop previous = null;
    	for (eu.netmobiel.planner.model.Leg leg: itinerary.getLegs()) {
    		if (previous == null) {
    			itinerary.setStops(new ArrayList<>());
    			itinerary.getStops().add(leg.getFrom());
    		} else {
    			if (! previous.equals(leg.getFrom() )) {
        			long distance = Math.round(previous.getLocation().getDistanceFlat(leg.getFrom().getLocation()) * 1000);
        			if (distance > 100) {
        				log.warn(String.format("Leg connecting stop inconsistency detected: Arrival stop was %s, departure stop is %s, displacement = %d meter", 
        						previous.toString(), leg.getFrom().toString(), distance));
        			}
    			}
    			// Nevermind the gap, we must close the graph
    			leg.setFrom(previous);
    		}
			itinerary.getStops().add(leg.getTo());
    		previous = leg.getTo();
		}
    }
}
