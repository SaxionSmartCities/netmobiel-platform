package eu.netmobiel.banker.api.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.User;
import eu.netmobiel.commons.model.PagedResult;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class BalanceMapper {

	public abstract eu.netmobiel.banker.api.model.User map(User source);

//	@Mapping(target = "id", ignore = true)
	@Mapping(target = "ledger", source = "ledger.name")
	@Mapping(target = "account", source = "account.reference")
	@Mapping(target = "userRef", source = "account.holder.managedIdentity")
	public abstract eu.netmobiel.banker.api.model.Balance map(Balance source);
	
	public abstract eu.netmobiel.banker.api.model.Page map(PagedResult<Balance> source);

	// Instant --> OffsetDateTime
    public  OffsetDateTime map(Instant instant) {
    	return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
    
    // OffsetDateTime --> Instant 
    public  Instant  map(OffsetDateTime offsetDateTime) {
    	return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
    
}
