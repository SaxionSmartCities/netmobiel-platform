package eu.netmobiel.banker.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import eu.netmobiel.banker.filter.DonationFilter;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharitySortBy;
import eu.netmobiel.banker.model.CharityUserRoleType;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.DonationGroupBy;
import eu.netmobiel.banker.model.DonationSortBy;
import eu.netmobiel.banker.model.SettlementOrder;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.banker.repository.CharityDao;
import eu.netmobiel.banker.repository.DonationDao;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.Logging;

/**
 * Charity EJB service.
 * 
 * @author Jaap Reitsma
 *
 */
@Stateless
@Logging
public class CharityManager {
	public static final Integer MAX_RESULTS = 10; 

	@Inject
	private CharityDao charityDao;
	
	@Inject
	private DonationDao donationDao;
	
	@Inject
	private BankerUserDao userDao;

	@Inject @Created
    private Event<Charity> charityCreatedEvent;

	@Inject @Created
    private Event<SettlementOrder> settlementOrderCreatedEvent;

	/**
	 * Lists the charities according the criteria specified by the parameters.
	 * @param now parameter used to manipulate the current time for this method. If null then the actual system time is taken. 
	 * @param location The center point of the search area
	 * @param radius The radius (in meter) of the circular search area.
	 * @param since only lists charities that start campaigning after this date..  
	 * @param until limit the search to charities having started campaigning before this date.
	 * @param inactiveToo include also charities that are not active in the selected period (i.e., outside campaigning period). 
	 * @param sortBy Sort by name, (campaign start) date or distance. 
	 * @param sortDir Sort ascending or descending.
	 * @param adminView if true then include the users having a role with regard to the charity.
	 * @param maxResults The maximum number of results, Default is 10.
	 * @param offset The zero-based offset in the search result. 
	 * @return A Page object with Charity objects. 
	 * @throws BadRequestException
	 */
    public PagedResult<Charity> findCharities(Instant now, GeoLocation location, Integer radius, Instant since, Instant until, Boolean inactiveToo, 
    		CharitySortBy sortBy, SortDirection sortDir, boolean adminView, Integer maxResults, Integer offset) throws BadRequestException {
    	if (now == null) {
    		now = Instant.now();
    	}
    	if (until != null && since != null && !until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: 'until' must be later than 'since'.");
    	}
    	if (maxResults != null && maxResults > 100) {
    		throw new BadRequestException("Constraint violation: 'maxResults' <= 100.");
    	}
    	if (maxResults != null && maxResults <= 0) {
    		throw new BadRequestException("Constraint violation: 'maxResults' > 0.");
    	}
    	if (offset != null && offset < 0) {
    		throw new BadRequestException("Constraint violation: 'offset' >= 0.");
    	}
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
        List<Charity> results = Collections.emptyList();
        Long totalCount = 0L;
		PagedResult<Long> prs = charityDao.findCharities(now, location, radius, since, until, inactiveToo, sortBy, sortDir, 0, 0);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> charityIds = charityDao.findCharities(now, location, radius, since, until, inactiveToo, sortBy, sortDir, maxResults, offset);
    		if (charityIds.getData().size() > 0) {
    			String graph = adminView ? Charity.LIST_ROLES_ENTITY_GRAPH : Charity.LIST_ENTITY_GRAPH;
    			results = charityDao.fetch(charityIds.getData(), graph, Charity::getId);
    		}
    	}
    	return new PagedResult<Charity>(results, maxResults, offset, totalCount);
    }

    /**
     * Retrieves a charity, including the roles. Anyone can read a charity, given the id. The amount of details depends on the caller.
     * @param id the charity id
     * @return a charity object
     * @throws NotFoundException No matching charity found.
     */
    public Charity getCharity(Long id) throws NotFoundException {
    	Charity charitydb = charityDao.loadGraph(id, Charity.LIST_ROLES_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + id));
    	return charitydb;
    }

    protected void validateCharityInput(Charity charity, boolean lenient) throws BadRequestException {
    	if ((!lenient || charity.getCampaignEndTime() != null) && charity.getCampaignStartTime() == null) {
    		throw new BadRequestException("Constraint violation: 'campaignStartTime' must be set.");
    	}
    	if (charity.getCampaignEndTime() != null && charity.getCampaignEndTime().isBefore(charity.getCampaignStartTime())) {
    		throw new BadRequestException("Constraint violation: 'campaignEndTime' must be later than 'campaignStartTime'.");
    	}
    	if (!lenient && (charity.getDescription() == null || charity.getDescription().trim().isEmpty())) {
    		throw new BadRequestException("Constraint violation: 'description' must be non-empty.");
    	}
    	if (!lenient && charity.getGoalAmount() == null) {
    		throw new BadRequestException("Constraint violation: 'goalAmount' must be greater than 0.");
    	}
    	if (!lenient && charity.getLocation() == null) {
    		throw new BadRequestException("Constraint violation: 'location' must be set.");
    	}
    }
    
    /**
     * Creates a charity and assigns the manager role to the specified user.
     * @param user The user to assign the manager role to.
     * @param charity the charity to create.
     * @return the id of the new charity object.
     * @throws BadRequestException 
     * @throws NotFoundException 
     */
    public Long createCharity(BankerUser user, Charity charity) throws BadRequestException, NotFoundException {
    	validateCharityInput(charity, false);
    	// Synchronous event to create an account
    	BankerUser userdb = userDao.find(user.getId())
    			.orElseThrow(() -> new NotFoundException("No such user: " + user.getId()));
    	charityCreatedEvent.fire(charity);
    	charity.addUserRole(userdb, CharityUserRoleType.MANAGER);
    	charityDao.save(charity);
    	return charity.getId();
    }

    /**
     * Updates some charity attributes. The following attributes can be modified: campaignStartTime,
     * campaignEndTime, description, goalAmount, location, imageUrl, name. If campaignEndTime is set, 
     * then campaignEndTime must also be set.
     * @param id the charity id.
     * @param charity the partial charity with the attributes to update. 
     * @throws NotFoundException when the charity does not exists.
     * @throws BadRequestException when an attribute has an invalid value.
     */
    public void updateCharity(Long id, Charity charity)  throws NotFoundException, BadRequestException {
    	validateCharityInput(charity, true);
    	Charity charitydb = charityDao.loadGraph(id, Charity.LIST_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + id));
    	// Only copy those fields that are allowed to be updated.
    	if (charity.getAccount().getName() != null) {
    		charitydb.getAccount().setName(charity.getAccount().getName());
    	}
    	if (charity.getCampaignStartTime() != null) {
    		charitydb.setCampaignStartTime(charity.getCampaignStartTime());
    	}
		charitydb.setCampaignEndTime(charity.getCampaignEndTime());
    	if (charity.getDescription() != null) {
    		charitydb.setDescription(charity.getDescription());
    	}
    	if (charity.getGoalAmount() != null) {
    		charitydb.setGoalAmount(charity.getGoalAmount());
    	}
    	if (charity.getImageUrl() != null) {
    		charitydb.setImageUrl(charity.getImageUrl());
    	}
    	if (charity.getLocation() != null) {
    		charitydb.setLocation(charity.getLocation());
    	}
    }

    /**
     * Retrieves a charity. Anyone can read a charity, given the id. The amount of details depends on the caller.
     * @param id the charity id
     * @return a charity object
     * @throws NotFoundException No matching charity found.
     */
    public void stopCampaigning(Long id) throws NotFoundException {
    	Charity charitydb = charityDao.loadGraph(id, Charity.LIST_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + id));
    	charitydb.setCampaignEndTime(Instant.now());
    }

    /**
     * Donate an amount of credits to a charity.
     * @param user the donor.
     * @param charityId The id of the charity to receive the credits.
     * @param donation the donation object
     * @return the id of the donation
     * @throws BusinessException in case of trouble, especially when the balance of the donor is insufficient for the amount to donate.
     */
    public Long donate(BankerUser user, Long charityId, Donation donation) throws BusinessException {
    	Charity charitydb = charityDao.loadGraph(charityId, Charity.LIST_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + charityId));
    	if (donation.getAmount() <= 0) {
    		throw new BadRequestException("Not a valid amount: " + donation.getAmount());
    	}
    	if (donation.getDescription() == null || donation.getDescription().trim().isEmpty()) {
    		throw new BadRequestException("Description is mandatory");
    	}
    	donation.setDonationTime(Instant.now());
    	donation.setUser(user);
    	donation.setCharity(charitydb);
    	donationDao.save(donation);
    	// Track the cumulative amount of donations. Could be done in the database too at the cost of a lot of effort when listing charities.
    	charitydb.addDonation(donation.getAmount());
    	// Process the donation settlement order in process
    	EventFireWrapper.fire(settlementOrderCreatedEvent, new SettlementOrder(donation));
    	return donation.getId();
    }

    /**
     * Retrieves a donation. Anyone can read a donation, given the id. 
     * @param charityId the charity id
     * @param donationId the charity id
     * @return a charity object
     * @throws NotFoundException No matching charity found.
     */
    public Donation getDonation(Long charityId, Long donationId) throws NotFoundException {
    	Donation donation = donationDao.find(donationId)
    			.orElseThrow(() -> new NotFoundException("No such donation: " + donationId));
    	return donation;
    }

	/**
	 * Lists the donations for a specific charity according some criteria.
	 * @param now parameter used to manipulate the current time for this method. If null then the actual system time is taken. 
	 * @param userId only lists donations for this user.  
	 * @param since only lists donations after or equal to this date.  
	 * @param until limit the list to donations before this date.
	 * @param groupBy Group the results by a a particular method.  
	 * @param sortBy Sort by the specified method. 
	 * @param sortDir Sort ascending or descending.
	 * @param maxResults The maximum number of results, Default is 10.
	 * @param offset The zero-based offset in the search result. 
	 * @return A Page object with Charity objects. 
	 * @throws BadRequestException
	 */
    public PagedResult<Donation> listDonations(DonationFilter filter, Cursor cursor) throws NotFoundException, BadRequestException {
    	filter.validate();
    	cursor.validate(MAX_RESULTS, 0);
    	if (filter.getCharityId() != null) {
    		filter.setCharity(charityDao.find(filter.getCharityId())
    			.orElseThrow(() -> new NotFoundException("No such charity: " + filter.getCharityId())));
    	}
    	if (filter.getUserId() != null) {
    		filter.setUser(userDao.find(filter.getUserId())
        			.orElseThrow(() -> new NotFoundException("No such user: " + filter.getUserId())));
    	}
        
        List<Donation> results = Collections.emptyList();
        Long totalCount = 0L;
//		PagedResult<Long> prs = charityDao.listDonations(now, charitydb, userdb, since, until, groupBy, sortBy, sortDir, 0, 0);
//		totalCount = prs.getTotalCount();
//    	if (totalCount > 0 && maxResults > 0) {
//    		// Get the actual data
//    		PagedResult<Long> donationIds = charityDao.listDonations(now, charitydb, userdb, since, until, groupBy, sortBy, sortDir, maxResults, offset);
//    		if (donationIds.getData().size() > 0) {
//    			results = charityDao.fetch(donationIds.getData(), graph, Charity::getId);
//    		}
//    	}
//    	return new PagedResult<Charity>(results, maxResults, offset, totalCount);
    	return null;
    }
}
