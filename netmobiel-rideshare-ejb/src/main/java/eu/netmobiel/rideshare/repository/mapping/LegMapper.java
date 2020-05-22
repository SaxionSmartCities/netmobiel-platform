package eu.netmobiel.rideshare.repository.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.inject.Inject;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.slf4j.Logger;

import eu.netmobiel.opentripplanner.api.model.Leg;
import eu.netmobiel.opentripplanner.api.model.Place;
import eu.netmobiel.rideshare.model.Stop;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class LegMapper {
	@Inject
	private Logger log;

    @Mapping(target = "label", source = "name")
    @Mapping(target = "latitude", source = "lat")
    @Mapping(target = "longitude", source = "lon")
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "arrivalTime", source = "arrival")
    @Mapping(target = "departureTime", source = "departure")
    public abstract Stop placeToStop(Place source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "legGeometry", ignore = true)
    @Mapping(target = "legGeometryEncoded", source = "legGeometry")
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "legIx", ignore = true)
    public abstract eu.netmobiel.rideshare.model.Leg legToLeg(Leg leg);
    
    public abstract eu.netmobiel.rideshare.model.Leg[] legsToLegs(Leg[] leg);

    public OffsetDateTime map(Instant instant) {
    	return instant.atOffset(ZoneOffset.UTC);
    }

    @AfterMapping
    // Replace the leg list structure with a linear graph
    public eu.netmobiel.rideshare.model.Leg[] transformIntoLinearGraph(@MappingTarget eu.netmobiel.rideshare.model.Leg[] legs) {
    	Stop previous = null;
    	int ix = 0;
    	for (eu.netmobiel.rideshare.model.Leg leg: legs) {
    		leg.setLegIx(ix++);
    		if (previous == null) {
    			// keep the stop
    		} else if (! previous.equals(leg.getFrom())) {
    			log.warn(String.format("Leg connecting stop inconsistency detected: Arrival stop was %s, departure stop is %s", previous.toString(), leg.getFrom().toString()));
    			// Hmmm well, ignore the difference for, just connect. What else can we do?
    			leg.setFrom(previous);
    		}
    		previous = leg.getTo();
		}
    	return legs;
    }

}
