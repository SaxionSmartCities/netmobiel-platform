package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.banker.test.Fixture;
import eu.netmobiel.commons.model.PagedResult;

@RunWith(Arquillian.class)
public class LedgerDaoIT extends BankerIntegrationTestBase {
	@Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
        	    .addClass(LedgerDao.class)
        ;
// 		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private Logger log;
    
    @Inject
    private LedgerDao ledgerDao;

	@Override
	protected void insertData() throws Exception {
		
	}

    private void dump(String subject, Collection<Ledger> ledgers) {
    	ledgers.forEach(obj -> log.info(subject + ": " + obj.toString()));
    }
    
    @Test
    public void saveLedger() {
    	String startTime = "2019-01-01T01:00:00Z";
    	String endTime = "2020-01-01T01:00:00Z";
    	String name = "ledger-1";
		Ledger ledger = Fixture.createLedger(name, startTime, endTime);
    	ledgerDao.save(ledger);
    	List<Ledger> actual = ledgerDao.findAll();
    	assertNotNull(actual);
    	assertEquals(1, actual.size());
    	Ledger ldg = actual.get(0);
    	assertEquals(endTime, DateTimeFormatter.ISO_INSTANT.format(ldg.getEndPeriod()));
    	assertEquals(startTime, DateTimeFormatter.ISO_INSTANT.format(ldg.getStartPeriod()));
    	assertEquals(name, ldg.getName());
    	dump("saveLedger", actual);
    }

    @Test
    public void findByDate() {
    	String startTime = "2019-01-01T01:00:00Z";
    	String endTime = "2020-01-01T01:00:00Z";
    	String name = "ledger-1";
		Ledger ledger = Fixture.createLedger(name, startTime, endTime);
    	ledgerDao.save(ledger);

    	String startTime2 = endTime;
    	String name2 = "ledger-2";
		Ledger ledger2 = Fixture.createLedger(name2, startTime2, null);
    	ledgerDao.save(ledger2);

    	Ledger actual = ledgerDao.findByDate(Instant.parse("2019-07-01T01:00:00Z"));
    	assertNotNull(actual);
    	assertEquals(name, actual.getName());

    	actual = ledgerDao.findByDate(Instant.parse("2020-07-01T01:00:00Z"));
    	assertNotNull(actual);
    	assertEquals(name2, actual.getName());

    	// border case start
    	actual = ledgerDao.findByDate(Instant.parse("2020-01-01T01:00:00Z"));
    	assertNotNull(actual);
    	assertEquals(name2, actual.getName());

    	// border case end
    	actual = ledgerDao.findByDate(Instant.parse("2020-01-01T01:00:00Z").minusSeconds(1));
    	assertNotNull(actual);
    	assertEquals(name, actual.getName());

    }

    @Test
    public void findByDate_NotFound() {
    	String startTime = "2019-01-01T01:00:00Z";
    	String endTime = "2020-01-01T01:00:00Z";
    	String name = "ledger-1";
		Ledger ledger = Fixture.createLedger(name, startTime, endTime);
    	ledgerDao.save(ledger);

    	String startTime2 = endTime;
    	String name2 = "ledger-2";
		Ledger ledger2 = Fixture.createLedger(name2, startTime2, null);
    	ledgerDao.save(ledger2);

    	try {
    		@SuppressWarnings("unused")
			Ledger actual = ledgerDao.findByDate(Instant.parse("2018-07-01T01:00:00Z"));
    		fail("Expected NoResultFoundException");
    	} catch (NoResultException ex) {
    		log.info("findByDate_NotFound: " + ex.toString());
    	}
    	
    }

    @Test
    public void listLedgers() {
    	ledgerDao.save(Fixture.createLedger("ledger-0", "2018-01-01T01:00:00Z", "2019-01-01T01:00:00Z"));
    	ledgerDao.save(Fixture.createLedger("ledger-1", "2019-01-01T01:00:00Z", "2020-01-01T01:00:00Z"));
    	ledgerDao.save(Fixture.createLedger("ledger-2", "2020-01-01T01:00:00Z", null));

    	PagedResult<Long> actual = ledgerDao.listLedgers(0, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertEquals(3, actual.getTotalCount().intValue());

    
    	actual = ledgerDao.listLedgers(1, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
    	assertEquals(1, actual.getData().size());
    	assertNull(actual.getTotalCount());
    	List<Ledger> ledgers = ledgerDao.loadGraphs(actual.getData(), null, Ledger::getId);
    	// sorting by startperiod desc
    	assertEquals("ledger-2", ledgers.get(0).getName());

    	actual = ledgerDao.listLedgers(10, 1);
    	assertNotNull(actual);
    	assertEquals(2, actual.getCount());
    	assertEquals(2, actual.getData().size());
    	assertNull(actual.getTotalCount());
    	ledgers = ledgerDao.loadGraphs(actual.getData(), null, Ledger::getId);
    	// sorting by startperiod desc
    	assertEquals("ledger-1", ledgers.get(0).getName());
    	assertEquals("ledger-0", ledgers.get(1).getName());

    }

}
