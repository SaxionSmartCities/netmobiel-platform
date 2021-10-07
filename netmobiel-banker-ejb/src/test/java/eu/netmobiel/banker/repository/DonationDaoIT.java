package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.banker.filter.DonationFilter;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.DonationSortBy;
import eu.netmobiel.banker.repository.DonationDao.CharityPopularity;
import eu.netmobiel.banker.repository.DonationDao.DonorGenerosity;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.banker.test.Fixture;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;

@RunWith(Arquillian.class)
public class DonationDaoIT  extends BankerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
                .addClass(DonationFilter.class)
                .addClass(DonationDao.class)
;
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private Logger log;
    
    @Inject
    private DonationDao donationDao;

    private BankerUser driver1;
    private BankerUser passenger1;
    private Instant now = Instant.parse("2020-09-25T12:00:00Z");
    private Charity charity1;
    private Charity charity2;
    private Charity charity3;
    private Charity charity4;
    
    @Override
	public boolean isSecurityRequired() {
    	return true;
    }

    @Override
    protected void insertData() throws Exception {
        driver1 = Fixture.createUser(loginContextDriver);
		em.persist(driver1);
        passenger1 = Fixture.createUser(loginContextPassenger);
		em.persist(passenger1);
		
		account1 = Account.newInstant("PAL-1", "Account 1", AccountType.LIABILITY, Instant.parse("2020-07-01T00:00:00Z"));
		em.persist(account1);
    	account2 = Account.newInstant("PLA-2", "Account 2", AccountType.LIABILITY, Instant.parse("2020-09-01T00:00:00Z"));
    	em.persist(account2);
    	account3 = Account.newInstant("PLA-3", "Account 3", AccountType.LIABILITY, Instant.parse("2020-09-15T00:00:00Z"));
    	em.persist(account3);
    	account4 = Account.newInstant("PLA-4", "Account 4 closed", AccountType.LIABILITY, Instant.parse("2020-07-01T00:00:00Z"));
    	account4.setClosedTime(Instant.parse("2020-07-31T00:00:00Z"));
    	em.persist(account4);
    	
    	charity1 = Fixture.createCharity(account1, "Charity 1", "Description 1", 100, 500, Fixture.placeSlingeland, null);
    	em.persist(charity1);
    	charity2 = Fixture.createCharity(account2, "Charity 2", "Description 2", 100, 500, Fixture.placeRozenkwekerijZutphen, null);
    	charity2.setCampaignEndTime(Instant.parse("2020-09-30T00:00:00Z"));
    	em.persist(charity2);
    	charity3 = Fixture.createCharity(account3, "Charity 3", "Description 3", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	em.persist(charity3);
    	charity4 = Fixture.createCharity(account4, "Charity 4", "Description 4 finished", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	charity4.setCampaignEndTime(account4.getClosedTime().minusSeconds(3600));
    	em.persist(charity4);
    	
    	Donation d0 = Fixture.createDonation(charity4, driver1, "Donatie 0", 50, Instant.parse("2020-07-02T12:00:00Z"), false);
    	em.persist(d0);;
    	Donation d1 = Fixture.createDonation(charity1, driver1, "Donatie 1", 10, Instant.parse("2020-07-03T12:00:00Z"), false);
    	em.persist(d1);;
    	Donation d2 = Fixture.createDonation(charity1, driver1, "Donatie 2", 20, Instant.parse("2020-07-04T12:00:00Z"), false);
    	em.persist(d2);;
    	Donation d3 = Fixture.createDonation(charity1, driver1, "Donatie 3", 30, Instant.parse("2020-07-05T12:00:00Z"), false);
    	em.persist(d3);;
    	Donation d4 = Fixture.createDonation(charity1, passenger1, "Donatie 4", 10, Instant.parse("2020-07-06T12:00:00Z"), false);
    	em.persist(d4);;
    	Donation d5 = Fixture.createDonation(charity2, passenger1, "Donatie 5", 20, Instant.parse("2020-07-07T12:00:00Z"), false);
    	em.persist(d5);
    	Donation d6 = Fixture.createDonation(charity2, passenger1, "Donatie 6", 300, Instant.parse("2020-07-08T12:00:00Z"), true);
    	em.persist(d6);
    	Donation d7 = Fixture.createDonation(charity3, driver1, "Donatie 7", 90, Instant.parse("2020-07-08T16:00:00Z"), false);
    	em.persist(d7);
    }
    
    private void dump(String subject, Collection<Donation> donations) {
    	donations.forEach(d -> log.info(String.format("%s: %s C %s U %s A %s %s %s", 
    			subject, 
    			d.getId() != null ? d.getId() : "-", 
    			d.getCharity() != null ? d.getCharity().getId() : "-", 
    			d.getUser() != null ? d.getUser().getId() : "-", 
    			d.getAmount(), 
    			d.getDescription(),
    			d.getDonationTime() != null ? d.getDonationTime().toString() : "-")));
    }
    
    
    @Test
    public void listDonation_Charity1() throws BusinessException {
    	try {
			DonationFilter filter = new DonationFilter();
			filter.setCharity(charity1);
			filter.setNow(now);
			filter.setSortBy(DonationSortBy.DATE);
			filter.setSortDir(SortDirection.DESC);
	    	filter.validate();
			Cursor cursor = new Cursor(3, 0);
			PagedResult<Long> ids = donationDao.listDonations(filter, cursor);
			assertNotNull(ids);
			List<Donation> donations = donationDao.loadGraphs(ids.getData(), Donation.CHARITY_GRAPH, Donation::getId);
			dump("listDonations charity 1 limit 3", donations);
			Donation d1 = donations.get(0); 
			Donation d2 = donations.get(1);
			Donation d3 = donations.get(2);
			donations.forEach(d -> assertEquals(charity1, d.getCharity()));
			assertTrue(d2.getDonationTime().isBefore(d1.getDonationTime()));
			assertTrue(d3.getDonationTime().isBefore(d2.getDonationTime()));
			assertEquals(3, ids.getData().size());
			assertEquals(4, ids.getTotalCount().intValue());
			assertEquals(3, ids.getCount());
		} catch (Throwable t) {
    		log.error("Error", t);
    		throw t;
		}
    }    

    @Test
    public void reportCharityPopularityTopN_Default() throws BusinessException {
    	DonationFilter filter = new DonationFilter();
    	filter.setNow(now);
    	filter.setOmitInactiveCharities(true);
		filter.setSortBy(DonationSortBy.DONORS);
		filter.setSortDir(SortDirection.DESC);
    	filter.validate();
    	Cursor cursor = new Cursor(5, 0);
    	try {
    		PagedResult<CharityPopularity> results = donationDao.reportCharityPopularityTopN(filter, cursor);
        	assertNotNull(results);
        	assertEquals(3, results.getData().size());
        	CharityPopularity d1 = results.getData().get(0); 
        	CharityPopularity d2 = results.getData().get(1);
        	assertNotNull(d1.charityId);
        	assertNotNull(d1.donorCount);
        	assertNotNull(d2.charityId);
        	assertNotNull(d2.donorCount);
        	assertTrue(d2.donorCount < d1.donorCount);
        	assertNotNull(results.getTotalCount());
        	assertEquals(3, results.getTotalCount().intValue());
        	assertEquals(3, results.getCount());
        	results.getData().forEach(d -> log.info(String.format("reportCharityPopularityTopN_Default: C %s D %s", d.charityId, d.donorCount))); 
    	} catch (Throwable t) {
    		log.error("Error", t);
    		throw t;
    	}
    }

    @Test
    public void reportMostRecentlyDonatedTo() throws BusinessException {
    	Cursor cursor = new Cursor(5, 0);
    	PagedResult<Long> ids = donationDao.reportMostRecentDistinctDonations(passenger1, cursor);
    	assertNotNull(ids);
    	assertEquals(2, ids.getData().size());
    	Long id1 = ids.getData().get(0); 
    	Long id2 = ids.getData().get(1);
    	log.info("Recently donated: " + id1 + " " + id2);
    	assertNotNull(id1);
    	assertNotNull(id2);
    	// Do not forget to add the identity function, otherwise the order is unspecified!
    	List<Donation> donations = donationDao.loadGraphs(ids.getData(), Donation.CHARITY_GRAPH, Donation::getId);
    	dump("Default Recently Donated", donations);
    	Donation d1 = donations.get(0); 
    	Donation d2 = donations.get(1);
    	assertEquals("Donatie 6", d1.getDescription());
    	assertEquals("Donatie 4", d2.getDescription());
    	assertEquals(300, d1.getAmount().intValue());
    	assertEquals(10, d2.getAmount().intValue());
    	assertNotNull(ids.getTotalCount());
    	assertEquals(2, ids.getTotalCount().intValue());
    	assertEquals(2, ids.getCount());
    	dump("reportMostRecentlyDonatedTo", donations);
    }

    @Test
    public void reportMostGenerousDonor_General() throws BusinessException {
    	DonationFilter filter = new DonationFilter();
    	filter.setOmitInactiveCharities(true);
    	filter.setNow(now);
		filter.setSortBy(DonationSortBy.AMOUNT);
		filter.setSortDir(SortDirection.DESC);
    	filter.validate();
    	Cursor cursor = new Cursor(5, 0);
    	
    	PagedResult<DonorGenerosity> results = donationDao.reportDonorGenerosityTopN(filter, cursor);
    	assertNotNull(results);
    	DonorGenerosity d1 = results.getData().get(0); 
    	DonorGenerosity d2 = results.getData().get(1);
    	assertEquals(driver1.getId().longValue(), d1.donorId);
    	assertEquals(passenger1.getId().longValue(), d2.donorId);
    	results.getData().forEach(d -> log.info(String.format("Donor generosity general: U %s A %s", d.donorId, d.amount))); 
    	assertEquals(150, d1.amount);
    	assertEquals(30, d2.amount);
    	assertNotNull(results.getTotalCount());
    	assertEquals(2, results.getTotalCount().intValue());
    	assertEquals(2, results.getCount());
    }

    @Test
    public void reportMostGenerousDonor_Charity() throws BusinessException {
    	DonationFilter filter = new DonationFilter();
    	Cursor cursor = new Cursor(5, 0);
    	filter.setNow(now);
    	filter.setCharity(charity1);
		filter.setSortBy(DonationSortBy.AMOUNT);
		filter.setSortDir(SortDirection.DESC);
    	filter.validate();
    	PagedResult<DonorGenerosity> results = donationDao.reportDonorGenerosityTopN(filter, cursor);
    	assertNotNull(results);
    	DonorGenerosity d1 = results.getData().get(0); 
    	DonorGenerosity d2 = results.getData().get(1);
    	assertEquals(driver1.getId().longValue(), d1.donorId);
    	assertEquals(passenger1.getId().longValue(), d2.donorId);
    	results.getData().forEach(d -> log.info(String.format("Donor generosity charity1: U %s A %s", d.donorId, d.amount))); 
    	assertEquals(60, d1.amount);
    	assertEquals(10, d2.amount);
    	assertNotNull(results.getTotalCount());
    	assertEquals(2, results.getTotalCount().intValue());
    	assertEquals(2, results.getCount());
    }
}
