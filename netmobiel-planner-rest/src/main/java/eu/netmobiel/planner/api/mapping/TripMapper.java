package eu.netmobiel.planner.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.mapstruct.AfterMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;

import eu.netmobiel.commons.api.EncodedPolylineBean;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.commons.util.PolylineEncoder;
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
public abstract class TripMapper {
	@Inject
	private Logger log;

	// Trip <--> Trip
	public abstract eu.netmobiel.planner.api.model.Trip map(Trip source );

    @Mapping(target = "traveller", ignore = true)
    @Mapping(target = "stops", ignore = true)
    public abstract Trip map(eu.netmobiel.planner.api.model.Trip source );

    // Location <--> GeoLocation
    @Mapping(target = "point", ignore = true)
    public abstract GeoLocation map(eu.netmobiel.planner.api.model.Location source);

    // Stop <-->
    public abstract eu.netmobiel.planner.api.model.Stop map(Stop source );

    @InheritInverseConfiguration
    @Mapping(target = "location", ignore = true)
    public abstract Stop map(eu.netmobiel.planner.api.model.Stop source );

    // Leg <--> Leg
    @Mapping(target = "legGeometry", source = "legGeometryEncoded")
    public abstract eu.netmobiel.planner.api.model.Leg map(Leg source );

    @InheritInverseConfiguration
    @Mapping(target = "intermediateStops", ignore= true)
    @Mapping(target = "legGeometry", ignore= true)
    public abstract Leg map(eu.netmobiel.planner.api.model.Leg source );

    // GuideStep <--> GuideStep
    public abstract eu.netmobiel.planner.api.model.GuideStep map(GuideStep source );

    // EncodedPolylineBean --> MultiPoint
    public MultiPoint map(EncodedPolylineBean encodedPolylineBean) {
    	MultiPoint result = null;
    	if (encodedPolylineBean != null) {
        	List<Coordinate> coords = PolylineEncoder.decode(encodedPolylineBean);
        	result = GeometryHelper.createMultiPoint(coords.toArray(new Coordinate[coords.size()]));
    	}
    	return result;
    }

    // Instant --> OffsetDateTime
    public  OffsetDateTime map(Instant instant) {
    	return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
    
    // OffsetDateTime --> Instant 
    public  Instant  map(OffsetDateTime offsetDateTime) {
    	return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
    
    @AfterMapping
    // Replace the leg list structure with a linear graph
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
		}
    	return trip;
    }

}
