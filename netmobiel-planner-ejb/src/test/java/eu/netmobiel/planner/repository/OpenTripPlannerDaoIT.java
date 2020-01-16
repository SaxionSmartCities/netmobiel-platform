package eu.netmobiel.planner.repository;

import static org.junit.Assert.*;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.mapping.TripPlanMapper;

@RunWith(Arquillian.class)
public class OpenTripPlannerDaoIT {
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
//                .addPackage(PlannerUrnHelper.class.getPackage())
            .addPackages(true, TripPlan.class.getPackage())
            .addPackages(true, TripPlanMapper.class.getPackage())
            .addClass(OpenTripPlannerDao.class)
//            .addClass(Resources.class)
        	.addAsWebInfResource("jboss-deployment-structure.xml")
            // Arquillian tests need the beans.xml to recognize it as a CDI application
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private OpenTripPlannerDao otpDao;

    @Inject
    private Logger log;

	@Test
	public void testPlanCarOnly() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR }; 
    	try {
			TripPlan plan = otpDao.createPlan(fromPlace, toPlace, departureTime, null, modes, false, null, null, 1);
			assertNotNull(plan);
		} catch (NotFoundException e) {
			fail("Did not expect " + e);
		} catch (BadRequestException e) {
			fail("Did not expect " + e);
		}
	}

}