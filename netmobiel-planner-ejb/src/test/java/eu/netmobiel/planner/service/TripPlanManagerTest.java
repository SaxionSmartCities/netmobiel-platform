package eu.netmobiel.planner.service;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netmobiel.commons.event.BookingCancelledFromProviderEvent;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.event.BookingProposalRejectedEvent;
import eu.netmobiel.planner.event.TravelOfferEvent;
import eu.netmobiel.planner.filter.ShoutOutFilter;
import eu.netmobiel.planner.filter.TripPlanFilter;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.ItineraryDao;
import eu.netmobiel.planner.repository.OpenTripPlannerDao;
import eu.netmobiel.planner.repository.OtpClusterDao;
import eu.netmobiel.planner.repository.TripPlanDao;
import eu.netmobiel.planner.test.Fixture;
import eu.netmobiel.rideshare.service.RideManager;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;

@SuppressWarnings("unused")
public class TripPlanManagerTest {

	private Logger log = LoggerFactory.getLogger(TripPlanManagerTest.class);
	
	@Tested
	private TripPlanManager tested;
	
	@Injectable
	private Logger logger;
	
	@Injectable
    private Planner planner;

	@Injectable
    private TripPlanHelper tripPlanHelper;

	@Injectable
    private TripPlanDao tripPlanDao;

	@Injectable
    private OtpClusterDao otpClusterDao;
	
	@Injectable
    private ItineraryDao itineraryDao;

	@Injectable
    private OpenTripPlannerDao otpDao;

	@Injectable
    private Event<TripPlan> shoutOutRequestedEvent;

	@Injectable
    private Event<TravelOfferEvent> travelOfferEvent;

	@Injectable
    private Event<BookingCancelledFromProviderEvent> bookingCancelledEvent;

    @Injectable
    private Event<BookingProposalRejectedEvent> bookingRejectedEvent;

    @Injectable
    private Event<Leg> quoteRequestedEvent;

	@Injectable
    private RideManager rideManager;

	@Injectable
	private PlannerUser traveller;
	@Injectable
	private PlannerUser driver;
	
