package eu.netmobiel.communicator.api.mapping;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.communicator.model.Envelope;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class, UserMapper.class })
public abstract class EnvelopeMapper {

	@Mapping(target = "conversationRef", source = "conversation.urn")
	public abstract eu.netmobiel.communicator.api.model.Envelope map(Envelope source);

	public List<eu.netmobiel.communicator.api.model.Envelope> map(List<Envelope> source) {
		// Skip the artificial sender envelope 
		if (source != null) {
			return source.stream().filter(env -> !env.isSender())
					.map(env -> map(env))
					.collect(Collectors.toList());
		}
		return null;
	}

}
