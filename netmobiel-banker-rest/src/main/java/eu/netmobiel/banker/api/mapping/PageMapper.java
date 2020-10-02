package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.CharityDetails;
import eu.netmobiel.banker.api.mapping.annotation.CharityMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.CharityWithRoleAndBalanceDetails;
import eu.netmobiel.banker.api.mapping.annotation.DonationDetails;
import eu.netmobiel.banker.api.mapping.annotation.DonationMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.DonationWithCharity;
import eu.netmobiel.banker.api.mapping.annotation.UserCreditDetails;
import eu.netmobiel.banker.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.UserOnlyDetails;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.commons.model.PagedResult;

/**
 * This mapper defines the mapping from the domain PagedResult to the API PagedResult as defined by OpenAPI.
 * One way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
		uses = { AccountingEntryMapper.class, CharityMapper.class, UserMapper.class, DonationMapper.class })
public interface PageMapper {
	@Mapping(target = "data", source = "data", qualifiedBy = { AccountingEntryMapperQualifier.class } )
	eu.netmobiel.banker.api.model.Page mapAccountingEntries(PagedResult<AccountingEntry> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { CharityMapperQualifier.class, CharityWithRoleAndBalanceDetails.class } )
	eu.netmobiel.banker.api.model.Page mapCharitiesWithRoleAndBalance(PagedResult<Charity> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { CharityMapperQualifier.class, CharityDetails.class } )
	eu.netmobiel.banker.api.model.Page mapCharities(PagedResult<Charity> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { UserMapperQualifier.class, UserCreditDetails.class } )
	eu.netmobiel.banker.api.model.Page mapUsers(PagedResult<BankerUser> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class } )
	eu.netmobiel.banker.api.model.Page mapUsersWithoutPersonalCredit(PagedResult<BankerUser> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { DonationMapperQualifier.class, DonationDetails.class } )
	eu.netmobiel.banker.api.model.Page mapDonations(PagedResult<Donation> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { DonationMapperQualifier.class, DonationWithCharity.class } )
	eu.netmobiel.banker.api.model.Page mapDonationWithCharity(PagedResult<Donation> source);
}
