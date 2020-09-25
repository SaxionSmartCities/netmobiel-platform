package eu.netmobiel.banker.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import eu.netmobiel.banker.api.CharitiesApi;
import eu.netmobiel.banker.api.mapping.AccountingEntryMapper;
import eu.netmobiel.banker.api.mapping.CharityMapper;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharitySortBy;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.CharityManager;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.exception.BusinessException;
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
	private CharityMapper charityMapper;

	@Inject
	private PageMapper pageMapper;

	@Inject
    private LedgerService ledgerService;
	
	@Context
	private HttpServletRequest request;
	
	@Override
	public Response getCharity(String charityId) {
		Charity charity = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			charity = charityManager.getCharity(cid);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.ok(charityMapper.map(charity)).build();
	}

	@Override
	public Response listCharityStatements(String charityId, OffsetDateTime since, OffsetDateTime until, Integer maxResults, Integer offset) {
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			Charity charity = charityManager.getCharity(cid);
			PagedResult<AccountingEntry> result = ledgerService.listAccountingEntries(charity.getAccount().getNcan(), si, ui, maxResults, offset); 
			rsp = Response.ok(accountingEntryMapper.map(result)).build();
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
			boolean adminView = request.isUserInRole("admin");
	    	PagedResult<Charity> results = charityManager.findCharities(null, GeoLocation.fromString(location), 
	    			radius, si, ui, closedToo, sortByEnum, sortDirEnum, adminView, maxResults, offset);
			rsp = Response.ok(pageMapper.mapCharities(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}

		return rsp;
	}

	@Override
	public Response postDonation(String charityId, eu.netmobiel.banker.api.model.Donation donation) {
		Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
			BankerUser user = userManager.registerCallingUser();
			Donation domainDonation = charityMapper.map(donation);
			Long donationId = charityManager.donate(user, cid, domainDonation);
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(donationId)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
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

	@Override
	public Response getDonation(String charityId, String donationId) {
		Response rsp = null;
		try {
        	Long cid = UrnHelper.getId(Charity.URN_PREFIX, charityId);
        	Long did = UrnHelper.getId(Donation.URN_PREFIX, donationId);
			Donation donation = charityManager.getDonation(cid, did);
			Response.ok(charityMapper.map(donation)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

}
