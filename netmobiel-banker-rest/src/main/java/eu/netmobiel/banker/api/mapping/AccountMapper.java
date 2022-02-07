package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountAll;
import eu.netmobiel.banker.api.mapping.annotation.AccountMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.AccountMinimal;
import eu.netmobiel.banker.model.Account;

/**
 * This mapper defines the mapping from the domain Account to the API Account as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class })
@AccountMapperQualifier
public interface AccountMapper {

	// Domain --> API
	@Mapping(target = "credits", ignore = true)
	@Mapping(target = "iban", ignore = true)
	@Mapping(target = "ibanHolder", ignore = true)
//	@Mapping(target = "id", ignore = true)
	@Mapping(target = "ncan", ignore = true)
	@Mapping(target = "purpose", ignore = true)
	@AccountMinimal
	eu.netmobiel.banker.api.model.Account mapMinimal(Account acc);

	@Mapping(target = "credits", source = "actualBalance.endAmount")
//	@Mapping(target = "id", ignore = true)
	@AccountAll
	eu.netmobiel.banker.api.model.Account mapAll(Account acc);

	// API --> Domain
	@Mapping(target = "accountType", ignore = true)
	@Mapping(target = "actualBalance", ignore = true)
	@Mapping(target = "balances", ignore = true)
	@Mapping(target = "closedTime", ignore = true)
	@Mapping(target = "createdTime", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "ncan", ignore = true)
	@Mapping(target = "purpose", ignore = true)
	Account map(eu.netmobiel.banker.api.model.Account acc);
}
