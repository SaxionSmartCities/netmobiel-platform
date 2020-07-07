package eu.netmobiel.planner.repository;

import static org.junit.Assert.*;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PlannerReport;
import eu.netmobiel.planner.model.PlannerResult;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.mapping.TripPlanMapper;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@RunWith(Arquillian.class)
public class OpenTripPlannerDaoIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	File[] deps = Maven.configureResolver()
				.loadPomFromFile("pom.xml")
				.importCompileAndRuntimeDependencies() 
				.resolve()
				.withTransitivity()
//				.using(new RejectDependenciesStrategy("eu.netmobiel:netmobiel-rideshare-ejb"))
				.asFile();
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(deps)
//                .addPackage(PlannerUrnHelper.class.getPackage())
                .addPackages(true, PlannerUrnHelper.class.getPackage())
                .addPackages(true, TripPlan.class.getPackage())
                .addPackages(true, TripPlanMapper.class.getPackage())
                .addClass(OpenTripPlannerDao.class)
//            	.addClass(Resources.class)
                .addAsWebInfResource("jboss-deployment-structure.xml")
                .addAsResource("log4j.properties")
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
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.CAR })); 
    	Instant now = departureTime.minusSeconds(24 * 60 * 60);
		PlannerResult result = otpDao.createPlan(now, fromPlace, toPlace, departureTime, false, modes, false, null, null, null, 1);
		assertNotNull(result);
		assertNotNull(result.getItineraries());
		assertNotNull(result.getReport());
		PlannerReport report = result.getReport();
		assertNotNull(report.getExecutionTime());
		assertEquals(1, result.getItineraries().size());
		Itinerary it = result.getItineraries().get(0);
		assertEquals(1, it.getLegs().size());
		Leg leg = it.getLegs().get(0);
    	int distance = leg.getDestinationStopDistance();
    	log.debug("Distance to destination is " + distance);
    	assertTrue("Remaining distance is < 10 meter", distance < 10);
		assertNull(result.getReport().getErrorText());
	}

	@Test
	public void testPlanCarOnlyVia() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	GeoLocation[] via = new GeoLocation[] { GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966") }; 
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.CAR })); 
    	Instant now = departureTime.minusSeconds(24 * 60 * 60);
		PlannerResult result = otpDao.createPlan(now, fromPlace, toPlace, departureTime, false, modes, false, null, null, Arrays.asList(via), 1);
		assertNotNull(result);
		assertNotNull(result.getItineraries());
		assertNotNull(result.getReport());
		assertEquals(1, result.getItineraries().size());
		Itinerary it = result.getItineraries().get(0);
		assertEquals(2, it.getLegs().size());
		Leg leg1 = it.getLegs().get(0);
		Leg leg2 = it.getLegs().get(1);
		assertTrue("Stop between legs must be the same object if equals", !leg1.getTo().equals(leg2.getFrom()) || leg1.getTo() == leg2.getFrom());
    	int distance = leg2.getDestinationStopDistance();
    	log.debug("Distance to destination is " + distance);
    	assertTrue("Remaining distance is < 10 meter", distance < 10);
	}

	@Test
	public void testPlanTransitOutsideService() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.now().plusYears(1);
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.TRANSIT })); 
    	Instant now = departureTime.minusSeconds(24 * 60 * 60);
    	PlannerResult result = otpDao.createPlan(now, fromPlace, toPlace, departureTime, false, modes, false, null, null, null, 1);
		assertNotNull(result);
		assertNotNull(result.getItineraries());
		assertNotNull(result.getReport());
		PlannerReport report = result.getReport(); 
		assertNotNull(report.getErrorText());
		assertEquals("NO_TRANSIT_TIMES", report.getErrorVendorCode());
		log.debug(report.getErrorText());
	}

	@Test
	public void testPlanTooClose() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.CAR })); 
    	Instant now = departureTime.minusSeconds(24 * 60 * 60);
		PlannerResult result = otpDao.createPlan(now, fromPlace, toPlace, departureTime, false, modes, false, null, null, null, 1);
		assertNotNull(result);
		assertNotNull(result.getItineraries());
		assertNotNull(result.getReport());
		PlannerReport report = result.getReport(); 
		assertNotNull(report.getErrorText());
		assertEquals("TOO_CLOSE", report.getErrorVendorCode());
		log.debug(report.getErrorText());
	}

	@Test
	public void testPlanGoToNowhere() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Het Hilgelo Waterplas::51.993629,6.715974");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK })); 
    	Instant now = departureTime.minusSeconds(24 * 60 * 60);
		PlannerResult result = otpDao.createPlan(now, fromPlace, toPlace, departureTime, false, modes, false, 10, null, null, 1);
		Leg leg = result.getItineraries().get(0).getLegs().get(0);
