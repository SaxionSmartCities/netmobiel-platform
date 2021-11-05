package eu.netmobiel.communicator.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class, UserMapper.class })
public abstract class MessageMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "pushTime", ignore = true)
	@Mapping(target = "ackTime", ignore = true)
	@Mapping(target = "message", ignore = true)
	public abstract Envelope map(eu.netmobiel.communicator.api.model.Envelope source);

	public abstract eu.netmobiel.communicator.api.model.Envelope map(Envelope source);
	
	// Domain Envelope --> API Message 
	public abstract eu.netmobiel.communicator.api.model.Message map(Message source);

	
	public abstract eu.netmobiel.communicator.api.model.Page map(PagedResult<Message> source);

	@Mapping(target = "id", ignore = true)
	public abstract Message map(eu.netmobiel.communicator.api.model.Message source);

}
