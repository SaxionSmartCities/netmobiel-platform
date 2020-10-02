package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.commons.model.PagedResult;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class, UserMapper.class })
public interface BalanceMapper {

//	@Mapping(target = "id", ignore = true)
	@Mapping(target = "ledger", source = "ledger.name")
	@Mapping(target = "ncan", source = "account.ncan")
	@Mapping(target = "accountName", source = "account.name")
	eu.netmobiel.banker.api.model.Balance map(Balance source);
	
	eu.netmobiel.banker.api.model.Page map(PagedResult<Balance> source);

}
