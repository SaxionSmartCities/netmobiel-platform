package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.PersistenceUnitUtil;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.netmobiel.banker.filter.DonationFilter;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.Donation_;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.banker.test.Fixture;
import eu.netmobiel.commons.exception.BusinessException;

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
    private DonationDao donationDao;

    private BankerUser driver1;
    private BankerUser passenger1;
    private Account account1;
    private Instant now = Instant.parse("2020-09-25T12:00:00Z");
    private Charity charity1;
    private Charity charity2;
    private Charity charity3;
    private Charity charity4;
    
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
    	Account account2 = Account.newInstant("PLA-2", "Account 2", AccountType.LIABILITY, Instant.parse("2020-09-01T00:00:00Z"));
    	em.persist(account2);
    	Account account3 = Account.newInstant("PLA-3", "Account 3", AccountType.LIABILITY, Instant.parse("2020-09-15T00:00:00Z"));
    	em.persist(account3);
    	Account account4 = Account.newInstant("PLA-4", "Account 4 closed", AccountType.LIABILITY, Instant.parse("2020-07-01T00:00:00Z"));
    	account4.setClosedTime(Instant.parse("2020-07-31T00:00:00Z"));
    	em.persist(account4);
    	
    	charity1 = Fixture.createCharity(account1, "Description 1", 100, 500, Fixture.placeSlingeland, null);
    	em.persist(charity1);
    	charity2 = Fixture.createCharity(account2, "Description 2", 100, 500, Fixture.placeRozenkwekerijZutphen, null);
    	charity2.setCampaignEndTime(Instant.parse("2020-09-30T00:00:00Z"));
    	em.persist(charity2);
    	charity3 = Fixture.createCharity(account3, "Description 3", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	em.persist(charity3);
    	charity4 = Fixture.createCharity(account4, "Description 4 finished", 100, 500, Fixture.placeZieuwentRKKerk, null);
    	charity4.setCampaignEndTime(account4.getClosedTime().minusSeconds(3600));
    	em.persist(charity4);
    	
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
    }
    
    private void dump(String subject, Collection<Donation> donations) {
    	donations.forEach(d -> log.info(String.format("%s: %s C %s U %s A %s #%s %s %s", 
    			subject, 
    			d.getId() != null ? d.getId() : "-", 
    			d.getCharity() != null ? d.getCharity().getId() : "-", 
    			d.getUser() != null ? d.getUser().getId() : "-", 
    			d.getAmount(), 
    			d.getCount() != null ? d.getCount() : "",
    			d.getDescription(),
    			d.getDonationTime() != null ? d.getDonationTime().toString() : "-")));
    }
    
    
    @Test
    public void reportCharityPopularityTopN_Default() throws BusinessException {
    	DonationFilter filter = new DonationFilter();
    	filter.setNow(now);
    	
    	List<Donation> results = donationDao.reportCharityPopularityTopN(filter, 3);
    	assertNotNull(results);
//    	dump("Default filter", results);
    	assertEquals(2, results.size());
    	Donation d1 = results.get(0); 
    	Donation d2 = results.get(1);
    	assertNotNull(d1.getCount());
    	assertNotNull(d2.getCount());
    	assertNull(d2.getUser());
    	assertTrue(d2.getCount() < d1.getCount());
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertFalse(em.contains(d1));
    	assertFalse(puu.isLoaded(d1, Donation_.CHARITY));
//    	assertTrue(puu.isLoaded(d1.getCharity(), Charity_.ACCOUNT));
    }

    @Test
    public void reportMostRecentlyDonatedTo() throws BusinessException {
    	List<Long> ids = donationDao.reportMostRecentDistinctDonations(passenger1, 3);
    	assertNotNull(ids);
    	assertEquals(2, ids.size());
    	Long id1 = ids.get(0); 
    	Long id2 = ids.get(1);
    	log.info("Recently donated: " + id1 + " " + id2);
    	assertNotNull(id1);
    	assertNotNull(id2);
    	// Do not forget to add the identity function, otherwise the order is unspecified!
    	List<Donation> donations = donationDao.fetch(ids, Donation.REPORT_TOP_N_CHARITY, Donation::getId);
    	dump("Default Recently Donated", donations);
    	Donation d1 = donations.get(0); 
    	Donation d2 = donations.get(1);
    	assertEquals("Donatie 6", d1.getDescription());
    	assertEquals("Donatie 4", d2.getDescription());
    	assertEquals(d1.getAmount().intValue(), 300);
    	assertEquals(d2.getAmount().intValue(), 10);
    	
    }

    @Test
    public void reportMostGenerousDonor_General() throws BusinessException {
    	DonationFilter filter = new DonationFilter();
    	filter.setNow(now);
    	
    	List<Donation> donations = donationDao.reportDonorGenerousityTopN(filter, 3);
    	assertNotNull(donations);
    	Donation d1 = donations.get(0); 
    	Donation d2 = donations.get(1);
    	assertEquals(driver1.getId(), d1.getUser().getId());
    	assertEquals(passenger1.getId(), d2.getUser().getId());
    	dump("Donor generousity", donations);
    	assertEquals(d1.getAmount().intValue(), 60);
    	assertEquals(d2.getAmount().intValue(), 30);
    	
    }

    @Test
    public void reportMostGenerousDonor_Charity() throws BusinessException {
    	DonationFilter filter = new DonationFilter();
    	filter.setNow(now);
    	filter.setCharity(charity1);
    	List<Donation> donations = donationDao.reportDonorGenerousityTopN(filter, 3);
    	assertNotNull(donations);
    	Donation d1 = donations.get(0); 
    	Donation d2 = donations.get(1);
    	assertEquals(driver1.getId(), d1.getUser().getId());
    	assertEquals(passenger1.getId(), d2.getUser().getId());
    	dump("Donor generousity for charity1", donations);
    	assertEquals(d1.getAmount().intValue(), 60);
    	assertEquals(d2.getAmount().intValue(), 10);
    	
    }
}
