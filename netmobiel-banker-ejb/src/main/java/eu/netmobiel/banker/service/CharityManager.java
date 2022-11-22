package eu.netmobiel.banker.service;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import eu.netmobiel.banker.filter.CharityFilter;
import eu.netmobiel.banker.filter.DonationFilter;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharityUserRoleType;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.SettlementOrder;
import eu.netmobiel.banker.repository.BalanceDao;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.banker.repository.CharityDao;
import eu.netmobiel.banker.repository.DonationDao;
import eu.netmobiel.banker.repository.DonationDao.CharityPopularity;
import eu.netmobiel.banker.repository.DonationDao.DonorGenerosity;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.RemoveException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.Logging;

/**
 * Charity EJB service for managing the charities and the donations to them by the users.
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

    @Inject
    private BalanceDao balanceDao;

	@Inject @Created
    private Event<Charity> charityCreatedEvent;

	@Inject @Created
    private Event<SettlementOrder> settlementOrderCreatedEvent;

    @Resource
	private SessionContext sessionContext;

	@Resource(lookup = "java:global/imageService/imageFolder")
	private String imageServiceImageFolder;
	
    /**
	 * Lists the charities according the criteria specified by the parameters.
	 * Default sort is name, default order direction is ascending. The date used for the selection is the campaign start.
	 * @param now parameter used to manipulate the current time for this method. If null then the actual system time is taken. 
	 * @param adminView if true then include the users having a role with regard to the charity.
	 * @param filter the filter parameters
	 * @param cursor the cursor  
	 * @return A Page object with Charity objects. 
	 * @throws BadRequestException
	 */
    public PagedResult<Charity> listCharities(Instant now, boolean adminView, CharityFilter filter, Cursor cursor) throws BadRequestException {
    	if (now == null) {
    		now = Instant.now();
    	}
    	filter.validate();
    	cursor.validate(MAX_RESULTS, 0);
        List<Charity> results = Collections.emptyList();
        Long totalCount = 0L;
		PagedResult<Long> prs = charityDao.findCharities(now, filter, Cursor.COUNTING_CURSOR);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && cursor.getMaxResults() > 0) {
    		// Get the actual data
    		PagedResult<Long> charityIds = charityDao.findCharities(now, filter, cursor);
    		if (charityIds.getData().size() > 0) {
    			String graph = adminView ? Charity.ROLES_ENTITY_GRAPH : Charity.SHALLOW_ENTITY_GRAPH;
    			results = charityDao.loadGraphs(charityIds.getData(), graph, Charity::getId);
    		}
    	}
    	return new PagedResult<>(results, cursor, totalCount);
    }

    static private boolean userHasRoleOnCharity(BankerUser user, Charity ch, CharityUserRoleType role) {
		return ch.getRoles().stream()
				.filter(r -> r.getUser().equals(user) && (role == null || role == r.getRole()))
				.findFirst()
				.isPresent();
    }

    private void checkAccessRightsForCreate(BankerUser user) {
		boolean admin = sessionContext.isCallerInRole("admin");
		boolean treasurer = sessionContext.isCallerInRole("treasurer");
		if (!admin && !treasurer) {
			throw new EJBAccessException("You have no privilege to create a charity");
		}
    }

    private void checkAccessRightsForWrite(BankerUser user, Charity charity) {
		boolean admin = sessionContext.isCallerInRole("admin");
		boolean treasurer = sessionContext.isCallerInRole("treasurer");
		if (!admin && !treasurer && !userHasRoleOnCharity(user, charity, CharityUserRoleType.MANAGER)) {
			throw new EJBAccessException("Write access to charity not allowed");
		}
    }
    
    private boolean hasFullAccessRightsForRead(BankerUser user, Charity charity) {
		boolean admin = sessionContext.isCallerInRole("admin");
		boolean treasurer = sessionContext.isCallerInRole("treasurer");
		return admin || treasurer || userHasRoleOnCharity(user, charity, null);
    }

    /**
     * Retrieves a charity, including the roles. Anyone can read a charity, given the id. The amount of details depends on the caller.
     * @param id the charity id
     * @return a charity object
     * @throws NotFoundException No matching charity found.
     */
    public Charity getCharity(Long id) throws NotFoundException {
    	Charity charitydb = charityDao.loadGraph(id, Charity.ACCOUNT_ROLES_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + id));
    	if (charitydb.getAccount() == null) {
    		throw new IllegalStateException("Charity has no account: " + id);
    	}
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	charityDao.detach(charitydb);
		if (!hasFullAccessRightsForRead(me, charitydb)) {
			// Roles and Account are privileged
			charitydb.getRoles().clear();
			charitydb.setAccount(null);
		} else {
			// Add the balance too
			Balance balance = balanceDao.findActualBalance(charitydb.getAccount());
			charitydb.getAccount().setActualBalance(balance);
		}
    	return charitydb;
    }

    public Account getCharityAccount(Long id) throws NotFoundException {
    	Charity ch = getCharity(id);
    	if (ch.getAccount() == null) {
    		throw new EJBAccessException("Read access to charity account not allowed");
    	}
    	return ch.getAccount();
    }

    public void updateCharityAccount(Long charityId, Account acc) throws NotFoundException {
    	Charity charitydb = charityDao.loadGraph(charityId, Charity.ACCOUNT_ROLES_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + charityId));
    	if (charitydb.getAccount() == null) {
    		throw new IllegalStateException("Charity has no account: " + charityId);
    	}
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	checkAccessRightsForWrite(me, charitydb);
    	Account accdb = charitydb.getAccount();
    	// Set only specific attributes
    	accdb.setIban(acc.getIban());
    	accdb.setIbanHolder(acc.getIbanHolder());
    	accdb.setName(acc.getName());
    }

    protected void validateCharityInput(Charity charity, boolean lenient) throws BadRequestException {
    	if ((!lenient || charity.getCampaignEndTime() != null) && charity.getCampaignStartTime() == null) {
    		throw new BadRequestException("Constraint violation: 'campaignStartTime' must be set.");
    	}
    	if (charity.getCampaignEndTime() != null && charity.getCampaignEndTime().isBefore(charity.getCampaignStartTime())) {
    		throw new BadRequestException("Constraint violation: 'campaignEndTime' must be later than 'campaignStartTime'.");
    	}
//    	if (!lenient && (charity.getDescription() == null || charity.getDescription().isBlank())) {
//    		throw new BadRequestException("Constraint violation: 'description' must be non-empty.");
//    	}
    	if (!lenient && charity.getGoalAmount() == null) {
    		throw new BadRequestException("Constraint violation: 'goalAmount' must be greater than 0.");
    	}
    	if (!lenient && charity.getLocation() == null) {
    		throw new BadRequestException("Constraint violation: 'location' must be set.");
    	}
    	if (!lenient && (charity.getName() == null || charity.getName().isBlank())) {
    		throw new BadRequestException("Constraint violation: 'name' must be set.");
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
    	checkAccessRightsForCreate(userdb);
    	charityCreatedEvent.fire(charity);
    	// Only admin and treasurer can create. Need endpoint to add managers.
//		charity.addUserRole(userdb, CharityUserRoleType.MANAGER);
    	charityDao.save(charity);
    	return charity.getId();
    }

    /**
     * Updates some charity attributes. The following attributes can be modified: account name, campaignStartTime,
     * campaignEndTime, description, goalAmount, location, name. If campaignEndTime is set, 
     * then campaignEndTime must also be set.
     * @param id the charity id.
     * @param charity the partial charity with the attributes to update. 
     * @throws NotFoundException when the charity does not exists.
     * @throws BadRequestException when an attribute has an invalid value.
     */
    public void updateCharity(Long id, Charity charity)  throws NotFoundException, BadRequestException {
    	validateCharityInput(charity, true);
    	Charity charitydb = charityDao.loadGraph(id, Charity.ACCOUNT_ROLES_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + id));
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	checkAccessRightsForWrite(me, charitydb);
    	// Only copy those fields that are allowed to be updated.
    	if (charity.getAccount() != null && charity.getAccount().getName() != null) {
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
    	if (charity.getLocation() != null) {
    		charitydb.setLocation(charity.getLocation());
    	}
    	if (charity.getName() != null) {
    		charitydb.setName(charity.getName());
    	}
    }

	public String uploadCharityImage(Long id, String filetype, byte[] image) throws NotFoundException, UpdateException {
    	Charity charitydb = charityDao.loadGraph(id, Charity.SHALLOW_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + id));
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	checkAccessRightsForWrite(me, charitydb);

    	// Note: use a timestamp in the filename to defeat the caching of the browser
		String filename = String.format("ch-%d-%d.%s", id, Instant.now().toEpochMilli(), filetype);
    	String folder = Long.toHexString(id % 256);
    	Path newFile = Path.of(folder, filename); 
    	Path newPath = Paths.get(imageServiceImageFolder).resolve(newFile);
    	Path oldFile = null;
    	if (charitydb.getImageUrl() != null) {
    		String[] parts = charitydb.getImageUrl().split("/");
    		String oldFolder = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
    		String oldFilename = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
    		oldFile = Path.of(oldFolder, oldFilename);
    	}
		try {
			Files.createDirectories(newPath.getParent());
			Files.write(newPath, image, StandardOpenOption.CREATE_NEW);
	    	if (oldFile != null) {
	    		Files.deleteIfExists(Paths.get(imageServiceImageFolder).resolve(oldFile));
	    	}
			charitydb.setImageUrl(String.format("%s/%s", URLEncoder.encode(folder, StandardCharsets.UTF_8), URLEncoder.encode(filename, StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new UpdateException("Error writing or replacing image " + newPath , e);
		}
		return charitydb.getImageUrl();
	}
	
	public void removeCharityImage(Long id) throws NotFoundException, RemoveException {
    	Charity charitydb = charityDao.loadGraph(id, Charity.SHALLOW_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + id));
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	checkAccessRightsForWrite(me, charitydb);

		try {
	    	if (charitydb.getImageUrl() != null) {
	    		String[] parts = charitydb.getImageUrl().split("/");
	    		String oldFolder = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
	    		String oldFilename = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
	    		Path oldFile = Path.of(oldFolder, oldFilename);
	    		Files.deleteIfExists(Paths.get(imageServiceImageFolder).resolve(oldFile));
				charitydb.setImageUrl(null);
	    	}
		} catch (IOException e) {
			throw new RemoveException("Error removing image " + charitydb.getImageUrl(), e);
		}
	}
	
	/**
     * Stops the campaigning of a charity. You must have sufficient privileges.
     * @param id the charity id
     * @param delete If true then soft delete the charity.
     * @throws NotFoundException No matching charity found.
     * @throws BadRequestException 
     */
    public void stopCampaigning(Long id, boolean delete) throws NotFoundException, BadRequestException {
    	Charity charitydb = charityDao.loadGraph(id, Charity.SHALLOW_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + id));
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	checkAccessRightsForWrite(me, charitydb);
    	Instant now = Instant.now();
    	if (charitydb.getCampaignEndTime() == null || now.isBefore(charitydb.getCampaignEndTime())) {
    		// Set the date if not set yet, move to now if it was set to some future date.
        	charitydb.setCampaignEndTime(Instant.now());
    	}
    	charitydb.setDeleted(delete);
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
    	Charity charitydb = charityDao.loadGraph(charityId, Charity.SHALLOW_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + charityId));
    	if (donation.getAmount() <= 0) {
    		throw new BadRequestException("Not a valid amount: " + donation.getAmount());
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
    public Donation getDonation(Long charityId, Long donationId, String graph) throws NotFoundException {
    	Donation donation = donationDao.loadGraph(donationId, graph)
    			.orElseThrow(() -> new NotFoundException("No such donation: " + donationId));
    	return donation;
    }

    protected void completeTheFilter(DonationFilter filter) throws BadRequestException, NotFoundException {
    	filter.validate();
    	if (filter.getCharityId() != null) {
    		filter.setCharity(charityDao.find(filter.getCharityId())
    			.orElseThrow(() -> new NotFoundException("No such charity: " + filter.getCharityId())));
    	}
    	if (filter.getUserId() != null) {
    		filter.setUser(userDao.find(filter.getUserId())
        			.orElseThrow(() -> new NotFoundException("No such user: " + filter.getUserId())));
    	}
    }
	/**
     * Lists donations according specific criteria. 
     * @param filter The donation selection and sorting criteria.
     * @param cursor The position and size of the result set. 
     * @return A list of donations matching the criteria.
	 * @throws BadRequestException
	 */
    public PagedResult<Donation> listDonations(BankerUser effectiveUser, DonationFilter filter, Cursor cursor, boolean includeUserData) throws NotFoundException, BadRequestException {
    	completeTheFilter(filter);
    	cursor.validate(MAX_RESULTS, 0);
        
        List<Donation> results = Collections.emptyList();
		PagedResult<Long> prs = donationDao.listDonations(filter, cursor);
		Long totalCount = prs.getTotalCount();
    	if (totalCount > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		String graph = includeUserData ? Donation.USER_GRAPH : null;
			results = donationDao.loadGraphs(prs.getData(), graph, Donation::getId);
    	}
    	// If there is user data, then check for anonymous donations.
    	// An anonymous donation is listed, but without the user, unless the admin is asking for it.
    	if (includeUserData) {
    		boolean admin = sessionContext.isCallerInRole("admin") ;
    		if (! admin) {
    			// Detach all the donations, otherwise post-filter changes will be persisted in the database!
    	    	donationDao.clear();
    			results.stream()
    				.filter(d -> d.isAnonymous() && !d.getUser().equals(effectiveUser))
    				.forEach(d -> {
    					// Make sure the donor remains anonymous 
    					d.setUser(null);	
    				});
    		}
    	}
    	return new PagedResult<>(results, cursor, totalCount);
    }
    
    /**
     * Retrieves the popular charities according some filter: Count distinct users per charity ordered by 
     * descending donor count and by charity id descending (younger charities are prioritized). The donation
     * object is used to store the results.
     * @param filter The donation selection and sorting criteria.
     * @param cursor The position and size of the result set. 
     * @return A list of donation objects matching the criteria with only the charity and the count set. Charity is a proxy!
     * @throws NotFoundException 
     * @throws BadRequestException 
     */
    public PagedResult<Charity> reportCharityPopularityTopN(DonationFilter filter, Cursor cursor) throws BadRequestException, NotFoundException {
    	completeTheFilter(filter);
    	cursor.validate(MAX_RESULTS, 0);
    	PagedResult<CharityPopularity> prs = donationDao.reportCharityPopularityTopN(filter, cursor);
    	List<Long> charityIds = prs.getData().stream()
    			.map(d -> d.charityId)
    			.collect(Collectors.toList());
		List<Charity> charities = charityDao.loadGraphs(charityIds, Charity.SHALLOW_ENTITY_GRAPH, Charity::getId);
    	for (int ix = 0; ix < charities.size(); ix++) {
    		charities.get(ix).setDonorCount(Math.toIntExact(prs.getData().get(ix).donorCount));
		}
        return new PagedResult<>(charities, cursor, prs.getTotalCount());
    }
    
    /**
     * Donated before: List of latest donations by a donor to each charity ordered by donation date descending
     * @param user The user to select the latest (distinct) donations for. 
     * @param cursor The position and size of the result set.
     * @return A list of donation ids.
     * @throws BadRequestException 
     * @throws NotFoundException 
     */
    public PagedResult<Donation> reportMostRecentDistinctDonations(BankerUser user, Cursor cursor) throws BadRequestException, NotFoundException {
    	cursor.validate(MAX_RESULTS, 0);
		BankerUser userdb = userDao.find(user.getId())
    			.orElseThrow(() -> new NotFoundException("No such user: " + user.getId()));
        List<Donation> results = Collections.emptyList();
		PagedResult<Long> prs = donationDao.reportMostRecentDistinctDonations(userdb, cursor);
		Long totalCount = prs.getTotalCount();
    	if (totalCount > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
			results = donationDao.loadGraphs(prs.getData(), Donation.CHARITY_GRAPH, Donation::getId);
    	}
    	return new PagedResult<>(results, cursor, totalCount);
    }

    /**
     * Retrieves the total amount donated in to any charity ordered by total amount donated descending 
     * and by user id descending (late adopters are prioritized). If the totals per charity are needed, then specify
     * a charity as filter criterium.
     * @param filter The donation selection and sorting criteria.
     * @param cursor The position and size of the result set. 
     * @return A list of BankerUser objects with the total amount of donated credits.
     */
    public PagedResult<BankerUser> reportDonorGenerousityTopN(DonationFilter filter, Cursor cursor) throws BadRequestException, NotFoundException {
    	completeTheFilter(filter);
    	cursor.validate(MAX_RESULTS, 0);
    	PagedResult<DonorGenerosity> prs = donationDao.reportDonorGenerosityTopN(filter, cursor);
    	List<Long> userIds = prs.getData().stream()
    			.map(d -> d.donorId)
    			.collect(Collectors.toList());
		List<BankerUser> users = userDao.loadGraphs(userIds, BankerUser.GRAPH_WITHOUT_ACCOUNT, BankerUser::getId);
    	for (int ix = 0; ix < users.size(); ix++) {
    		users.get(ix).setDonatedCredits(Math.toIntExact(prs.getData().get(ix).amount));
		}
        return new PagedResult<>(users, cursor, prs.getTotalCount());
    }

    	
}
