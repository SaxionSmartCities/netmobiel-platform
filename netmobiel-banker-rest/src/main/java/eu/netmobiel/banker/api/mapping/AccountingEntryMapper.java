package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryMapperQualifier;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.commons.model.PagedResult;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class })
@AccountingEntryMapperQualifier
public interface AccountingEntryMapper {

	@Mapping(target = "type", source = "entryType")
	@Mapping(target = "accountName", source = "account.name")
	@Mapping(target = "ncan", source = "account.ncan")
	@Mapping(target = "accountingTime", source = "transaction.accountingTime")
	@Mapping(target = "transactionTime", source = "transaction.transactionTime")
	@Mapping(target = "description", source = "transaction.description")
	@Mapping(target = "transactionType", source = "transaction.transactionType")
	@Mapping(target = "context", source = "transaction.context")
	@Mapping(target = "counterparty", source = "counterparty.name")
	eu.netmobiel.banker.api.model.Statement map(AccountingEntry source);
	
	eu.netmobiel.banker.api.model.Page map(PagedResult<AccountingEntry> source);

}
