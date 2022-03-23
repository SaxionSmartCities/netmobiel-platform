package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collection;

import javax.inject.Inject;
import javax.persistence.PersistenceUnitUtil;
import javax.transaction.RollbackException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.banker.filter.CharityFilter;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharitySortBy;
import eu.netmobiel.banker.model.Charity_;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.banker.test.Fixture;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;

@RunWith(Arquillian.class)
public class CharityDaoIT  extends BankerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
                .addClass(CharityDao.class)
	        ;
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private Logger log;
    
    @Inject
    private CharityDao charityDao;

    private Charity charity1;
    private Charity charity2;
    private Charity charity3;
    private Charity charity4;

    @Override
    protected void insertData() throws Exception {
    	prepareBasicLedger();
    }
    
    private void dump(String subject, Collection<Charity> charities) {
    	charities.forEach(c -> log.info(String.format("%s: %s %s %s %s %d%%", subject, c.getId(), 
    			c.getAccount().getName(), c.getCampaignStartTime(), c.getCampaignEndTime(), (c.getDonatedAmount() * 100) / c.getGoalAmount())));
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
    	ch.setName("My charity");
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
    	assertNotNull(ch);
    	assertTrue(em.contains(ch));
    	assertEquals(campaignStart, ch.getCampaignStartTime());
    	assertEquals(description, ch.getDescription());
    	assertEquals(donatedAmount, ch.getDonatedAmount());
    	assertEquals(goalAmount, ch.getGoalAmount());
    	assertEquals(location, ch.getLocation());
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertFalse(puu.isLoaded(ch, Charity_.ACCOUNT));
    	ch.getAccount().getNcan();
    	assertTrue(puu.isLoaded(ch, Charity_.ACCOUNT));
    	assertEquals(account1.getId(), ch.getAccount().getId());
    	assertEquals(imageUrl, ch.getImageUrl());
    	log.info("Charity: " + ch);
    }

    @Test(expected = RollbackException.class)
    public void saveCharity_DuplicateAccount() throws Exception {
		Charity ch1 = Fixture.createCharity(account1, "Charity 1", "Charity 1 description", 100, 500, Fixture.placeRozenkwekerijZutphen, "http://www.demo.nl/picture");
		charityDao.save(ch1);
		// Same account - error
		Charity ch2 = Fixture.createCharity(account1, "Charity 2", "Charity 2 description", 100, 500, Fixture.placeRozenkwekerijZutphen, null);
		charityDao.save(ch2);
		expectFailure();
		flush();
    }

    @Test(expected = RollbackException.class)
    public void saveCharity_LocationMandatory() throws Exception {
		Charity ch1 = Fixture.createCharity(account1, "Charity 1", null, 100, 500, null, null);
		charityDao.save(ch1);
		expectFailure();
		flush();
		fail("Expected constraint violation");
    }
    
    @Test(expected = RollbackException.class)
    public void saveCharity_DonationsPositive() throws Exception {
		Charity ch1 = Fixture.createCharity(account1, "Charity 1", null, -1, 500, null, null);
		charityDao.save(ch1);
		expectFailure();
		flush();
		fail("Expected constraint violation");
    }

    @Test(expected = RollbackException.class)
    public void saveCharity_GoalPositive() throws Exception {
		Charity ch1 = Fixture.createCharity(account1, "Charity 1", null, 0, -1, null, null);
		charityDao.save(ch1);
		expectFailure();
		flush();
		fail("Expected constraint violation");
    }

    private void prepareCharities() {
    	charity1 = Fixture.createCharity(account1, "Charity 1", "Description 1", 100, 500, Fixture.placeSlingeland, null);
    	em.persist(charity1);
    	charity2 = Fixture.createCharity(account2, "Charity 2", "Description 2", 10, 500, Fixture.placeRozenkwekerijZutphen, null);
    	charity2.setCampaignEndTime(Instant.parse("2020-09-30T00:00:00Z"));
    	em.persist(charity2);
    	charity3 = Fixture.createCharity(account3, "Charity 3", "Description 3", 200, 500, Fixture.placeZieuwentRKKerk, null);
    	em.persist(charity3);
    	charity4 = Fixture.createCharity(account4, "Charity 4", "Description 4 finished", 400, 500, Fixture.placeZieuwentRKKerk, null);
    	assertNotNull("Account 4 is closed", account4.getClosedTime());
    	charity4.setCampaignEndTime(account4.getClosedTime().minusSeconds(3600));
    	em.persist(charity4);
    }

    @Test
    public void listCharities() throws BusinessException {
    	prepareCharities();
    	Instant now = Instant.parse("2020-09-25T12:00:00Z");
    	Cursor cursor = new Cursor(10, 0); 
    	// Search any
    	CharityFilter filter = new CharityFilter();
    	filter.validate();
    	PagedResult<Long> actual = charityDao.findCharities(now, filter, cursor);
    	assertNotNull(actual);
    	dump("non closed", charityDao.loadGraphs(actual.getData(), Charity.SHALLOW_ENTITY_GRAPH, Charity::getId));
    	assertEquals(3, actual.getTotalCount().intValue());

    	// Search location
    	filter = new CharityFilter();
    	filter.setLocation(Fixture.placeZieuwent);
    	filter.setRadius(1000);
    	filter.validate();
    	actual = charityDao.findCharities(now, filter, cursor);
    	assertNotNull(actual);
    	dump("nearby Zieuwent", charityDao.loadGraphs(actual.getData(), Charity.SHALLOW_ENTITY_GRAPH, Charity::getId));
    	assertEquals(1, actual.getCount());
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	filter.setRadius(50000);
    	actual = charityDao.findCharities(now, filter, cursor);
    	dump("all around Zieuwent", charityDao.loadGraphs(actual.getData(), Charity.SHALLOW_ENTITY_GRAPH, Charity::getId));
    	assertEquals(3, actual.getCount());

    	// Since 
    	filter = new CharityFilter();
    	filter.setSince(Instant.parse("2020-09-10T00:00:00Z"));
    	filter.validate();
    	actual = charityDao.findCharities(now, filter, cursor);
    	assertNotNull(actual);
    	dump("Start after " + filter.getSince(), charityDao.loadGraphs(actual.getData(), Charity.SHALLOW_ENTITY_GRAPH, Charity::getId));
    	assertEquals(1, actual.getCount());
    	assertTrue(actual.getData().contains(charity3.getId()));

    	// Until 
    	filter = new CharityFilter();
    	filter.setUntil(Instant.parse("2020-09-10T00:00:00Z"));
    	filter.validate();
    	actual = charityDao.findCharities(now, filter, cursor);
    	assertNotNull(actual);
    	dump("Started before " + filter.getUntil(), charityDao.loadGraphs(actual.getData(), Charity.SHALLOW_ENTITY_GRAPH, Charity::getId));
    	assertEquals(2, actual.getCount());
    	assertTrue(actual.getData().contains(charity1.getId()));
    	assertTrue(actual.getData().contains(charity2.getId()));

    	// Closed too - false
    	filter = new CharityFilter();
    	filter.setInactiveToo(false);
    	filter.validate();
    	actual = charityDao.findCharities(now, filter, cursor);
    	assertNotNull(actual);
    	dump("Active", charityDao.loadGraphs(actual.getData(), Charity.SHALLOW_ENTITY_GRAPH, Charity::getId));
    	assertEquals(3, actual.getCount());
    	
    	// Closed too - true
    	filter = new CharityFilter();
    	filter.setInactiveToo(true);
    	filter.validate();
    	assertNotNull(actual);
    	dump("All", charityDao.loadGraphs(actual.getData(), Charity.SHALLOW_ENTITY_GRAPH, Charity::getId));
    	assertEquals(4, actual.getCount());

    }

    @Test
    public void listCharities_verifyOrder() throws BusinessException {
    	prepareCharities();
    	Instant now = Instant.parse("2020-09-25T12:00:00Z");
    	Cursor cursor = new Cursor(10, 0); 

    	CharityFilter filter = new CharityFilter();
    	filter.validate();
    	PagedResult<Long> actual = charityDao.findCharities(now, filter, cursor);
    	assertNotNull(actual);
    	assertEquals(3, actual.getCount());
    	// Default order is by name ascending
    	assertEquals(charity1.getId(), actual.getData().get(0));
    	assertEquals(charity2.getId(), actual.getData().get(1));
    	assertEquals(charity3.getId(), actual.getData().get(2));

    	filter = new CharityFilter();
    	filter.setSortDir(SortDirection.DESC);
    	filter.validate();
    	actual = charityDao.findCharities(now, filter, cursor);
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	assertEquals(charity2.getId(), actual.getData().get(1));
    	assertEquals(charity1.getId(), actual.getData().get(2));

    	filter = new CharityFilter();
    	filter.setSortBy(CharitySortBy.DATE);
    	filter.setSortDir(SortDirection.ASC);
    	filter.validate();
    	actual = charityDao.findCharities(now, filter, cursor);
    	assertEquals(charity1.getId(), actual.getData().get(0));
    	assertEquals(charity2.getId(), actual.getData().get(1));
    	assertEquals(charity3.getId(), actual.getData().get(2));

    	filter = new CharityFilter();
    	filter.setLocation(Fixture.placeZieuwent);
    	filter.setRadius(50000);
    	filter.setSortBy(CharitySortBy.DISTANCE);
    	filter.setSortDir(SortDirection.ASC);
    	filter.validate();
    	actual = charityDao.findCharities(now, filter, cursor);
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	assertEquals(charity1.getId(), actual.getData().get(1));
    	assertEquals(charity2.getId(), actual.getData().get(2));

    	filter = new CharityFilter();
    	filter.setSortBy(CharitySortBy.SCORE);
    	filter.setSortDir(SortDirection.DESC);
    	filter.validate();
    	actual = charityDao.findCharities(now, filter, cursor);
    	dump("sort by score desc", charityDao.loadGraphs(actual.getData(), Charity.SHALLOW_ENTITY_GRAPH, Charity::getId));
    	assertEquals(charity3.getId(), actual.getData().get(0));
    	assertEquals(charity1.getId(), actual.getData().get(1));
    	assertEquals(charity2.getId(), actual.getData().get(2));
    }
    
    @Test(expected = BadRequestException.class)
    public void listCharities_OrderDistanceNoLocation() throws Exception {
    	Cursor cursor = new Cursor(10, 0); 
    	CharityFilter filter = new CharityFilter();
    	filter.setSortBy(CharitySortBy.DISTANCE);
    	filter.validate();
    	charityDao.findCharities(Instant.now(), filter, cursor);
		fail("Expected bad request");
    }

    
}
