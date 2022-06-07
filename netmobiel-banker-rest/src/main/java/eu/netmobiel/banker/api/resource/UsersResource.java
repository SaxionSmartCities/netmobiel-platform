package eu.netmobiel.banker.api.resource;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.UsersApi;
import eu.netmobiel.banker.api.mapping.AccountMapper;
import eu.netmobiel.banker.api.mapping.AccountingEntryMapper;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.api.mapping.UserMapper;
import eu.netmobiel.banker.api.model.PaymentLink;
import eu.netmobiel.banker.filter.DonationFilter;
import eu.netmobiel.banker.filter.IncentiveFilter;
import eu.netmobiel.banker.filter.RewardFilter;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.DonationSortBy;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.model.TransactionType;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.service.CharityManager;
import eu.netmobiel.banker.service.DepositService;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.service.RewardService;
import eu.netmobiel.banker.service.WithdrawalService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.UrnHelper;

@RequestScoped
public class UsersResource extends BankerResource implements UsersApi {

	@Inject
	private AccountingEntryMapper accountingEntryMapper;

	@Inject
	private AccountMapper accountMapper;

	@Inject
	private UserMapper userMapper;

	@Inject
	private PageMapper pageMapper;

	@Inject
    private LedgerService ledgerService;
	
	@Inject
    private DepositService depositService;
	
	@Inject
    private WithdrawalService withdrawalService;
	
	@Inject
    private CharityManager charityManager;

	@Inject
    private RewardService rewardService;

