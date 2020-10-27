package eu.netmobiel.rideshare.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.test.Fixture;
import eu.netmobiel.rideshare.test.RideshareIntegrationTestBase;

@RunWith(Arquillian.class)
public class RideTemplateDaoIT extends RideshareIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(RideTemplateDao.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private RideTemplateDao rideTemplateDao;

    @SuppressWarnings("unused")
	@Inject
    private Logger log;
    
    private RideshareUser driver1;
    private Car car1;

    protected void insertData() throws Exception {
        driver1 = Fixture.createDriver1();
		em.persist(driver1);

		car1 = Fixture.createCarVolvo(driver1);
		em.persist(car1);

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
