package eu.netmobiel.communicator.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.communicator.model.CommunicatorUser;

/**
 * This mapper defines the mapping from the domain to the API as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserMapper {

	// Domain --> API
	eu.netmobiel.communicator.api.model.User map(CommunicatorUser source);

	// API --> Domain
	@Mapping(target = "id", ignore = true)
	CommunicatorUser map(eu.netmobiel.communicator.api.model.User source);

}
