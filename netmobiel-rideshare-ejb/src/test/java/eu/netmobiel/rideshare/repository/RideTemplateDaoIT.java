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
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.RideTemplate_;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.converter.BookingStateConverter;
import eu.netmobiel.rideshare.test.Fixture;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@RunWith(Arquillian.class)
public class RideTemplateDaoIT {
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
                .addPackages(true, RideTemplate.class.getPackage())
                .addPackages(true, RideTemplate_.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, BookingStateConverter.class.getPackage())
                .addPackages(true, Fixture.class.getPackage())
            .addClass(RideTemplateDao.class)
            .addClass(Resources.class)
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource("jboss-deployment-structure.xml");
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private RideTemplateDao rideTemplateDao;

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
        em.createQuery("delete from RideTemplate").executeUpdate();
        em.createQuery("delete from Car").executeUpdate();
        em.createQuery("delete from User").executeUpdate();
        utx.commit();
    }

    private void insertData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Inserting records...");
        driver1 = Fixture.createDriver1();
		em.persist(driver1);
//        driver2 = Fixture.createUser2();
//		em.persist(driver2);

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
    
    // We have to test all permutations of (departure time (D), horizon (H), system horizon (HS)
    // Horizon can be null too

    private void testSetup(Instant departureTime, Instant horizon, Instant systemHorizon, int expectCount) {
    	RideTemplate t = Fixture.createTemplate(car1, departureTime, horizon);
    	em.persist(t);
    	List<RideTemplate> templates = rideTemplateDao.findOpenTemplates(systemHorizon, 0, 10);
    	assertNotNull(templates);
    	assertEquals(expectCount, templates.size());
    }
    @Test
    public void findOpenTemplates_D_HS() {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Instant horizon = null;
    	Instant systemHorizon = Instant.parse("2020-06-01T00:00:00Z");
    	testSetup(departureTime, horizon, systemHorizon, 1);
    }

    @Test
    public void findOpenTemplates_HS_D() {
    	Instant departureTime = Instant.parse("2020-06-01T00:00:00Z");
    	Instant horizon = null;
    	Instant systemHorizon = Instant.parse("2020-05-01T00:00:00Z");
    	testSetup(departureTime, horizon, systemHorizon, 0);
    }

    @Test
    public void findOpenTemplates_D_H_HS() {
    	Instant departureTime = Instant.parse("2020-04-01T00:00:00Z");
    	Instant horizon = Instant.parse("2020-05-01T00:00:00Z");
    	Instant systemHorizon = Instant.parse("2020-06-01T00:00:00Z");
    	testSetup(departureTime, horizon, systemHorizon, 1);
    }

    @Test
    public void findOpenTemplates_D_HS_H() {
    	Instant departureTime = Instant.parse("2020-04-01T00:00:00Z");
    	Instant horizon = Instant.parse("2020-06-01T00:00:00Z");
    	Instant systemHorizon = Instant.parse("2020-05-01T00:00:00Z");
    	testSetup(departureTime, horizon, systemHorizon, 1);
    }

    @Test
    public void findOpenTemplates_H_D_HS() {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Instant horizon = Instant.parse("2020-04-01T00:00:00Z");
    	Instant systemHorizon = Instant.parse("2020-06-01T00:00:00Z");
    	testSetup(departureTime, horizon, systemHorizon, 0);
    }

    @Test
    public void findOpenTemplates_H_HS_D() {
    	Instant departureTime = Instant.parse("2020-06-01T00:00:00Z");
    	Instant horizon = Instant.parse("2020-04-01T00:00:00Z");
    	Instant systemHorizon = Instant.parse("2020-05-01T00:00:00Z");
    	testSetup(departureTime, horizon, systemHorizon, 0);
    }

    @Test
    public void findOpenTemplates_HS_D_H() {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Instant horizon = Instant.parse("2020-06-01T00:00:00Z");
    	Instant systemHorizon = Instant.parse("2020-04-01T00:00:00Z");
    	testSetup(departureTime, horizon, systemHorizon, 0);
    }

    @Test
    public void findOpenTemplates_HS_H_D() {
    	Instant departureTime = Instant.parse("2020-06-01T00:00:00Z");
    	Instant horizon = Instant.parse("2020-05-01T00:00:00Z");
    	Instant systemHorizon = Instant.parse("2020-04-01T00:00:00Z");
    	testSetup(departureTime, horizon, systemHorizon, 0);
    }
}