//        	String wkt = GeometryHelper.createWKT(leg.getLegGeometry());
//        	System.out.println(wkt);
    	int distance = leg.getDestinationStopDistance();
    	log.debug("Distance to destination is " + distance);
    	assertTrue("Remaining distance is > 100 meter", distance > 100);
	}

	@Test
	public void testPlanTransitMaxWalkDistanceLow() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK })); 
    	Instant now = departureTime.minusSeconds(24 * 60 * 60);
		PlannerResult result = otpDao.createPlan(now, fromPlace, toPlace, departureTime, false, modes, false, 100, null, null, 1);
		assertNotNull(result);
		assertNotNull(result.getItineraries());
		assertNotNull(result.getReport());
		PlannerReport report = result.getReport(); 
		assertNotNull(report.getErrorText());
		assertEquals("PATH_NOT_FOUND", report.getErrorVendorCode());
		log.debug(report.getErrorText());
	}

	@Test
	public void testPlanTransitMaxWalkDistanceHigh() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK })); 
    	Instant now = departureTime.minusSeconds(24 * 60 * 60);
		PlannerResult result = otpDao.createPlan(now, fromPlace, toPlace, departureTime, false, modes, false, 1000, null, null, 1);
		assertNotNull(result);
		assertNotNull(result.getItineraries());
		assertNotNull(result.getReport());
		assertEquals(1, result.getItineraries().size());
		Leg leg = result.getItineraries().get(0).getLegs().get(0);
    	int distance = leg.getDestinationStopDistance();
    	log.debug("Distance to destination is " + distance);
    	assertTrue("Remaining distance is < 10 meter", distance < 10);
	}

	// ****** MaxTransfers is NOT supported with the current routing algoritm! ******

//	@Test
//	public void testPlan_MaxTransfers_0() {
//    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
//    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
//    	LocalDate date = LocalDate.now();
//    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
//    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK };
//    	Integer maxTransfers = 0;
//    	try {
//    		TripPlan plan = otpDao.createPlan(fromPlace, toPlace, departureTime, false, modes, false, 1000, maxTransfers, null, 5);
//			assertNotNull(plan);
////			log.debug(String.format("maxTransfers = %d: %s", maxTransfers, plan.toString()));
//			assertEquals(0, plan.getItineraries().size());
//		} catch (NotFoundException e) {
//			fail("Did not expect " + e);
//		} catch (BadRequestException e) {
//			fail("Did not expect " + e);
//		}
//	}

	@Test
	public void testPlan_MaxTransfers_1() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK })); 
    	Integer maxTransfers = 1;
    	Instant now = departureTime.minusSeconds(24 * 60 * 60);
		PlannerResult result = otpDao.createPlan(now, fromPlace, toPlace, departureTime, false, modes, false, 1000, maxTransfers, null, 5);
		assertNotNull(result);
		assertNotNull(result.getItineraries());
		assertNotNull(result.getReport());
//			log.debug(String.format("maxTransfers = %d: %s", maxTransfers, plan.toString()));
		assertEquals(0, result.getItineraries().size());
	}

	@Test
	public void testPlan_MaxTransfers_2() {
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.now();
    	Instant departureTime = OffsetDateTime.of(date, LocalTime.parse("12:00:00"), ZoneOffset.UTC).toInstant();
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK })); 
    	Integer maxTransfers = 2;
    	Instant now = departureTime.minusSeconds(24 * 60 * 60);
		PlannerResult result = otpDao.createPlan(now, fromPlace, toPlace, departureTime, false, modes, false, 1000, maxTransfers, null, 5);
		assertNotNull(result);
		assertNotNull(result.getItineraries());
		assertNotNull(result.getReport());
//			log.debug(String.format("maxTransfers = %d: %s", maxTransfers, plan.toString()));
		assertEquals(0, result.getItineraries().size());
	}
}
