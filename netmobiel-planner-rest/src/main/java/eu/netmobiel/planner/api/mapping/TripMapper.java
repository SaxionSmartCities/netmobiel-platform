package eu.netmobiel.planner.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import javax.inject.Inject;

import org.mapstruct.AfterMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.api.mapping.annotation.TripDetails;
import eu.netmobiel.planner.api.mapping.annotation.TripMapperQualifier;
import eu.netmobiel.planner.api.mapping.annotation.TripMyDetails;
import eu.netmobiel.planner.model.GuideStep;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Stop;
import eu.netmobiel.planner.model.Trip;

/**
 * This mapper defines the mapping from the domain Trip to the API Trip as defined by OpenAPI
 * Becasue trip are written to and read from the service, a bi-directional mapping is necessary.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
@TripMapperQualifier
public abstract class TripMapper {
	@Inject
	private Logger log;

	// Domain trip --> Api Trip in full detail
	@TripDetails
	public abstract eu.netmobiel.planner.api.model.Trip mapInDetail(Trip source );

	// Domain trip --> Api Trip but without traveller, because these are mine
	@Mapping(target = "traveller", ignore = true)
	@Mapping(target = "travellerRef", ignore = true)
	@TripMyDetails
	public abstract eu.netmobiel.planner.api.model.Trip mapMine(Trip source );

	// Api Trip --> Domain trip. 
    @Mapping(target = "traveller", ignore = true)
    @Mapping(target = "travellerRef", ignore = true)
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "score", ignore = true)
    public abstract Trip map(eu.netmobiel.planner.api.model.Trip source );

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
    public abstract eu.netmobiel.planner.api.model.Leg map(Leg source );

    // API Leg --> Domain Leg
    @InheritInverseConfiguration
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
    
    @AfterMapping
    // Replace the leg list structure with a linear graph, and convert the encoded leg geometry
    public Trip transformIntoLinearGraph(@MappingTarget Trip trip) {
    	Stop previous = null;
    	for (Leg leg: trip.getLegs()) {
    		if (previous == null) {
    			trip.setStops(new ArrayList<>());
    			trip.getStops().add(leg.getFrom());
    		} else if (! previous.equals(leg.getFrom() )) {
    			log.warn(String.format("Leg connecting stop inconsistency detected: Arrival stop was %s, departure stop is %s", previous.toString(), leg.getFrom().toString()));
    			// Hmmm ok, keep the hole between the legs then, what else can we do?
    			trip.getStops().add(leg.getFrom());
    		} else {
    			leg.setFrom(previous);
    		}
			trip.getStops().add(leg.getTo());
    		previous = leg.getTo();
    		// Also decode the encoded legGeometry while we are here
    		leg.decodeLegGeometry();
		}
    	return trip;
    }

}
