package eu.netmobiel.banker.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.ejb.EJBAccessException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import eu.netmobiel.banker.api.CharitiesApi;
import eu.netmobiel.banker.api.mapping.AccountMapper;
import eu.netmobiel.banker.api.mapping.AccountingEntryMapper;
import eu.netmobiel.banker.api.mapping.CharityMapper;
import eu.netmobiel.banker.api.mapping.DonationMapper;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.filter.DonationFilter;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharitySortBy;
import eu.netmobiel.banker.model.CharityUserRoleType;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.DonationSortBy;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.CharityManager;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.UrnHelper;

@RequestScoped
public class CharitiesResource implements CharitiesApi {

	@Inject
    private CharityManager charityManager;

	@Inject
    private BankerUserManager userManager;

	@Inject
	private AccountingEntryMapper accountingEntryMapper;

	@Inject
	private AccountMapper accountMapper;

	@Inject
	private CharityMapper charityMapper;

	@Inject
	private DonationMapper donationMapper;

	@Inject
	private PageMapper pageMapper;

	@Inject
    private LedgerService ledgerService;
	
	@Context
	private HttpServletRequest request;

    protected BankerUser resolveUserReference(String userId, boolean createIfNeeded) {
		BankerUser user = null;
		if ("me".equals(userId)) {
			user = createIfNeeded ? userManager.registerCallingUser() : userManager.findCallingUser();
		} else {
			user = userManager
					.resolveUrn(userId)
					.orElseThrow(() -> new NotFoundException("No such user: " + userId));
		}
		return user;
    }

