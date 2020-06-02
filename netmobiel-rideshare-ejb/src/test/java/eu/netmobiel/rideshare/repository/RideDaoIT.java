package eu.netmobiel.rideshare.repository;


import static org.junit.Assert.*;

import java.io.File;
import java.time.Instant;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.Resources;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.Ride_;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.converter.BookingStateConverter;
import eu.netmobiel.rideshare.test.Fixture;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@RunWith(Arquillian.class)
public class RideDaoIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	File[] deps = Maven.configureResolver()
				.loadPomFromFile("pom.xml")
				.importCompileAndRuntimeDependencies() 
				.resolve()
				.withTransitivity()
				.asFile();
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(deps)
                .addPackages(true, RideshareDatabase.class.getPackage())
                .addPackages(true, RideshareUrnHelper.class.getPackage())
                .addPackages(true, Ride.class.getPackage())
                .addPackages(true, Ride_.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, BookingStateConverter.class.getPackage())
                .addPackages(true, Fixture.class.getPackage())
            .addClass(RideDao.class)
            .addClass(Resources.class)
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource("jboss-deployment-structure.xml");
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private RideDao rideDao;

    @PersistenceContext(unitName = "pu-rideshare")
    private EntityManager em;
    
    @Inject
    private UserTransaction utx;
    
    @Inject
    private Logger log;

    private User driver1;
    private Car car1;

	@Before
    public void preparePersistenceTest() throws Exception {
        clearData();
        insertData();
        startTransaction();
    }
    
    private void clearData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Dumping old records...");
        em.createQuery("delete from Leg").executeUpdate();
        em.createQuery("delete from Stop").executeUpdate();
        em.createQuery("delete from Ride").executeUpdate();
        em.createQuery("delete from RideTemplate").executeUpdate();
        em.createQuery("delete from Car").executeUpdate();
        em.createQuery("delete from User").executeUpdate();
        utx.commit();
    }

    private void insertData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Inserting records...");
        driver1 = Fixture.createUser1();
		em.persist(driver1);
		car1 = Fixture.createCarVolvo(driver1);
		em.persist(car1);

        utx.commit();
        // clear the persistence context (first-level cache)
        em.clear();
    }

    private void startTransaction() throws Exception {
        utx.begin();
        em.joinTransaction();
    }

    @After
    public void commitTransaction() throws Exception {
        utx.commit();
    }
    
	protected void flush() throws Exception {
		utx.commit();
		// clear the persistence context (first-level cache)
		em.clear();
		utx.begin();
		em.joinTransaction();
	}

    private void saveNewRide(Ride r) {
    	rideDao.save(r);
    	r.getStops().forEach(stop -> em.persist(stop));
    	r.getLegs().forEach(leg -> em.persist(leg));
    }

    public void findRidesBeyondTemplateSetup(int depShift, int expectedCount) {
    	Instant departureTime = Instant.parse("2020-06-01T00:00:00Z");
    	RideTemplate t = Fixture.createTemplate(car1, departureTime, null);
    	em.persist(t);

    	Ride r1 = Fixture.createRide(t, departureTime.plusSeconds(depShift));
    	saveNewRide(r1);
    	
    	List<Ride> rides = rideDao.findRidesBeyondTemplate(t);
    	assertNotNull(rides);
    	assertEquals(expectedCount, rides.size());
    }

    @Test
    public void findRidesBeyondTemplate_None() {
    	Instant departureTime = Instant.parse("2020-06-01T00:00:00Z");
    	RideTemplate t = Fixture.createTemplate(car1, departureTime, null);
    	em.persist(t);

    	List<Ride> rides = rideDao.findRidesBeyondTemplate(t);
    	assertNotNull(rides);
    	assertEquals(0, rides.size());
    }

    @Test
    public void findRidesBeyondTemplate_Before() {
    	findRidesBeyondTemplateSetup(-120 * 60, 0);
    }

    @Test
    public void findRidesBeyondTemplate_ArrivalOverlap() {
    	findRidesBeyondTemplateSetup(-5 * 60, 1);
    }

    @Test
    public void findRidesBeyondTemplate_DepartureOverlap() {
    	findRidesBeyondTemplateSetup(5 * 60, 1);
    }

    @Test
    public void findRidesBeyondTemplate_After() {
    	findRidesBeyondTemplateSetup(120 * 60, 1);
    }

    @Test
    public void listRides_NoFilter() {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);
    	
    	PagedResult<Long> rides = rideDao.findByDriver(driver1, null, null, null, 0, 0);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());
    }

    @Test
    public void listRides_Since() {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);
    	
    	Instant since = Instant.parse("2020-06-01T00:00:00Z");
    	PagedResult<Long> rides = rideDao.findByDriver(driver1, since, null, null, 0, 0);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());

    	since = Instant.parse("2020-06-03T00:00:00Z");
    	rides = rideDao.findByDriver(driver1, since, null, null, 0, 0);
    	assertNotNull(rides);
    	assertEquals(0, rides.getTotalCount().intValue());
    }

    @Test
    public void listRides_Until() {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);

    	Instant until = Instant.parse("2020-06-01T00:00:00Z");
    	PagedResult<Long> rides = rideDao.findByDriver(driver1, null, until, null, 0, 0);
    	assertNotNull(rides);
    	assertEquals(0, rides.getTotalCount().intValue());

    	until = Instant.parse("2020-06-03T00:00:00Z");
    	rides = rideDao.findByDriver(driver1, null, until, null, 0, 0);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());
    }

    @Test
    public void listRides_Deleted() throws Exception {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);

    	Boolean deletedToo = Boolean.FALSE;
    	PagedResult<Long> rides = rideDao.findByDriver(driver1, null, null, deletedToo, 0, 0);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());

    	r1.setDeleted(true);
    	flush();
    	rides = rideDao.findByDriver(driver1, null, null, deletedToo, 0, 0);
    	assertNotNull(rides);
    	assertEquals(0, rides.getTotalCount().intValue());

    	deletedToo = Boolean.TRUE;
    	rides = rideDao.findByDriver(driver1, null, null, deletedToo, 0, 0);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());
    }
}
