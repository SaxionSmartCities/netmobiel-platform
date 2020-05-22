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
    
    public void findRidesBeyondTemplateSetup(int depShift, int expectedCount) {
    	Instant departureTime = Instant.parse("2020-06-01T00:00:00Z");
    	RideTemplate t = Fixture.createTemplate(car1, departureTime, null);
    	em.persist(t);

    	Ride r1 = Fixture.createRide(t, departureTime.plusSeconds(depShift));
    	em.persist(r1);
    	
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
}
