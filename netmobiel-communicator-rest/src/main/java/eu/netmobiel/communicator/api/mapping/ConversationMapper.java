package eu.netmobiel.communicator.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.communicator.model.Conversation;

/**
 * This mapper defines the mapping from the domain to the API and vice versa as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class, UserMapper.class, MessageMapper.class })
public abstract class ConversationMapper {

	// Domain --> API 
	public abstract eu.netmobiel.communicator.api.model.Conversation map(Conversation source);

	
	public abstract eu.netmobiel.communicator.api.model.Page map(PagedResult<Conversation> source);

	@Mapping(target = "id", ignore = true)
	public abstract Conversation map(eu.netmobiel.communicator.api.model.Conversation source);

}
