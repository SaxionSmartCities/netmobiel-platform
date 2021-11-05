package eu.netmobiel.communicator.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * This mapper defines the mapping from the domain Leg to the API Leg as defined by OpenAPI and vice versa.
 * Because trip are written to and read from the service, a bi-directional mapping is necessary.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface JavaTimeMapper {

    // Instant --> OffsetDateTime
    default OffsetDateTime map(Instant instant) {
    	return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
    // OffsetDateTime --> Instant 
    default Instant map(OffsetDateTime offsetDateTime) {
    	return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
    
}