    @Override
	public Response createPersonalDeposit(String xDelegator, String userId, eu.netmobiel.banker.api.model.DepositRequest deposit) {
		Response rsp = null;
		try {
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			allowAdminOrEffectiveUser(context, user);
			user = bankerUserManager.getUserWithBalance(user.getId());
			String paymentUrl = depositService.createDepositRequest(user.getPersonalAccount(), deposit.getAmountCredits(), deposit.getDescription(), deposit.getReturnUrl());
			PaymentLink plink = new PaymentLink();
			plink.setPaymentUrl(paymentUrl);
			rsp = Response.ok(plink).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response getUser(String xDelegator, String userId) {
		Response rsp = null;
		try {
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			user = bankerUserManager.getUserWithBalance(user.getId());
			rsp = Response.ok(userMapper.map(user)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp; 
	}

	@Override
	public Response listUserStatements(String xDelegator, String userId, OffsetDateTime since, OffsetDateTime until, Integer maxResults, Integer offset) {
		Instant si = toInstant(since);
		Instant ui = toInstant(until);
		Response rsp = null;
		try {
			final TransactionType purpose = null;
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			allowAdminOrEffectiveUser(context, user);
			user = bankerUserManager.getUserWithBalance(user.getId());
			PagedResult<AccountingEntry> result = ledgerService.listAccountingEntries(user.getPersonalAccount().getNcan(), si, ui, purpose, maxResults, offset); 
			rsp = Response.ok(pageMapper.mapAccountingEntriesShallow(result)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response getStatement(String xDelegator, String userId, String entryId) {
		Response rsp = null;
		try {
        	Long eid = UrnHelper.getId(AccountingEntry.URN_PREFIX, entryId);
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			allowAdminOrEffectiveUser(context, user);
			user = bankerUserManager.getUserWithBalance(user.getId());
			AccountingEntry entry = ledgerService.getAccountingEntry(eid);
			if (!entry.getAccount().equals(user.getPersonalAccount()) && !isAdmin.test(request)) {
				throw new SecurityException("Access to resource not allowed by this user");
			}
			rsp = Response.ok(accountingEntryMapper.mapWithAccount(entry)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response reportGenerosity(String user, String charity, String location, Integer radius, Boolean omitInactive,
			OffsetDateTime since, OffsetDateTime until, String sortBy, String sortDir, Integer maxResults,
			Integer offset) {
		Response rsp = null;
		try {
			DonationFilter filter;
			Long userId = null;
			if (user != null) {
				CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
				BankerUser bnuser = resolveUserReference(context, user);
				// Anyone can see the generosity of anyone in Netmobiel
				userId = bnuser.getId();
			}
			if (charity != null) {
				filter = new DonationFilter(charity, userId, since, until, sortBy, sortDir, false);
			} else {
				filter = new DonationFilter(location, radius, Boolean.TRUE.equals(omitInactive), userId, since, until, sortBy, sortDir, false);
			}
			filter.setSortBy(sortBy, DonationSortBy.AMOUNT, new DonationSortBy[] { DonationSortBy.AMOUNT });
			if (filter.getSortDir() == null) {
				filter.setSortDir(SortDirection.DESC);
			}
			Cursor cursor = new Cursor(maxResults, offset);
	    	PagedResult<BankerUser> results = charityManager.reportDonorGenerousityTopN(filter, cursor);
			rsp = Response.ok(pageMapper.mapUsersWithoutPersonalCredit(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}

		return rsp;
	}

	@Override
	public Response reportRecentDonations(String xDelegator, String userId, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			Cursor cursor = new Cursor(maxResults, offset);
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			allowAdminOrEffectiveUser(context, user);
	    	PagedResult<Donation> results = charityManager.reportMostRecentDistinctDonations(user, cursor);
			rsp = Response.ok(pageMapper.mapDonationsWithCharity(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}

		return rsp;
	}

	@Override
	public Response createPersonalWithdrawal(String xDelegator, String userId, eu.netmobiel.banker.api.model.WithdrawalRequest withdrawal) {
		Response rsp = null;
		try {
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			user = bankerUserManager.getUserWithBalance(user.getId());
	    	String caller = request.getUserPrincipal().getName();
			boolean admin = request.isUserInRole("admin");
			if (!admin && ! caller.equals(user.getManagedIdentity())) {
				throw new ForbiddenException("You are not allowed to create a withdrawal request for this user: " + userId);
			}
			Long id = withdrawalService.createWithdrawalRequest(user.getPersonalAccount(), withdrawal.getAmountCredits(), withdrawal.getDescription());
			String wrid = UrnHelper.createUrn(WithdrawalRequest.URN_PREFIX, id);
			rsp = Response.created(URI.create(wrid)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getPersonalAccount(String xDelegator, String userId) {
		Response rsp = null;
		try {
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
        	Account acc = bankerUserManager.getPersonalAccount(user.getId());
			rsp = Response.ok(accountMapper.mapAll(acc)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response updatePersonalAccount(String xDelegator, String userId, eu.netmobiel.banker.api.model.Account acc) {
		Response rsp = null;
		try {
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			allowAdminOrEffectiveUser(context, user);
	    	Account accdom = accountMapper.map(acc);
        	bankerUserManager.updatePersonalUserAccount(user.getId(), accdom);
    		rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response listUserWithdrawalRequests(String xDelegator, String userId, OffsetDateTime since, OffsetDateTime until,
			String status, Integer maxResults, Integer offset) {
		Instant si = toInstant(since);
		Instant ui = toInstant(until);
		Response rsp = null;
		try {
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
        	Account acc = bankerUserManager.getPersonalAccount(user.getId());
			allowAdminOrEffectiveUser(context, user);
			PaymentStatus ps = status == null ? null : PaymentStatus.valueOf(status);
	    	PagedResult<WithdrawalRequest> results = withdrawalService.listWithdrawalRequests(acc.getName(), si, ui, ps, maxResults, offset);
			rsp = Response.ok(pageMapper.mapWithdrawalRequests(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	/**
	 * Cancels the withdrawal request. Only admin and the effective owner of the account can cancel the request. Cancellation is only possible 
	 * if the withdrawal is not yet being processed.
	 */
	@Override
	public Response cancelUserWithdrawalRequest(String xDelegator, String userId, String withdrawalRequestId, String reason) {
		try {
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			allowAdminOrEffectiveUser(context, user);
        	Account acc = bankerUserManager.getPersonalAccount(user.getId());
	    	Long wrid = UrnHelper.getId(WithdrawalRequest.URN_PREFIX, withdrawalRequestId);
			withdrawalService.cancelWithdrawalRequest(acc, wrid, reason);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
	}

	@Override
	public Response listUserRewards(String xDelegator, String userId, Boolean cancelled, String sortDir, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			allowAdminOrEffectiveUser(context, user);
			RewardFilter filter = new RewardFilter(user, cancelled, sortDir);
			Cursor cursor = new Cursor(maxResults, offset);
	    	PagedResult<Reward> results = rewardService.listRewards(Reward.GRAPH_WITH_INCENTIVE, filter, cursor);
			rsp = Response.ok(pageMapper.mapRewardsShallow(results)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
		return rsp;
	}

	@Override
	public Response listCallToActions(String xDelegator, String userId, String sortDir, Integer maxResults,
			Integer offset) {
		Response rsp = null;
		try {
			CallingContext<BankerUser> context = bankerUserManager.findOrRegisterCallingContext(securityIdentity);
			BankerUser user = resolveUserReference(context, userId);
			allowAdminOrEffectiveUser(context, user);
			IncentiveFilter filter = new IncentiveFilter(user, false, false, sortDir == null ? SortDirection.DESC.name() : sortDir); 
			Cursor cursor = new Cursor(maxResults, offset);
	    	PagedResult<Incentive> results = rewardService.listCallToActions(filter, cursor);
			rsp = Response.ok(pageMapper.mapIncentives(results)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
		return rsp;
	}

}
