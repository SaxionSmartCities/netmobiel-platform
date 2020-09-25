package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collection;

import javax.inject.Inject;
import javax.transaction.RollbackException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharitySortBy;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.banker.test.Fixture;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;

@RunWith(Arquillian.class)
public class CharityDaoIT  extends BankerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
	        ;
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private CharityDao charityDao;

    private BankerUser driver1;
    private Account account1;
    
    public boolean isSecurityRequired() {
    	return true;
    }
	

    @Override
    protected void insertData() throws Exception {
        driver1 = Fixture.createUser(loginContextDriver);
		em.persist(driver1);
		account1 = Account.newInstant("PAL-1", "Account 1", AccountType.LIABILITY, Instant.parse("2020-07-01T00:00:00Z"));
		em.persist(account1);
    }
    
    private void dump(String subject, Collection<Charity> charities) {
    	charities.forEach(c -> log.info(String.format("%s: %s %s %s %s", subject, c.getId(), c.getAccount().getName(), c.getCampaignStartTime(), c.getCampaignEndTime())));
    }
    
    @Test
    public void saveCharity() throws Exception {
    	Charity ch = new Charity();
    	String description = "My charity description";
    	Integer donatedAmount = 120;
    	Integer goalAmount = 250;
    	String imageUrl = "https://www.netmobiel.eu/123456.img";
    	GeoLocation location = Fixture.placeZieuwentRKKerk;
    	Instant campaignStart = account1.getCreatedTime().plusSeconds(7200);
    	ch.setAccount(account1);
    	ch.setCampaignStartTime(campaignStart);
    	ch.setImageUrl(imageUrl);
    	ch.setDescription(description);
    	ch.setDonatedAmount(donatedAmount);
    	ch.setGoalAmount(goalAmount);
    	ch.setLocation(location);
    	ch.setImageUrl(imageUrl);
    	charityDao.save(ch);
    	flush();
    
    	ch = em.find(Charity.class, ch.getId());
    	assertEquals(campaignStart, ch.getCampaignStartTime());
    	assertEquals(description, ch.getDescription());
    	assertEquals(donatedAmount, ch.getDonatedAmount());
    	assertEquals(goalAmount, ch.getGoalAmount());
    	assertEquals(location, ch.getLocation());
    	assertEquals(account1, ch.getAccount());
    	assertEquals(imageUrl, ch.getImageUrl());
    	log.info("Charity: " + ch);
    }

    @Test(expected = RollbackException.class)
    public void saveCharity_DuplicateAccount() throws Exception {
		Charity ch1 = Fixture.createCharity(account1, "Charity 1 description", 100, 500, Fixture.placeRozenkwekerijZutphen, "http://www.demo.nl/picture");
		charityDao.save(ch1);
		// Same account - error
		Charity ch2 = Fixture.createCharity(account1, "Charity 1 description", 100, 500, Fixture.placeRozenkwekerijZutphen, null);
		charityDao.save(ch2);
		expectFailure();
		flush();
    }

    @Test(expected = RollbackException.class)
    public void saveCharity_LocationMandatory() throws Exception {
		Charity ch1 = Fixture.createCharity(account1, null, 100, 500, null, null);
		charityDao.save(ch1);
		expectFailure();
		flush();
		fail("Expected constraint violation");
    }
    
    @Test(expected = RollbackException.class)
    public void saveCharity_DonationsPositive() throws Exception {
		Charity ch1 = Fixture.createCharity(account1, null, -1, 500, null, null);
		charityDao.save(ch1);
		expectFailure();
		flush();
		fail("Expected constraint violation");
    }

    @Test(expected = RollbackException.class)
    public void saveCharity_GoalPositive() throws Exception {
		Charity ch1 = Fixture.createCharity(account1, null, 0, -1, null, null);
		charityDao.save(ch1);
		expectFailure();
		flush();
		fail("Expected constraint violation");
    }

    
    @Test
    public void listCharities() throws BusinessException {
    	Account account2 = Account.newInstant("PLA-2", "Account 2", AccountType.LIABILITY, Instant.parse("2020-09-01T00:00:00Z"));
    	em.persist(account2);
    	Account account3 = Account.newInstant("PLA-3", "Account 3", AccountType.LIABILITY, Instant.parse("2020-09-15T00:00:00Z"));
    	em.persist(account3);
    	Account account4 = Account.newInstant("PLA-4", "Account 4 closed", AccountType.LIABILITY, Instant.parse("2020-07-01T00:00:00Z"));
    	account4.setClosedTime(Instant.parse("2020-07-31T00:00:00Z"));
    	em.persist(account4);
    	Charity charity1 = Fixture.createCharity(account1, "Description 1", 100, 500, Fixture.placeSlingeland, null);
    	
    	em.persist(charity1);
    	Charity charity2 = Fixture.createCharity(account2, "Description 2", 100, 500, Fixture.placeRozenkwekerijZutphen, null);
    	charity2.setCampaignEndTime(Instant.parse("2020-09-30T00:00:00Z"));
    	em.persist(charity2);
    	Charity charity3 = Fixture.createCharity(account3, "Description 3", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	em.persist(charity3);
    	Charity charity4 = Fixture.createCharity(account4, "Description 4 finished", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	charity4.setCampaignEndTime(account4.getClosedTime().minusSeconds(3600));
    	em.persist(charity4);

    	Instant now = Instant.parse("2020-09-25T12:00:00Z");
    	
    	// Search any
    	PagedResult<Long> actual = charityDao.findCharities(now, null, null, null, null, null, null, null, 0, 0);
    	assertNotNull(actual);
    	dump("non closed", charityDao.fetch(actual.getData(), Charity.LIST_ENTITY_GRAPH));
    	assertEquals(3, actual.getTotalCount().intValue());

    	// Search location
    	actual = charityDao.findCharities(now, Fixture.placeZieuwent, 1000, null, null, null, null, null, 10, 0);
    	assertNotNull(actual);
    	dump("nearby Zieuwent", charityDao.fetch(actual.getData(), Charity.LIST_ENTITY_GRAPH));
    	assertEquals(1, actual.getCount());
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	actual = charityDao.findCharities(now, Fixture.placeZieuwent, 50000, null, null, null, null, null, 10, 0);
    	dump("all around Zieuwent", charityDao.fetch(actual.getData(), Charity.LIST_ENTITY_GRAPH));
    	assertEquals(3, actual.getCount());

    	// Since 
    	Instant since = Instant.parse("2020-09-10T00:00:00Z");
    	actual = charityDao.findCharities(now, null, null, since, null, null, null, null, 10, 0);
    	assertNotNull(actual);
    	dump("Start after " + since, charityDao.fetch(actual.getData(), Charity.LIST_ENTITY_GRAPH));
    	assertEquals(1, actual.getCount());
    	assertTrue(actual.getData().contains(charity3.getId()));

    	// Until 
    	Instant until = Instant.parse("2020-09-10T00:00:00Z");
    	actual = charityDao.findCharities(now, null, null, null, until, null, null, null, 10, 0);
    	assertNotNull(actual);
    	dump("Started before " + until, charityDao.fetch(actual.getData(), Charity.LIST_ENTITY_GRAPH));
    	assertEquals(2, actual.getCount());
    	assertTrue(actual.getData().contains(charity1.getId()));
    	assertTrue(actual.getData().contains(charity2.getId()));

    	// Closed too - false
    	actual = charityDao.findCharities(now, null, null, null, null, false, null, null, 10, 0);
    	assertNotNull(actual);
    	dump("Active", charityDao.fetch(actual.getData(), Charity.LIST_ENTITY_GRAPH));
    	assertEquals(3, actual.getCount());
    	
    	// Closed too - true
    	actual = charityDao.findCharities(now, null, null, null, null, true, null, null, 10, 0);
    	assertNotNull(actual);
    	dump("All", charityDao.fetch(actual.getData(), Charity.LIST_ENTITY_GRAPH));
    	assertEquals(4, actual.getCount());

    }

    @Test
    public void listCharities_verifyOrder() throws BusinessException {
    	Account account2 = Account.newInstant("PLA-2", "Account 2", AccountType.LIABILITY, Instant.parse("2020-09-01T00:00:00Z"));
    	em.persist(account2);
    	Account account3 = Account.newInstant("PLA-3", "Account 3", AccountType.LIABILITY, Instant.parse("2020-09-15T00:00:00Z"));
    	em.persist(account3);
    	Account account4 = Account.newInstant("PLA-4", "Account 4 closed", AccountType.LIABILITY, Instant.parse("2020-07-01T00:00:00Z"));
    	account4.setClosedTime(Instant.parse("2020-07-31T00:00:00Z"));
    	em.persist(account4);
    	
    	Charity charity1 = Fixture.createCharity(account1, "Description 1", 100, 500, Fixture.placeSlingeland, null);
    	em.persist(charity1);
    	Charity charity2 = Fixture.createCharity(account2, "Description 2", 100, 500, Fixture.placeRozenkwekerijZutphen, null);
    	charity2.setCampaignEndTime(Instant.parse("2020-09-30T00:00:00Z"));
    	em.persist(charity2);
    	Charity charity3 = Fixture.createCharity(account3, "Description 3", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	em.persist(charity3);
    	Charity charity4 = Fixture.createCharity(account4, "Description 4", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	charity4.setCampaignEndTime(account4.getClosedTime().minusSeconds(3600));
    	em.persist(charity4);

    	Instant now = Instant.parse("2020-09-25T12:00:00Z");

    	PagedResult<Long> actual = charityDao.findCharities(now, null, null, null, null, null, null, null, 10, 0);
    	assertNotNull(actual);
    	assertEquals(3, actual.getCount());
    	// Default order is by name ascending
    	assertEquals(charity1.getId(), actual.getData().get(0));
    	assertEquals(charity2.getId(), actual.getData().get(1));
    	assertEquals(charity3.getId(), actual.getData().get(2));

    	actual = charityDao.findCharities(now, null, null, null, null, null, null, SortDirection.DESC, 10, 0);
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	assertEquals(charity2.getId(), actual.getData().get(1));
    	assertEquals(charity1.getId(), actual.getData().get(2));

    	actual = charityDao.findCharities(now, null, null, null, null, null, CharitySortBy.DATE, SortDirection.ASC, 10, 0);
    	assertEquals(charity1.getId(), actual.getData().get(0));
    	assertEquals(charity2.getId(), actual.getData().get(1));
    	assertEquals(charity3.getId(), actual.getData().get(2));

    	actual = charityDao.findCharities(now, Fixture.placeZieuwent, 50000, null, null, null, CharitySortBy.DISTANCE, SortDirection.ASC, 10, 0);
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	assertEquals(charity1.getId(), actual.getData().get(1));
    	assertEquals(charity2.getId(), actual.getData().get(2));
    }
    
    @Test(expected = BadRequestException.class)
    public void listCharities_OrderDistanceNoLocation() throws Exception {
    	charityDao.findCharities(Instant.now(), null, null, null, null, null, CharitySortBy.DISTANCE, null, 10, 0);
		fail("Expected bad request");
    }

    
}