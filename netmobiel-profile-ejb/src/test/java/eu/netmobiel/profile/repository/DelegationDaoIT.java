package eu.netmobiel.profile.repository;

import static org.junit.Assert.*;

import java.time.Instant;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.filter.DelegationFilter;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.test.Fixture;
import eu.netmobiel.profile.test.ProfileIntegrationTestBase;

@RunWith(Arquillian.class)
public class DelegationDaoIT extends ProfileIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(DelegationDao.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private DelegationDao delegationDao;

	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    private Profile careTaker1;
    private Profile passenger1;
    private Profile passenger2;
    private Profile driver1;


    @Override
	protected void insertData() throws Exception {
		passenger1 = Fixture.createPassenger1();
		em.persist(passenger1);

		passenger2 = Fixture.createPassenger2();
		em.persist(passenger2);

		driver1 = Fixture.createDriver1();
		em.persist(driver1);

		careTaker1 = Fixture.createDriver3();
		em.persist(careTaker1);
    }

    
    @Test
    public void createDelegation() throws Exception {
    	Delegation del = Fixture.createDelegation(careTaker1, passenger1, Instant.parse("2021-03-22T10:21:00Z"), "1234");
    	delegationDao.save(del);
    	flush();
    	
    	assertNotNull(del.getId());
    	Delegation deldb = delegationDao.loadGraph(del.getId(), Delegation.DEFAULT_ENTITY_GRAPH).orElse(null);
    	assertNotNull(deldb);
    	assertEquals(del.getActivationTime(), deldb.getActivationTime());
    	assertEquals(del.getSubmissionTime(), deldb.getSubmissionTime());
    	assertEquals(del.getActivationCode(), deldb.getActivationCode());
    	assertEquals(careTaker1.getId(), deldb.getDelegate().getId());
    	assertEquals(passenger1.getId(), deldb.getDelegator().getId());
    }

    @Test
    public void testDelegationActive() throws Exception {
    	Delegation del = Fixture.createDelegation(careTaker1, passenger1, Instant.parse("2021-03-22T10:21:00Z"), "1234");
    	delegationDao.save(del);
    	flush();
    	
    	assertTrue(delegationDao.isDelegationActive(careTaker1, passenger1, null));
    	assertFalse(delegationDao.isDelegationActive(passenger1, careTaker1, null));
    	assertFalse(delegationDao.isDelegationActive(careTaker1, driver1, null));
    	assertFalse(delegationDao.isDelegationActive(careTaker1, careTaker1, null));
    }

    @Test
    public void testTerminateDelegation() throws Exception {
    	Delegation del = Fixture.createDelegation(careTaker1, passenger1, Instant.parse("2021-03-22T10:21:00Z"), "1234");
    	delegationDao.save(del);
    	flush();
    	assertTrue(delegationDao.isDelegationActive(careTaker1, passenger1, null));
    	assertTrue(delegationDao.isDelegationActive(careTaker1, passenger1, Instant.parse("2021-03-23T00:00:00Z")));
    	Delegation deldb = delegationDao.find(del.getId()).orElse(null);
    	assertNotNull(deldb);
    	deldb.setRevocationTime(deldb.getActivationTime().plusSeconds(3600));
    	flush();
    	deldb = delegationDao.find(del.getId()).orElse(null);
    	assertNotNull(deldb.getRevocationTime());
    	assertFalse(delegationDao.isDelegationActive(careTaker1, passenger1, null));
    	assertTrue(delegationDao.isDelegationActive(careTaker1, passenger1, Instant.parse("2021-03-22T10:30:00Z")));
    	assertFalse(delegationDao.isDelegationActive(careTaker1, passenger1, Instant.parse("2021-03-22T12:30:00Z")));
    }

    @Test
    public void testListDelegations() throws Exception {
    	DelegationFilter df = new DelegationFilter();
    	Cursor cursor = new Cursor(10, 0);
    	df.validate();
    	
    	PagedResult<Long> delegations = delegationDao.listDelegations(df, cursor);
    	assertNotNull(delegations);
    	assertEquals(0, delegations.getCount());
    
    	Delegation del1 = Fixture.createDelegation(careTaker1, passenger1, Instant.parse("2021-03-22T10:21:00Z"), "1234");
    	delegationDao.save(del1);
    	Delegation del2 = Fixture.createDelegation(careTaker1, passenger2, Instant.parse("2021-03-22T10:21:00Z"), "1234");
    	delegationDao.save(del2);
    	flush();

    	delegations = delegationDao.listDelegations(df, cursor);
    	assertNotNull(delegations);
    	assertEquals(2, delegations.getCount());
    	
    	df.setDelegator(passenger1);
    	delegations = delegationDao.listDelegations(df, cursor);
    	assertEquals(1, delegations.getCount());

    	df.setDelegate(careTaker1);
    	delegations = delegationDao.listDelegations(df, cursor);
    	assertEquals(1, delegations.getCount());

    	df.setDelegate(driver1);
    	delegations = delegationDao.listDelegations(df, cursor);
    	assertEquals(0, delegations.getCount());

    	df.setDelegator(null);
    	df.setDelegate(careTaker1);
    	delegations = delegationDao.listDelegations(df, cursor);
    	assertEquals(2, delegations.getCount());

    	df.setDelegate(null);
    	df.setSince(Instant.parse("2021-01-01T00:00:00Z"));
    	df.setUntil(Instant.parse("2021-03-01T00:00:00Z"));
    	delegations = delegationDao.listDelegations(df, cursor);
    	assertEquals(0, delegations.getCount());

    	df.setUntil(Instant.parse("2021-04-01T00:00:00Z"));
    	delegations = delegationDao.listDelegations(df, cursor);
    	assertEquals(2, delegations.getCount());

    	Delegation deldb = delegationDao.find(del1.getId()).orElse(null);
    	assertNotNull(deldb);
    	deldb.setRevocationTime(deldb.getActivationTime().plusSeconds(3600));
    	flush();

    	df.setNow(deldb.getActivationTime().plusSeconds(1800));
    	delegations = delegationDao.listDelegations(df, cursor);
    	assertEquals(2, delegations.getCount());

    	df.setNow(deldb.getRevocationTime().plusSeconds(3600));
    	delegations = delegationDao.listDelegations(df, cursor);
    	assertEquals(1, delegations.getCount());

    	df.setInactiveToo(true);
    	delegations = delegationDao.listDelegations(df, cursor);
    	assertEquals(2, delegations.getCount());
    }
}
