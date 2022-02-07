package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.AccountMinimal;
import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryAccount;
import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryShallow;
import eu.netmobiel.banker.model.AccountingEntry;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class, AccountMapper.class })
@AccountingEntryMapperQualifier
public interface AccountingEntryMapper {

	@Mapping(target = "type", source = "entryType")
	@Mapping(target = "accountingTime", source = "transaction.accountingTime")
	@Mapping(target = "transactionTime", source = "transaction.transactionTime")
	@Mapping(target = "description", source = "transaction.description")
	@Mapping(target = "transactionType", source = "purpose")
	@Mapping(target = "context", source = "transaction.context")
	@Mapping(target = "rollback", source = "transaction.rollback")
	@Mapping(target = "counterparty", source = "counterparty", qualifiedBy = { AccountMapperQualifier.class, AccountMinimal.class } )
	@Mapping(target = "account", source = "account", qualifiedBy = { AccountMapperQualifier.class, AccountMinimal.class } )
	@AccountingEntryAccount
	eu.netmobiel.banker.api.model.Statement mapWithAccount(AccountingEntry source);

	@Mapping(target = "type", source = "entryType")
	@Mapping(target = "accountingTime", source = "transaction.accountingTime")
	@Mapping(target = "transactionTime", source = "transaction.transactionTime")
	@Mapping(target = "description", source = "transaction.description")
	@Mapping(target = "transactionType", source = "purpose")
	@Mapping(target = "context", source = "transaction.context")
	@Mapping(target = "rollback", source = "transaction.rollback")
	@Mapping(target = "counterparty", source = "counterparty", qualifiedBy = { AccountMapperQualifier.class, AccountMinimal.class } )
	@Mapping(target = "account", ignore = true )
	@AccountingEntryShallow
	eu.netmobiel.banker.api.model.Statement mapWithoutAccount(AccountingEntry source);

}
