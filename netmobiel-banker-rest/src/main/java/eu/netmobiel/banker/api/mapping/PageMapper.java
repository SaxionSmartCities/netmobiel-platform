package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryMapperQualifier;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.commons.model.PagedResult;

/**
 * This mapper defines the mapping from the domain PagedResult to the API PagedResult as defined by OpenAPI.
 * One way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
		uses = { AccountingEntryMapper.class })
public abstract class PageMapper {
	@Mapping(target = "data", source = "data", qualifiedBy = { AccountingEntryMapperQualifier.class } )
	public abstract eu.netmobiel.banker.api.model.Page mapAccountingEntries(PagedResult<AccountingEntry> source);
}
