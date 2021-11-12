package eu.netmobiel.communicator.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.communicator.api.mapping.annotation.Complete;
import eu.netmobiel.communicator.api.mapping.annotation.ConversationMapperQualifier;
import eu.netmobiel.communicator.api.mapping.annotation.Shallow;
import eu.netmobiel.communicator.model.Conversation;

/**
 * This mapper defines the mapping from the domain to the API and vice versa as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class, UserMapper.class, MessageMapper.class })
@ConversationMapperQualifier
public abstract class ConversationMapper {

	// Domain --> API 
	@Complete
	@Mapping(target = "conversationRef", source = "urn")
	public abstract eu.netmobiel.communicator.api.model.Conversation mapComplete(Conversation source);

	@Shallow
	@Mapping(target = "contexts", ignore = true)
	@Mapping(target = "conversationRef", source = "urn")
	public abstract eu.netmobiel.communicator.api.model.Conversation mapShallow(Conversation source);
	
	@Mapping(target = "id", ignore = true)
	public abstract Conversation map(eu.netmobiel.communicator.api.model.Conversation source);

}
