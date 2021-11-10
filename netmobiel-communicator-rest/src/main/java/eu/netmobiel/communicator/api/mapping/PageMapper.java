package eu.netmobiel.communicator.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.communicator.api.mapping.annotation.ConversationMapperQualifier;
import eu.netmobiel.communicator.api.mapping.annotation.Shallow;
import eu.netmobiel.communicator.model.Conversation;

/**
 * This mapper defines the mapping from the domain PagedResult to the API PagedResult as defined by OpenAPI.
 * One way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
uses = { JavaTimeMapper.class, ConversationMapper.class, MessageMapper.class })
public abstract class PageMapper {

	// Domain page with conversation --> Api page of conversation
	@Mapping(target = "data", source = "data", qualifiedBy = { ConversationMapperQualifier.class, Shallow.class } )
	public abstract eu.netmobiel.communicator.api.model.Page mapShallow(PagedResult<Conversation> source);

}
