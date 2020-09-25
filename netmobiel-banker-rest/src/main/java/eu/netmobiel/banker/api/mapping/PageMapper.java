package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.CharityDetails;
import eu.netmobiel.banker.api.mapping.annotation.CharityMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.CharityWithRoleDetails;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.commons.model.PagedResult;

/**
 * This mapper defines the mapping from the domain PagedResult to the API PagedResult as defined by OpenAPI.
 * One way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
		uses = { AccountingEntryMapper.class, CharityMapper.class })
public interface PageMapper {
	@Mapping(target = "data", source = "data", qualifiedBy = { AccountingEntryMapperQualifier.class } )
	eu.netmobiel.banker.api.model.Page mapAccountingEntries(PagedResult<AccountingEntry> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { CharityMapperQualifier.class, CharityWithRoleDetails.class } )
	eu.netmobiel.banker.api.model.Page mapCharitiesWithRoles(PagedResult<Charity> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { CharityMapperQualifier.class, CharityDetails.class } )
	eu.netmobiel.banker.api.model.Page mapCharities(PagedResult<Charity> source);
}
