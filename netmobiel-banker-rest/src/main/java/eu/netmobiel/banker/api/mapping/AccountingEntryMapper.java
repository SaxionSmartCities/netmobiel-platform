package eu.netmobiel.banker.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
@AccountingEntryMapperQualifier
public abstract class AccountingEntryMapper {

	@Mapping(target = "type", source = "entryType")
	@Mapping(target = "accountName", source = "account.name")
	@Mapping(target = "accountReference", source = "account.reference")
	@Mapping(target = "accountingTime", source = "transaction.accountingTime")
	@Mapping(target = "transactionTime", source = "transaction.transactionTime")
	@Mapping(target = "description", source = "transaction.description")
	@Mapping(target = "transactionType", source = "transaction.transactionType")
	@Mapping(target = "merchantReference", source = "transaction.reference")
	public abstract eu.netmobiel.banker.api.model.Statement map(AccountingEntry source);
	
	public abstract eu.netmobiel.banker.api.model.Page map(PagedResult<AccountingEntry> source);

	// Instant --> OffsetDateTime
    public  OffsetDateTime map(Instant instant) {
    	return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
    
    // OffsetDateTime --> Instant 
    public  Instant  map(OffsetDateTime offsetDateTime) {
    	return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
    
}
