package eu.netmobiel.communicator.api.mapping;

import org.mapstruct.Mapper;
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

}
