package eu.netmobiel.profile.repository.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * This mapper defines the mapping some java.time types.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface JavaTimeMapper {

    // OffsetDateTime --> Instant 
    default Instant map(OffsetDateTime offsetDateTime) {
    	return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
    
}
