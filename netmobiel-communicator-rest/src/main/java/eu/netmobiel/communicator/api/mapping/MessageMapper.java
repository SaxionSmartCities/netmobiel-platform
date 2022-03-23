package eu.netmobiel.communicator.api.mapping;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.communicator.model.Message;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class, UserMapper.class, EnvelopeMapper.class })
public abstract class MessageMapper {
	// Domain Message --> API Message 
	public abstract eu.netmobiel.communicator.api.model.Message map(Message source);

	
	public abstract eu.netmobiel.communicator.api.model.Page map(PagedResult<Message> source);

	@Mapping(target = "id", ignore = true)
	public abstract Message map(eu.netmobiel.communicator.api.model.Message source);

	@AfterMapping
    protected void attachEnvelopesToMessage(eu.netmobiel.communicator.api.model.Message source, @MappingTarget Message target) {
		// Assure the envelopes have an association with the Message.
		target.getEnvelopes().forEach(env -> env.setMessage(target));
	}
}
