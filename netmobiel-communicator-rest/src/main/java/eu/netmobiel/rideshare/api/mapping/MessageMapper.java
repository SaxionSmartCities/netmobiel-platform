package eu.netmobiel.rideshare.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.User;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class MessageMapper {

	public abstract eu.netmobiel.communicator.api.model.User map(User source);
	public abstract User map(eu.netmobiel.communicator.api.model.User source);
	
	// Domain Envelope --> API Message 
	@Mapping(target = "deliveryMode", source = "message.deliveryMode")
	@Mapping(target = "body", source = "message.body")
	@Mapping(target = "context", source = "message.context")
	@Mapping(target = "creationTime", source = "message.creationTime")
	@Mapping(target = "recipients", ignore = true)
	@Mapping(target = "sender", source = "message.sender")
	@Mapping(target = "subject", source = "message.subject")
	public abstract eu.netmobiel.communicator.api.model.Message map(Envelope source);

	
	public abstract eu.netmobiel.communicator.api.model.Page map(PagedResult<Envelope> source);

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
