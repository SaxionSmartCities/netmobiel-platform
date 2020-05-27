package eu.netmobiel.rideshare.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.model.Stop;

/**
 * This mapper defines the mapping from the domain Stop to the API Stop as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class StopMapper {

	// Domain Stop --> API Stop
	public abstract eu.netmobiel.rideshare.api.model.Stop map(Stop source);

    // Instant --> OffsetDateTime
    public  OffsetDateTime map(Instant instant) {
    	return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
   
}