	@Override
	public Response getCharity(String charityId) {
    	Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	Charity charity = charityManager.getCharity(cid);
			boolean adminView = request.isUserInRole("admin");
			rsp = Response.ok(adminView ? charityMapper.mapWithRolesAndAccount(charity) : charityMapper.mapWithoutRoles(charity)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response createCharity(eu.netmobiel.banker.api.model.Charity charity) {
    	Response rsp = null;
		// The calling user will become a manager of the charity
		try {
			BankerUser user = userManager.registerCallingUser();
			Charity dch = charityMapper.map(charity);
			String newCharityId = BankerUrnHelper.createUrn(Charity.URN_PREFIX, charityManager.createCharity(user, dch));
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newCharityId)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response listCharities(String location, Integer radius, OffsetDateTime since, OffsetDateTime until, Boolean closedToo,
			String sortBy, String sortDir, Integer maxResults, Integer offset) {
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
			SortDirection sortDirEnum = sortDir == null ? SortDirection.ASC : SortDirection.valueOf(sortDir);
			CharitySortBy sortByEnum = sortBy == null ? CharitySortBy.NAME : CharitySortBy.valueOf(sortBy);
			GeoLocation centerLocation = location == null ? null : GeoLocation.fromString(location);
			boolean adminView = request.isUserInRole("admin");
	    	PagedResult<Charity> results = charityManager.findCharities(null, centerLocation, 
	    			radius, si, ui, closedToo, sortByEnum, sortDirEnum, adminView, maxResults, offset);
			rsp = Response.ok(adminView ? pageMapper.mapCharitiesWithRoleAndBalance(results) : pageMapper.mapCharities(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}

		return rsp;
	}

	@Override
	public Response updateCharity(String charityId, eu.netmobiel.banker.api.model.Charity charity) {
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			charityManager.updateCharity(cid, charityMapper.map(charity));
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
	}

	@Override
	public Response closeCharity(String charityId) {
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			charityManager.stopCampaigning(cid);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
	}

	
	/*======================================   CHARITY FINANCIAL   ==========================================*/
	
	
	@Override
	public Response listCharityStatements(String charityId, OffsetDateTime since, OffsetDateTime until, Integer maxResults, Integer offset) {
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			Charity charity = charityManager.getCharity(cid);
			if (charity.getAccount() == null) {
				throw new EJBAccessException("You do not have access to the statements of this charity: " + charityId);
			}
			PagedResult<AccountingEntry> result = ledgerService.listAccountingEntries(charity.getAccount().getNcan(), si, ui, maxResults, offset); 
			rsp = Response.ok(accountingEntryMapper.map(result)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}


	
	/*======================================   DONATIONS   ==========================================*/
	
	@Override
	public Response postDonation(String charityId, eu.netmobiel.banker.api.model.Donation donation) {
		Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			BankerUser user = userManager.registerCallingUser();
			Donation domainDonation = donationMapper.map(donation);
			Long donationId = charityManager.donate(user, cid, domainDonation);
			// This is the correct method to create a location header.
			// The fromPath(resource, method) uses only the path of the method, it omits the resource.
			rsp = Response.created(UriBuilder.fromResource(CharitiesApi.class)
					.path(CharitiesApi.class.getMethod("getDonation", String.class, String.class)).build(cid, donationId)).build();
		} catch (BusinessException | NoSuchMethodException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response getDonation(String charityId, String donationId) {
		Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	Long did = UrnHelper.getId(Donation.URN_PREFIX, donationId);
			Donation donation = charityManager.getDonation(cid, did);
			rsp = Response.ok(donationMapper.mapPlain(donation)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response listDonationsForCharity(String charityId, String userId, OffsetDateTime since, OffsetDateTime until,
			String sortBy, String sortDir, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			BankerUser user = userId == null ? null : resolveUserReference(userId, false);
			DonationFilter filter = new DonationFilter(charityId, user == null ? null : user.getId(), since, until, sortBy, sortDir, false);
			filter.setAnonymousToo(true);
			filter.setSortBy(sortBy, DonationSortBy.DATE, new DonationSortBy[] { DonationSortBy.DATE, DonationSortBy.AMOUNT });
			Cursor cursor = new Cursor(maxResults, offset);
			// Include user data in result
	    	PagedResult<Donation> results = charityManager.listDonations(filter, cursor, true);
			rsp = Response.ok(pageMapper.mapDonationWithUser(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}

		return rsp;
	}

	@Override
	public Response reportPopularity(String charity, String location, Integer radius, Boolean omitInactive,
			OffsetDateTime since, OffsetDateTime until, String sortBy, String sortDir, Integer maxResults,
			Integer offset) {
		Response rsp = null;
		try {
			DonationFilter filter;
			if (charity != null) {
				filter = new DonationFilter(charity, null, since, until, sortBy, sortDir, false);
			} else {
				filter = new DonationFilter(location, radius, Boolean.TRUE.equals(omitInactive), null, since, until, sortBy, sortDir, false);
			}
			filter.setSortBy(sortBy, DonationSortBy.DONORS, new DonationSortBy[] { DonationSortBy.DONORS });
			Cursor cursor = new Cursor(maxResults, offset);
	    	PagedResult<Charity> results = charityManager.reportCharityPopularityTopN(filter, cursor);
			rsp = Response.ok(pageMapper.mapCharities(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}

		return rsp;
	}

	@Override
	public Response createCharityWithdrawal(String charityId, eu.netmobiel.banker.api.model.WithdrawalRequest withdrawal) {
		Response rsp = null;
		try {
			// Calling user is doing the withdrawal for the charity
			BankerUser user = resolveUserReference("me", true);
	    	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			Charity charity = charityManager.getCharity(cid);
			// Is this user allowed to do the withdrawal?
			boolean canWithdraw = charity.getRoles() != null && charity.getRoles().stream()
				.filter(r -> r.getUser().equals(user) && r.getRole() == CharityUserRoleType.MANAGER)
				.findFirst()
				.isPresent();
			if (! canWithdraw) {
				throw new ForbiddenException(String.format("User %d is not a manager of charity %d and not an admin", user.getId(), cid));
			}
			Long id = ledgerService.createWithdrawalRequest(user, charity.getAccount(), withdrawal.getAmountCredits(), withdrawal.getDescription());
			String wrid = UrnHelper.createUrn(WithdrawalRequest.URN_PREFIX, id);
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(wrid)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getCharityAccount(String charityId) {
		Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	Account acc = charityManager.getCharityAccount(cid);
			rsp = Response.ok(accountMapper.map(acc)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response updateCharityAccount(String charityId, eu.netmobiel.banker.api.model.Account acc) {
		Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	Account accdom = accountMapper.map(acc);
        	charityManager.updateCharityAccount(cid, accdom);
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

}
