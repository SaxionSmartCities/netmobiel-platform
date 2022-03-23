package eu.netmobiel.banker.api.resource;

import java.net.URI;
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

import eu.netmobiel.banker.api.CharitiesApi;
import eu.netmobiel.banker.api.mapping.AccountMapper;
import eu.netmobiel.banker.api.mapping.CharityMapper;
import eu.netmobiel.banker.api.mapping.DonationMapper;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.api.model.ImageResponse;
import eu.netmobiel.banker.api.model.ImageUploadRequest;
import eu.netmobiel.banker.filter.CharityFilter;
import eu.netmobiel.banker.filter.DonationFilter;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharityUserRoleType;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.DonationSortBy;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.CharityManager;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.service.WithdrawalService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.ImageHelper;
import eu.netmobiel.commons.util.UrnHelper;

@RequestScoped
public class CharitiesResource implements CharitiesApi {

	@Inject
    private CharityManager charityManager;

	@Inject
    private BankerUserManager userManager;

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
	
	@Inject
    private WithdrawalService withdrawalService;
	
	@Context
	private HttpServletRequest request;

    private BankerUser resolveUserReference(String userId, boolean createIfNeeded) throws NotFoundException, eu.netmobiel.commons.exception.BadRequestException {
		BankerUser user = null;
		if ("me".equals(userId)) {
			user = createIfNeeded ? userManager.findOrRegisterCallingUser() : userManager.findCallingUser();
		} else {
			user = userManager
					.resolveUrn(userId)
					.orElseThrow(() -> new NotFoundException("No such user: " + userId));
		}
		return user;
    }

    private boolean isAdminView() {
    	return request.isUserInRole("admin") || request.isUserInRole("treasurer"); 
    }
    
	@Override
	public Response getCharity(String charityId) {
    	Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	Charity charity = charityManager.getCharity(cid);
			boolean adminView = isAdminView();
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
			BankerUser user = userManager.findOrRegisterCallingUser();
			Charity dch = charityMapper.map(charity);
			final String newCharityId = UrnHelper.createUrn(Charity.URN_PREFIX, charityManager.createCharity(user, dch));
			final String urn = UrnHelper.createUrn(Charity.URN_PREFIX, newCharityId);
			rsp = Response.created(URI.create(urn)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response listCharities(String location, Integer radius, OffsetDateTime since, OffsetDateTime until, 
			Boolean closedToo, Boolean deletedToo,String sortBy, String sortDir, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			CharityFilter filter = new CharityFilter(location, radius, since, until, 
					Boolean.TRUE.equals(closedToo), Boolean.TRUE.equals(deletedToo), sortBy, sortDir);
			Cursor cursor = new Cursor(maxResults, offset);
			boolean adminView = isAdminView();
	    	PagedResult<Charity> results = charityManager.listCharities(null, adminView, filter, cursor);
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
	public Response closeCharity(String charityId, Boolean delete) {
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			charityManager.stopCampaigning(cid, Boolean.TRUE.equals(delete));
			
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
	}

	@Override
	public Response getCharityImage(String charityId) {
    	Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	Charity charity = charityManager.getCharity(cid);
        	ImageResponse ir = new ImageResponse();
        	ir.setImage(charity.getImageUrl());
			rsp = Response.ok(ir).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response uploadImage(String charityId, ImageUploadRequest imageUploadRequest) {
		Response rsp = null;
		try {
			ImageHelper.DecodedImage di = ImageHelper.decodeImage(imageUploadRequest.getImage(), new String[] { "jpg", "png" });
			Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			String imageUrl = charityManager.uploadCharityImage(cid, di.filetype, di.decodedImage);
        	ImageResponse ir = new ImageResponse();
        	ir.setImage(imageUrl);
			rsp = Response.ok(ir).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}
	
	@Override
	public Response removeImage(String charityId) {
		Response rsp = null;
		try {
			Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			charityManager.removeCharityImage(cid);
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
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
			rsp = Response.ok(pageMapper.mapAccountingEntriesShallow(result)).build();
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
        	final Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	final BankerUser user = userManager.findOrRegisterCallingUser();
        	final Donation domainDonation = donationMapper.map(donation);
        	final Long donationId = charityManager.donate(user, cid, domainDonation);
        	final String urn = UrnHelper.createUrn(Donation.URN_PREFIX, donationId);
			rsp = Response.created(URI.create(urn)).build();
		} catch (BusinessException ex) {
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
			Donation donation = charityManager.getDonation(cid, did, null);
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
			filter.setSortDir(sortDir, SortDirection.DESC);
			Cursor cursor = new Cursor(maxResults, offset);
			// Include user data in result
	    	PagedResult<Donation> results = charityManager.listDonations(filter, cursor, true);
			rsp = Response.ok(pageMapper.mapDonationsWithUser(results)).build();
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
			if (filter.getSortDir() == null) {
				filter.setSortDir(SortDirection.DESC);
			}
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
			boolean privileged = isAdminView();
			boolean canWithdraw = charity.getRoles() != null && charity.getRoles().stream()
				.filter(r -> r.getUser().equals(user) && r.getRole() == CharityUserRoleType.MANAGER)
				.findFirst()
				.isPresent();
			if (! canWithdraw && ! privileged) {
				throw new ForbiddenException(String.format("User %d is not a manager of charity %d and not an admin or treasurer", user.getId(), cid));
			}
			Long id = withdrawalService.createWithdrawalRequest(charity.getAccount(), withdrawal.getAmountCredits(), withdrawal.getDescription());
			String wrid = UrnHelper.createUrn(WithdrawalRequest.URN_PREFIX, id);
			rsp = Response.created(URI.create(wrid)).build();
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
			rsp = Response.ok(accountMapper.mapAll(acc)).build();
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
        	// Security check is done at service.
        	charityManager.updateCharityAccount(cid, accdom);
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response listCharityWithdrawalRequests(String charityId, OffsetDateTime since, OffsetDateTime until,
			String status, Integer maxResults, Integer offset) {
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	Account acc = charityManager.getCharityAccount(cid);
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

	@Override
	public Response cancelCharityWithdrawalRequest(String charityId, String withdrawalRequestId, String reason) {
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	Account acc = charityManager.getCharityAccount(cid);
	    	Long wrid = UrnHelper.getId(WithdrawalRequest.URN_PREFIX, withdrawalRequestId);
			withdrawalService.cancelWithdrawalRequest(acc, wrid);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
	}

}
