package eu.netmobiel.rideshare.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.api.mapping.annotation.LegDetails;
import eu.netmobiel.rideshare.api.mapping.annotation.LegMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.LegReference;
import eu.netmobiel.rideshare.model.Leg;

/**
 * This mapper defines the mapping from the domain Leg to the API Leg as defined by OpenAPI. There is never a conversion the other way.
 * Two variants are available:
 * ReferenceOnly: Use LegReference: only list the URN.
 * Details: Use LegDetails. The leg is completely listed, but the bookings are always omitted.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { BookingMapper.class })
@LegMapperQualifier
public abstract class LegMapper {

	// Domain Leg --> API Leg
    @Mapping(target = "legGeometry", source = "legGeometryEncoded")
    @LegDetails
	public abstract eu.netmobiel.rideshare.api.model.Leg mapDetails(Leg source);

    @LegReference
	public List<String> mapReferences(List<Leg> source) {
		return source.stream().map(Leg::getLegRef).collect(Collectors.toList());
	}

    // Instant --> OffsetDateTime
    public  OffsetDateTime map(Instant instant) {
    	return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

}
