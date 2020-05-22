package eu.netmobiel.planner.repository;

import static org.junit.Assert.*;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

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
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.rideshare.repository.mapping.LegMapper;

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
            .addPackages(true, LegMapper.class.getPackage())
            .addClass(OpenTripPlannerDao.class)
//            .addClass(Resources.class)
        	.addAsWebInfResource("jboss-deployment-structure.xml")
            // Arquillian tests need the beans.xml to recognize it as a CDI application
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
//		System.out.println(archive.toString(true));
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
			assertEquals(1, plan.getItineraries().size());
			Itinerary it = plan.getItineraries().get(0);
			assertEquals(1, it.getLegs().size());
    		Leg leg = it.getLegs().get(0);
        	int distance = leg.getDestinationStopDistance();
        	log.debug("Distance to destination is " + distance);
        	assertTrue("Remaining distance is < 10 meter", distance < 10);
		} catch (NotFoundException e) {
			fail("Did not expect " + e);
		} catch (BadRequestException e) {
			fail("Did not expect " + e);
		}
	}

	@Test
	public void testPlanCarOnlyVia() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	GeoLocation[] via = new GeoLocation[] { GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966") }; 
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR }; 
    	try {
			TripPlan plan = otpDao.createPlan(fromPlace, toPlace, departureTime, null, modes, false, null, Arrays.asList(via), 1);
			assertNotNull(plan);
			assertEquals(1, plan.getItineraries().size());
			Itinerary it = plan.getItineraries().get(0);
			assertEquals(2, it.getLegs().size());
    		Leg leg1 = it.getLegs().get(0);
    		Leg leg2 = it.getLegs().get(1);
    		assertTrue("Stop between legs must be the same object if equals", !leg1.getTo().equals(leg2.getFrom()) || leg1.getTo() == leg2.getFrom());
        	int distance = leg2.getDestinationStopDistance();
        	log.debug("Distance to destination is " + distance);
        	assertTrue("Remaining distance is < 10 meter", distance < 10);
		} catch (NotFoundException e) {
			fail("Did not expect " + e);
		} catch (BadRequestException e) {
			fail("Did not expect " + e);
		}
	}

	@Test
	public void testPlanTransitOutsideService() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.now().plusYears(1);
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.TRANSIT };
    	try {
    		otpDao.createPlan(fromPlace, toPlace, departureTime, null, modes, false, null, null, 1);
			fail("Expected a BadRequest");
		} catch (NotFoundException e) {
			fail("Did not expect " + e);
		} catch (BadRequestException e) {
			assertNotNull(e.getVendorCode());
			assertEquals("NO_TRANSIT_TIMES", e.getVendorCode());
			
		}
	}

	@Test
	public void testPlanTooClose() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR };
    	try {
    		otpDao.createPlan(fromPlace, toPlace, departureTime, null, modes, false, null, null, 1);
			fail("Expected a BadRequest");
		} catch (NotFoundException e) {
			fail("Did not expect " + e);
		} catch (BadRequestException e) {
			assertNotNull(e.getVendorCode());
			assertEquals("TOO_CLOSE", e.getVendorCode());
			
		}
	}

	@Test
	public void testPlanGoToNowhere() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Het Hilgelo Waterplas::51.993629,6.715974");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK };
    	try {
    		TripPlan plan = otpDao.createPlan(fromPlace, toPlace, departureTime, null, modes, false, 10, null, 1);
    		Leg leg = plan.getItineraries().get(0).getLegs().get(0);
//        	String wkt = GeometryHelper.createWKT(leg.getLegGeometry());
//        	System.out.println(wkt);
        	int distance = leg.getDestinationStopDistance();
        	log.debug("Distance to destination is " + distance);
        	assertTrue("Remaining distance is > 100 meter", distance > 100);
		} catch (NotFoundException e) {
			fail("Did not expect " + e);
		} catch (BadRequestException e) {
			fail("Did not expect " + e);
		}
	}

	@Test
	public void testPlanTransitMaxWalkDistanceLow() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK };
    	try {
    		otpDao.createPlan(fromPlace, toPlace, departureTime, null, modes, false, 100, null, 1);
			fail("Expected a NotFound");
		} catch (NotFoundException e) {
			assertNotNull(e.getVendorCode());
			assertEquals("PATH_NOT_FOUND", e.getVendorCode());
		} catch (BadRequestException e) {
			fail("Did not expect " + e);
		}
	}

	@Test
	public void testPlanTransitMaxWalkDistanceHigh() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK };
    	try {
    		TripPlan plan = otpDao.createPlan(fromPlace, toPlace, departureTime, null, modes, false, 1000, null, 1);
			assertNotNull(plan);
			assertEquals(1, plan.getItineraries().size());
    		Leg leg = plan.getItineraries().get(0).getLegs().get(0);
        	int distance = leg.getDestinationStopDistance();
        	log.debug("Distance to destination is " + distance);
        	assertTrue("Remaining distance is < 10 meter", distance < 10);
		} catch (NotFoundException e) {
			fail("Did not expect " + e);
		} catch (BadRequestException e) {
			fail("Did not expect " + e);
		}
	}
}
