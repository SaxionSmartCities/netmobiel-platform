package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;

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
		account1 = Account.newInstant("PAL-1", "Account 1", AccountType.LIABILITY);
		em.persist(account1);
    }
    
//    private void dump(String subject, Collection<Charity> charities) {
//    	charities.forEach(m -> log.info(subject + ": " + m.toString()));
//    }
    
    @Test
    public void saveCharity() throws Exception {
    	Charity ch = new Charity();
    	String description = "My charity description";
    	int donatedAmount = 120;
    	int goalAmount = 250;
    	String pictureUrl = "https://www.netmobiel.eu/123456.img";
    	GeoLocation location = Fixture.placeZieuwentRKKerk; 
    	ch.setAccount(account1);
    	ch.setDescription(description);
    	ch.setDonatedAmount(donatedAmount);
    	ch.setGoalAmount(goalAmount);
    	ch.setLocation(location);
    	ch.setPictureUrl(pictureUrl);
    	charityDao.save(ch);
    	flush();
    
    	ch = em.find(Charity.class, ch.getId());
    	assertEquals(description, ch.getDescription());
    	assertEquals(donatedAmount, ch.getDonatedAmount());
    	assertEquals(goalAmount, ch.getGoalAmount());
    	assertEquals(location, ch.getLocation());
    	assertEquals(account1, ch.getAccount());
    	assertEquals(pictureUrl, ch.getPictureUrl());
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
		flush();
		fail("Expected constraint violation");
    }

    @Test(expected = RollbackException.class)
    public void saveCharity_GoalPositive() throws Exception {
		Charity ch1 = Fixture.createCharity(account1, null, 0, -1, null, null);
		charityDao.save(ch1);
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
    	em.persist(charity2);
    	Charity charity3 = Fixture.createCharity(account3, "Description 3", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	em.persist(charity3);
    	Charity charity4 = Fixture.createCharity(account4, "Description 4", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	em.persist(charity4);

    	// Search any
    	PagedResult<Long> actual = charityDao.findCharities(null, null, null, null, null, null, null, 0, 0);
    	assertNotNull(actual);
    	assertEquals(3, actual.getTotalCount().intValue());

    	// Search location
    	actual = charityDao.findCharities(Fixture.placeZieuwent, 1000, null, null, null, null, null, 10, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	actual = charityDao.findCharities(Fixture.placeZieuwent, 50000, null, null, null, null, null, 10, 0);
    	assertEquals(3, actual.getCount());

    	// Since 
    	actual = charityDao.findCharities(null, null, Instant.parse("2020-09-10T00:00:00Z"), null, null, null, null, 10, 0);
    	assertNotNull(actual);
    	assertEquals(2, actual.getCount());
    	assertTrue(actual.getData().contains(charity1.getId()));
    	assertTrue(actual.getData().contains(charity3.getId()));

    	// Until 
    	actual = charityDao.findCharities(null, null, null, Instant.parse("2020-09-10T00:00:00Z"), null, null, null, 10, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
    	assertTrue(actual.getData().contains(charity2.getId()));

    	// Closed too - false
    	actual = charityDao.findCharities(null, null, null, null, false, null, null, 10, 0);
    	assertNotNull(actual);
    	assertEquals(3, actual.getCount());
    	
    	// Closed too - true
    	actual = charityDao.findCharities(null, null, null, null, true, null, null, 10, 0);
    	assertNotNull(actual);
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
    	em.persist(charity2);
    	Charity charity3 = Fixture.createCharity(account3, "Description 3", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	em.persist(charity3);
    	Charity charity4 = Fixture.createCharity(account4, "Description 4", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	em.persist(charity4);

    	PagedResult<Long> actual = charityDao.findCharities(null, null, null, null, null, null, null, 10, 0);
    	assertNotNull(actual);
    	assertEquals(3, actual.getCount());
    	// Default order is by name ascending
    	assertEquals(charity1.getId(), actual.getData().get(0));
    	assertEquals(charity2.getId(), actual.getData().get(1));
    	assertEquals(charity3.getId(), actual.getData().get(2));

    	actual = charityDao.findCharities(null, null, null, null, null, null, SortDirection.DESC, 10, 0);
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	assertEquals(charity2.getId(), actual.getData().get(1));
    	assertEquals(charity1.getId(), actual.getData().get(2));

    	// Account 1 is created with creation time equals now()
    	actual = charityDao.findCharities(null, null, null, null, null, CharitySortBy.DATE, SortDirection.ASC, 10, 0);
    	assertEquals(charity2.getId(), actual.getData().get(0));
    	assertEquals(charity3.getId(), actual.getData().get(1));
    	assertEquals(charity1.getId(), actual.getData().get(2));

    	actual = charityDao.findCharities(Fixture.placeZieuwent, 50000, null, null, null, CharitySortBy.DISTANCE, SortDirection.ASC, 10, 0);
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	assertEquals(charity1.getId(), actual.getData().get(1));
    	assertEquals(charity2.getId(), actual.getData().get(2));
    }
    
    @Test(expected = BadRequestException.class)
    public void listCharities_OrderDistanceNoLocation() throws Exception {
    	charityDao.findCharities(null, null, null, null, null, CharitySortBy.DISTANCE, null, 10, 0);
		fail("Expected bad request");
    }

    
}
