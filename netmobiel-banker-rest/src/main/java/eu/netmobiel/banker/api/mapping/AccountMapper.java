package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.model.Account;

/**
 * This mapper defines the mapping from the domain Account to the API Account as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class })
public interface AccountMapper {

	// Domain --> API
	@Mapping(target = "credits", source = "actualBalance.endAmount")
	eu.netmobiel.banker.api.model.Account map(Account acc);

	// API --> Domain
	@Mapping(target = "accountType", ignore = true)
	@Mapping(target = "actualBalance", ignore = true)
	@Mapping(target = "balances", ignore = true)
	@Mapping(target = "closedTime", ignore = true)
	@Mapping(target = "createdTime", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "ncan", ignore = true)
	Account map(eu.netmobiel.banker.api.model.Account acc);
}