package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.AccountMinimal;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.commons.model.PagedResult;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class, UserMapper.class, AccountMapper.class })
public interface BalanceMapper {

	// Domain --> API
	@Mapping(target = "ledger", source = "ledger.name")
	@Mapping(target = "account", source = "account", qualifiedBy = { AccountMapperQualifier.class, AccountMinimal.class })
	eu.netmobiel.banker.api.model.Balance map(Balance source);
	
	eu.netmobiel.banker.api.model.Page map(PagedResult<Balance> source);

}
