package eu.netmobiel.rideshare.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class MessageMapper {

	// Domain Envelope --> API Message 
	@Mapping(target = "deliveryMode", source = "message.deliveryMode")
	@Mapping(target = "body", source = "message.body")
	@Mapping(target = "context", source = "message.context")
	@Mapping(target = "creationTime", source = "message.creationTime")
	@Mapping(target = "recipients", ignore = true)
	@Mapping(target = "sender", source = "message.sender")
	@Mapping(target = "subject", source = "message.subject")
	public abstract eu.netmobiel.communicator.api.model.Message map(Envelope source);
	
	@Mapping(target = "id", ignore = true)
	public abstract Message map(eu.netmobiel.communicator.api.model.Message source);

	// Instant --> OffsetDateTime
    public  OffsetDateTime map(Instant instant) {
    	return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
    
    // OffsetDateTime --> Instant 
    public  Instant  map(OffsetDateTime offsetDateTime) {
    	return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
    
}
