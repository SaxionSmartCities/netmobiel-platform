package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountAll;
import eu.netmobiel.banker.api.mapping.annotation.AccountMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryAccount;
import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.AccountingEntryShallow;
import eu.netmobiel.banker.api.mapping.annotation.CharityDetails;
import eu.netmobiel.banker.api.mapping.annotation.CharityMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.CharityWithRoleAndAccountDetails;
import eu.netmobiel.banker.api.mapping.annotation.DonationDetails;
import eu.netmobiel.banker.api.mapping.annotation.DonationMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.DonationWithCharity;
import eu.netmobiel.banker.api.mapping.annotation.DonationWithUser;
import eu.netmobiel.banker.api.mapping.annotation.IncentiveDetails;
import eu.netmobiel.banker.api.mapping.annotation.IncentiveMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.PaymentBatchMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.PaymentBatchShallow;
import eu.netmobiel.banker.api.mapping.annotation.RewardDetails;
import eu.netmobiel.banker.api.mapping.annotation.RewardMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.RewardShallow;
import eu.netmobiel.banker.api.mapping.annotation.UserCreditDetails;
import eu.netmobiel.banker.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.UserOnlyDetails;
import eu.netmobiel.banker.api.mapping.annotation.WithdrawalRequestMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.WithdrawalRequestPaymentBatch;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.PaymentBatch;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.commons.model.PagedResult;

/**
 * This mapper defines the mapping from the domain PagedResult to the API
 * PagedResult as defined by OpenAPI. One way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, uses = {
		AccountMapper.class, AccountingEntryMapper.class, CharityMapper.class, DonationMapper.class,
		IncentiveMapper.class, PaymentBatchMapper.class, RewardMapper.class, UserMapper.class,
		WithdrawalRequestMapper.class, })
public interface PageMapper {
	@Mapping(target = "data", source = "data", qualifiedBy = { AccountMapperQualifier.class, AccountAll.class })
	eu.netmobiel.banker.api.model.Page mapAccountsAll(PagedResult<Account> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { AccountingEntryMapperQualifier.class,
			AccountingEntryShallow.class })
	eu.netmobiel.banker.api.model.Page mapAccountingEntriesShallow(PagedResult<AccountingEntry> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { AccountingEntryMapperQualifier.class,
			AccountingEntryAccount.class })
	eu.netmobiel.banker.api.model.Page mapAccountingEntriesWithAccount(PagedResult<AccountingEntry> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { CharityMapperQualifier.class,
			CharityWithRoleAndAccountDetails.class })
	eu.netmobiel.banker.api.model.Page mapCharitiesWithRoleAndBalance(PagedResult<Charity> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { CharityMapperQualifier.class, CharityDetails.class })
	eu.netmobiel.banker.api.model.Page mapCharities(PagedResult<Charity> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { DonationMapperQualifier.class, DonationDetails.class })
	eu.netmobiel.banker.api.model.Page mapDonations(PagedResult<Donation> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { DonationMapperQualifier.class,
			DonationWithCharity.class })
	eu.netmobiel.banker.api.model.Page mapDonationsWithCharity(PagedResult<Donation> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { DonationMapperQualifier.class, DonationWithUser.class })
	eu.netmobiel.banker.api.model.Page mapDonationsWithUser(PagedResult<Donation> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { IncentiveMapperQualifier.class, IncentiveDetails.class })
	eu.netmobiel.banker.api.model.Page mapIncentives(PagedResult<Incentive> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { PaymentBatchMapperQualifier.class,
			PaymentBatchShallow.class })
	eu.netmobiel.banker.api.model.Page mapPaymentBatches(PagedResult<PaymentBatch> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { RewardMapperQualifier.class, RewardDetails.class })
	eu.netmobiel.banker.api.model.Page mapRewardsDetailed(PagedResult<Reward> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { RewardMapperQualifier.class, RewardShallow.class })
	eu.netmobiel.banker.api.model.Page mapRewardsShallow(PagedResult<Reward> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { UserMapperQualifier.class, UserCreditDetails.class })
	eu.netmobiel.banker.api.model.Page mapUsers(PagedResult<BankerUser> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	eu.netmobiel.banker.api.model.Page mapUsersWithoutPersonalCredit(PagedResult<BankerUser> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { WithdrawalRequestMapperQualifier.class,
			WithdrawalRequestPaymentBatch.class })
	eu.netmobiel.banker.api.model.Page mapWithdrawalRequests(PagedResult<WithdrawalRequest> source);
}