	@Before
	public void setUp() throws Exception {
		traveller = new PlannerUser("ID1", "Pietje", "Puk", "pietje@puk.me");
		driver = new PlannerUser("ID2", "Jan", "Chauffeur", "jan@chauffeurs.me");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testListTripPlans() {
    	Cursor cursor = new Cursor(9, 1);
    	TripPlanFilter filter = new TripPlanFilter();
    	filter.setNow(Instant.parse("2020-03-20T09:00:00Z"));
    	filter.setPlanType(PlanType.REGULAR);
    	filter.setSince(Instant.parse("2020-06-24T00:00:00Z"));
    	filter.setUntil(Instant.parse("2020-06-25T00:00:00Z"));
    	filter.setInProgress(true);
    	filter.setSortDir(SortDirection.ASC);
    	filter.setTraveller(traveller);
		new Expectations() {{
			tripPlanDao.findTripPlans(filter, Cursor.COUNTING_CURSOR);
			result = PagedResult.empty();
		}};
		try {
			filter.validate();
			tested.listTripPlans(filter, cursor);
		} catch (BusinessException ex) {
			fail("Unexpected exception: " + ex);
		}
		new Verifications() {{
			// Verify call to DAO. No results returned, so no second call.
			tripPlanDao.findTripPlans(filter, Cursor.COUNTING_CURSOR);
			times = 1;
		}};
	}

	@Test
	public void testListShoutOuts() throws BadRequestException {
		GeoLocation location = Fixture.placeCentrumDoetinchem;
		OffsetDateTime start = OffsetDateTime.parse("2020-06-24T00:00:00Z");
		Integer depArrRadius = 10000;
		Integer travelRadius = 30000;
		Integer maxResults = 9;
		Integer offset = 1;
    	ShoutOutFilter filter = new ShoutOutFilter(driver, Fixture.placeCentrumDoetinchem.toString(), depArrRadius, travelRadius, start, null, null, null);
    	Cursor cursor = new Cursor(maxResults, offset);
		new Expectations() {{
			tripPlanDao.findShoutOutPlans(filter, Cursor.COUNTING_CURSOR);
			result = PagedResult.empty();
		}};
		tested.findShoutOuts(filter, cursor);
		new Verifications() {{
			// Verify call to DAO. No results returned, so no second call.
			tripPlanDao.findShoutOutPlans(filter, Cursor.COUNTING_CURSOR);
			times = 1;
		}};
	}
	
	private static String printAsLocalDateTime(Instant instant) {
		return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(instant.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toLocalDateTime());
	}

	private void dumpTravelTime(Instant travelTime, String otherDescription, Instant otherTime) {
		log.debug(String.format("Travel time is %s, %s is %s", 
				printAsLocalDateTime(travelTime), otherDescription, printAsLocalDateTime(otherTime)));
	}

	@Test
	public void testCalculateEarliestDepartureTime() {
		LocalDate today = LocalDate.now(); 
		Instant dayStart = LocalDateTime.of(today, TripPlanManager.DAY_START).atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		
		// Case 1: Into the day
    	LocalDateTime localTravelTime = LocalDateTime.of(today, TripPlanManager.DAY_START.plusSeconds(TripPlanManager.DAY_TIME_SLACK * 60 * 60 + 60));
    	Instant travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		Instant edt = TripPlanManager.calculateEarliestDepartureTime(travelTime, false);
		dumpTravelTime(travelTime, "earliest departure", edt);
		assertEquals(travelTime.minusSeconds(TripPlanManager.DAY_TIME_SLACK * 60 * 60), edt);
		
		// Case 2: Just after DAY_START
    	localTravelTime = LocalDateTime.of(today, TripPlanManager.DAY_START.plusSeconds(60));
    	travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		edt = TripPlanManager.calculateEarliestDepartureTime(travelTime, false);
		dumpTravelTime(travelTime, "earliest departure", edt);
		assertEquals(dayStart.minusSeconds(TripPlanManager.REST_TIME_SLACK * 60 * 60), edt);
		
		// Case 3: Before DAY_START
    	localTravelTime = LocalDateTime.of(today, TripPlanManager.DAY_START.minusSeconds(30 * 60));
    	travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		edt = TripPlanManager.calculateEarliestDepartureTime(travelTime, false);
		dumpTravelTime(travelTime, "earliest departure", edt);
		assertEquals(travelTime.minusSeconds(TripPlanManager.REST_TIME_SLACK * 60 * 60), edt);

		// Case 4: After DAY_END
		localTravelTime = LocalDateTime.of(today, TripPlanManager.DAY_END.plusSeconds(30 * 60));
    	travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		edt = TripPlanManager.calculateEarliestDepartureTime(travelTime, false);
		dumpTravelTime(travelTime, "earliest departure", edt);
		assertEquals(travelTime.minusSeconds(TripPlanManager.REST_TIME_SLACK * 60 * 60), edt);

		// Case 5: Around midnight
		localTravelTime = LocalDateTime.of(today, LocalTime.parse("00:30"));
    	travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		edt = TripPlanManager.calculateEarliestDepartureTime(travelTime, false);
		dumpTravelTime(travelTime, "earliest departure", edt);
		assertEquals(travelTime.minusSeconds(TripPlanManager.REST_TIME_SLACK * 60 * 60), edt);
	}

	@Test
	public void testCalculateLatestArrivalTime() {
		LocalDate today = LocalDate.now(); 
		Instant dayEnd = LocalDateTime.of(today, TripPlanManager.DAY_END).atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		
		// Case 1: Into the day
    	LocalDateTime localTravelTime = LocalDateTime.of(today, TripPlanManager.DAY_END.minusSeconds(TripPlanManager.DAY_TIME_SLACK * 60 * 60 + 30 * 60));
    	Instant travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		Instant lat = TripPlanManager.calculateLatestArrivalTime(travelTime, true);
		dumpTravelTime(travelTime, "latest arrival", lat);
		assertEquals(travelTime.plusSeconds(TripPlanManager.DAY_TIME_SLACK * 60 * 60), lat);

		// Case 2: Just before DAY_END
    	localTravelTime = LocalDateTime.of(today, TripPlanManager.DAY_END.minusSeconds(30 * 60));
    	travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		lat = TripPlanManager.calculateLatestArrivalTime(travelTime, true);
		dumpTravelTime(travelTime, "latest arrival", lat);
		assertEquals(dayEnd.plusSeconds(TripPlanManager.REST_TIME_SLACK * 60 * 60), lat);

		// Case 3: After DAY_END
    	localTravelTime = LocalDateTime.of(today, TripPlanManager.DAY_END.plusSeconds(30 * 60));
    	travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		lat = TripPlanManager.calculateLatestArrivalTime(travelTime, true);
		dumpTravelTime(travelTime, "latest arrival", lat);
		assertEquals(travelTime.plusSeconds(TripPlanManager.REST_TIME_SLACK * 60 * 60), lat);

		// Case 4: Before DAY_START
		localTravelTime = LocalDateTime.of(today, TripPlanManager.DAY_START.minusSeconds(30 * 60));
    	travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		lat = TripPlanManager.calculateLatestArrivalTime(travelTime, true);
		dumpTravelTime(travelTime, "latest arrival", lat);
		assertEquals(travelTime.plusSeconds(TripPlanManager.REST_TIME_SLACK * 60 * 60), lat);

		// Case 5: Around midnight
		localTravelTime = LocalDateTime.of(today, LocalTime.parse("00:30"));
    	travelTime = localTravelTime.atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toInstant();
		lat = TripPlanManager.calculateLatestArrivalTime(travelTime, true);
		dumpTravelTime(travelTime, "latest arrival", lat);
		assertEquals(travelTime.plusSeconds(TripPlanManager.REST_TIME_SLACK * 60 * 60), lat);

	}
	
	@Test
	public void testSanitizePlanInput() {
		TripPlan plan = new TripPlan();
		GeoLocation from = Fixture.placeHengeloStation;
		GeoLocation to = Fixture.placeRaboZutphen;
		plan.setRequestTime(Instant.parse("2020-07-02T18:00:00Z"));
		plan.setFrom(from);
		plan.setTo(to); 
		plan.setTravelTime(plan.getRequestTime());
		try {
			TripPlanManager.sanitizePlanInput(plan);
		} catch (BadRequestException ex) {
			log.debug("Anticipated exception: " + ex);
		}
		
		plan.setRequestTime(Instant.parse("2020-07-02T18:00:00Z"));
		plan.setTravelTime(plan.getRequestTime().minusSeconds(1));
		try {
			TripPlanManager.sanitizePlanInput(plan);
			fail("Expected BadRequestException because travel time is before now");
		} catch (BadRequestException ex) {
			log.debug("Anticipated exception: " + ex);
		}
		
		plan.setRequestTime(Instant.parse("2020-07-02T18:00:00Z"));
		plan.setTravelTime(null);
		plan.setEarliestDepartureTime(plan.getRequestTime());
		try {
			TripPlanManager.sanitizePlanInput(plan);
		} catch (BadRequestException ex) {
			fail("Unexpected exception: " + ex);
		}

		plan.setRequestTime(Instant.parse("2020-07-02T18:00:00Z"));
		plan.setTravelTime(null);
		plan.setEarliestDepartureTime(plan.getRequestTime().minusSeconds(1));
		try {
			TripPlanManager.sanitizePlanInput(plan);
			fail("Expected BadRequestException because earliest departure time is before now");
		} catch (BadRequestException ex) {
			log.debug("Anticipated exception: " + ex);
		}

		plan.setRequestTime(Instant.parse("2020-07-02T18:00:00Z"));
		plan.setTravelTime(Instant.parse("2020-07-03T12:00:00Z"));
		plan.setEarliestDepartureTime(plan.getTravelTime().plusSeconds(3600));
		try {
			TripPlanManager.sanitizePlanInput(plan);
			fail("Expected BadRequestException because earliest departure is after travel time");
		} catch (BadRequestException ex) {
			log.debug("Anticipated exception: " + ex);
		}

		plan.setRequestTime(Instant.parse("2020-07-02T18:00:00Z"));
		plan.setTravelTime(Instant.parse("2020-07-03T12:00:00Z"));
		plan.setEarliestDepartureTime(null);
		plan.setLatestArrivalTime(plan.getTravelTime().minusSeconds(3600));
		try {
			TripPlanManager.sanitizePlanInput(plan);
			fail("Expected BadRequestException because latest arrival is before travel time");
		} catch (BadRequestException ex) {
			log.debug("Anticipated exception: " + ex);
		}

		// Complete sane usage
		plan.setRequestTime(Instant.parse("2020-07-02T18:00:00Z"));
		plan.setTravelTime(Instant.parse("2020-07-03T12:00:00Z"));
		plan.setEarliestDepartureTime(Instant.parse("2020-07-03T09:00:00Z"));
		plan.setLatestArrivalTime(Instant.parse("2020-07-03T18:00:00Z"));
		try {
			TripPlanManager.sanitizePlanInput(plan);
		} catch (BadRequestException ex) {
			fail("Unexpected exception: " + ex);
		}

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testResolveShoutOut() {
		Long pid = 54L;
		String shoutOutRef = UrnHelper.createUrn(TripPlan.URN_PREFIX, pid);
		Instant now = Instant.parse("2020-07-05T14:00:00Z"); 
		TripPlan shoutOutPlan = Fixture.createShoutOutTripPlan(traveller, "2020-07-04T18:00:00Z", 
				Fixture.placeZieuwent, Fixture.placeRaboZutphen, "2020-07-06T10:00:00Z", false, null);
		TripPlan driverPlan = new TripPlan();
		driverPlan.setFrom(Fixture.placeThuisLichtenvoorde);
		new Expectations() {{
			tripPlanDao.find(pid);
			result = shoutOutPlan;
			List<GeoLocation> via = Arrays.asList(new GeoLocation[] { shoutOutPlan.getFrom(), shoutOutPlan.getTo() });
			otpDao.createPlan((Instant)any, driverPlan.getFrom(), shoutOutPlan.getTo(), shoutOutPlan.getTravelTime(), shoutOutPlan.isUseAsArrivalTime(), 
					(Set<TraverseMode>) any, false, shoutOutPlan.getMaxWalkDistance(), null, via, 1);
			result = Fixture.createShoutOutSolution(driverPlan.getFrom(), null, shoutOutPlan); 
		}};
		try {
			TripPlan plan = tested.planShoutOutSolution(now, driver, shoutOutRef, driverPlan, TraverseMode.RIDESHARE);
			assertEquals(1, plan.getItineraries().size());
			Itinerary it = plan.getItineraries().iterator().next();
			log.debug("Itinerary: " + it.toString());
		} catch (BusinessException e) {
			fail("Unexpected exception: " + e);
		}
	}
}
