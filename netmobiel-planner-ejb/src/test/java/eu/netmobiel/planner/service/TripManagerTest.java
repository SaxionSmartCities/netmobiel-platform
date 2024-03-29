package eu.netmobiel.planner.service;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Optional;

import javax.ejb.SessionContext;
import javax.ejb.TimerService;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netmobiel.commons.event.TripValidationEvent;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.ClockDao;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.here.search.HereSearchClient;
import eu.netmobiel.planner.event.BookingAssignedEvent;
import eu.netmobiel.planner.event.BookingCancelledEvent;
import eu.netmobiel.planner.event.BookingConfirmedEvent;
import eu.netmobiel.planner.event.BookingRequestedEvent;
import eu.netmobiel.planner.event.ShoutOutResolvedEvent;
import eu.netmobiel.planner.event.TripConfirmedEvent;
import eu.netmobiel.planner.event.TripStateUpdatedEvent;
import eu.netmobiel.planner.event.TripUnconfirmedEvent;
import eu.netmobiel.planner.filter.TripFilter;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.repository.ItineraryDao;
import eu.netmobiel.planner.repository.LegDao;
import eu.netmobiel.planner.repository.TripDao;
import eu.netmobiel.planner.repository.TripPlanDao;
import eu.netmobiel.planner.test.Fixture;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;

@SuppressWarnings("unused")
public class TripManagerTest {
	
	private Logger log = LoggerFactory.getLogger(TripManagerTest.class);
	
	@Tested
	private TripManager tested;
	@Injectable
	private Logger logger;
	@Injectable
    private ClockDao clockDao;
	@Injectable
    private TripDao tripDao;
	@Injectable
    private TripPlanDao tripPlanDao;

	@Injectable
    private LegDao legDao;

	@Injectable
    private ItineraryDao itineraryDao;
	
	@Injectable
	private TripMonitor tripMonitor;
	
	@Injectable
    private Event<BookingRequestedEvent> bookingRequestedEvent;

    @Injectable
    private Event<BookingCancelledEvent> bookingCancelledEvent;

	@Injectable
    private Event<BookingConfirmedEvent> bookingConfirmedEvent;

    @Injectable
    private Event<ShoutOutResolvedEvent> shoutOutResolvedEvent;

    @Injectable
    private Event<TripValidationEvent> tripValidationEvent;

    @Injectable
    private Event<TripConfirmedEvent> tripConfirmedEvent;

    @Injectable
    private Event<TripUnconfirmedEvent> tripUnconfirmedEvent;

    @Injectable
    private Event<BookingAssignedEvent> bookingAssignedEvent;

    @Injectable
    private SessionContext context;

    @Injectable
    private TimerService timerService;

    @Injectable
	private HereSearchClient hereSearchClient;
    
    private PlannerUser traveller;
	
	@Before
	public void setUp() throws Exception {
		traveller = new PlannerUser("ID1", "Pietje", "Puk", "pietje@puk.me");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testListTrips() throws BadRequestException {
    	TripFilter filter = new TripFilter();
    	Cursor cursor = new Cursor();
    	filter.setTripState(TripState.SCHEDULED);
    	filter.setSince(Instant.parse("2020-06-24T00:00:00Z"));
    	filter.setUntil(Instant.parse("2020-06-25T00:00:00Z"));
    	filter.setDeletedToo(true);
    	filter.setSortDir(SortDirection.ASC);
    	filter.validate();
    	cursor.validate(9,  1);
		new Expectations() {{
			tripDao.findTrips(filter, Cursor.COUNTING_CURSOR);
			result = PagedResult.empty();
		}};
		try {
			tested.listTrips(filter, cursor);
		} catch (BusinessException ex) {
			fail("Unexpected exception: " + ex);
		}
		new Verifications() {{
			// Verify call to DAO. No results returned, so no second call.
			tripDao.findTrips(filter, Cursor.COUNTING_CURSOR);
			times = 1;
//			tripDao.findTrips(traveller, state, since, until, deletedToo, sortDir, maxResults, offset);
//			times = 1;
		}};
	}

	@Test
	public void testCreateTrip_NoItinerary() {
		Trip input = new Trip();
		try {
			tested.createTrip(traveller, traveller, input);
			fail("Expected exception: BadRequest");
		} catch (BadRequestException ex) {
			log.debug("Anticipated exception: " + ex);
		} catch (BusinessException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

	@Test
	public void testCreateTrip_ItineraryNotFound() {
		Trip trip = new Trip(UrnHelper.createUrn(Itinerary.URN_PREFIX, 23L));
		new Expectations() {{
		}};
		
		try {
			tested.createTrip(traveller, traveller, trip);
			fail("Expected exception: NotFound");
		} catch (NotFoundException ex) {
			log.debug("Anticipated exception: " + ex);
		} catch (BusinessException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

//	@Test
//	public void testCreateTrip_NonBookable() {
//		Long itineraryId = 23L;
//		Long tripId = 55L;
//		TripPlan plan = Fixture.createTransitPlan(traveller);
//		plan.getItineraries().iterator().next().setId(itineraryId);
//		Trip trip = new Trip(UrnHelper.createUrn(Itinerary.URN_PREFIX, itineraryId));
//		trip.setId(tripId);
//		new Expectations() {{
//			itineraryDao.loadGraph(itineraryId, Itinerary.LIST_ITINERARIES_ENTITY_GRAPH);
//			result = Optional.of(plan.getItineraries().iterator().next());
//			tripDao.save(trip);
//			result = trip;
////			clockDao.now();
//			result = plan.getRequestTime();
//		}};
//		try {
//			Long id = tested.createTrip(traveller, traveller, trip);
//			assertEquals(tripId, id);
//			assertEquals(itineraryId, trip.getItinerary().getId());
////			assertEquals(TripState.SCHEDULED, trip.getState());
//			assertEquals(plan.getFrom(), trip.getFrom());
//			assertEquals(plan.getNrSeats(), trip.getNrSeats());
//			assertEquals(plan.getTo(), trip.getTo());
//			assertEquals(plan.getTraveller(), trip.getTraveller());
//		} catch (BusinessException ex) {
//			fail("Unexpected exception: " + ex);
//		}
//	}

	@Test
	public void testCreateTrip_Bookable() {
		Long itineraryId = 23L;
		Long tripId = 55L;
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, 
				Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		plan.getItineraries().iterator().next().setId(itineraryId);
		Trip trip = new Trip(UrnHelper.createUrn(Itinerary.URN_PREFIX, itineraryId));
		assertTrue(plan.getItineraries().iterator().next().getLegs().get(0).isBookingRequired());
		trip.setId(tripId);
		new Expectations() {{
			itineraryDao.loadGraph(itineraryId, Itinerary.LIST_ITINERARIES_ENTITY_GRAPH);
			result = Optional.of(plan.getItineraries().iterator().next());
			tripDao.save(trip);
			result = trip;
			tripDao.flush();
			tripDao.loadGraph(tripId, Trip.DETAILED_ENTITY_GRAPH);
			result = Optional.of(trip);
		}};
		try {
			Long id = tested.createTrip(traveller, traveller, trip);
			assertEquals(tripId, id);
			assertEquals(itineraryId, trip.getItinerary().getId());
			Leg leg = trip.getItinerary().getLegs().get(0);
//			assertEquals(TripState.BOOKING, leg.getState());
			assertEquals(plan.getFrom(), trip.getFrom());
			assertEquals(plan.getNrSeats(), trip.getNrSeats());
			assertEquals(plan.getTo(), trip.getTo());
			assertEquals(plan.getTraveller(), trip.getTraveller());
		} catch (BusinessException ex) {
			log.debug("Exception on create bookable trip", ex);
			fail("Unexpected exception: " + ex);
		}
//		new Verifications() {{
//			Leg leg = trip.getItinerary().getLegs().get(0);
//			BookingRequestedEvent event;
//			bookingRequestedEvent.fire(event = withCapture());
//			assertSame(leg, event.getLeg());
//			assertEquals(trip, event.getTrip());
//		}};
	}

	@Test
	public void testAssignBookingReference() {
		Instant now = Instant.now();
		Instant travelTime = now.plusSeconds(4 * 3600);
		TripPlan plan = Fixture.createRidesharePlan(traveller, now.toString(), Fixture.placeZieuwent, 
				Fixture.placeSlingeland, travelTime.toString(), false, 60 * 35, "urn:nb:rs:ride:364");
		Trip trip = Fixture.createTrip(traveller, plan);
		trip.setId(55L);
		trip.setState(TripState.BOOKING);
		String bookingRef = UrnHelper.createUrn("urn:nb:myservice:booking:", 42L);
		new Expectations() {{
			tripDao.find(trip.getId());
			result = trip;
		}};
		try {
//			trip.setState(TripState.BOOKING);
			Leg leg = trip.getItinerary().getLegs().get(0);
//			leg.setState(TripState.BOOKING);
			assertNull(leg.getBookingId());
			assertNotNull(leg.getTripId());
			tested.assignBookingReference(trip.getTripRef(), leg.getTripId(), bookingRef, true);
			assertEquals(bookingRef, leg.getBookingId());
		} catch (BusinessException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

	@Test
	public void testAssignBookingReference_CancelledInBetween() {
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, 
				Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		Trip trip = Fixture.createTrip(traveller, plan);
		trip.setId(55L);
		trip.setState(TripState.CANCELLED);
		Leg leg = trip.getItinerary().getLegs().get(0);
		leg.setState(TripState.CANCELLED);
		assertNull(leg.getBookingId());
		assertNotNull(leg.getTripId());
		String bookingRef = UrnHelper.createUrn("urn:nb:myservice:booking:", 42L);
		new Expectations() {{
			tripDao.find(trip.getId());
			result = trip;
		}};
		try {
			tested.assignBookingReference(trip.getTripRef(), leg.getTripId(), bookingRef, true);
			fail("Expected UpdateException because of invalid state");
		} catch (BusinessException ex) {
			log.debug("Anticipated exception: " + ex);
		}
	}

	@Test
	public void testCancelBooking() {
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		Trip trip = Fixture.createTrip(traveller, plan);
		trip.setId(55L);
		Leg leg = trip.getItinerary().getLegs().get(0);
		String bookingRef = UrnHelper.createUrn("urn:nb:myservice:booking:", 42L);
		leg.setBookingId(bookingRef);
		assertNotEquals(TripState.CANCELLED, trip.getState());
		new Expectations() {{
			tripDao.find(trip.getId());
			result = trip;
		}};
		String reason = "verkeerde dag gekozen";
		boolean cancelledByDriver = false;
		try {
			tested.cancelBooking(trip.getTripRef(), bookingRef, reason, cancelledByDriver);
			assertEquals(TripState.CANCELLED, leg.getState());
			assertEquals(Boolean.FALSE, leg.getCancelledByProvider());
			assertEquals(Boolean.FALSE, trip.getCancelledByProvider());
			assertEquals(reason,  trip.getCancelReason());
		} catch (BusinessException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

	@Test
	public void testGetTrip_Found() {
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		Trip trip = Fixture.createTrip(traveller, plan);
		trip.setId(55L);
		new Expectations() {{
			tripDao.loadGraph(trip.getId(), Trip.DETAILED_ENTITY_GRAPH);
			result = trip;
		}};
		try {
			Trip tripdb = tested.getTrip(trip.getId());
			assertNotNull(tripdb);
			assertEquals(trip, tripdb);
		} catch (NotFoundException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

	@Test
	public void testGetTrip_NotFound() {
		try {
			tested.getTrip(34L);
			fail("Expected exception: NotFound");
		} catch (NotFoundException ex) {
			log.debug("Anticipated exception: " + ex);
		}
	}
	@Test
	public void testRemoveTrip_NotFound() {
		try {
			tested.removeTrip(34L, null, true);
			fail("Expected exception: NotFound");
		} catch (NotFoundException ex) {
			log.debug("Anticipated exception: " + ex);
		} catch (BusinessException ex) {
			log.debug("Unexpected exception: " + ex);
		}
	}

	@Test
	public void testRemoveTrip_Booked() {
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		Trip trip = Fixture.createTrip(traveller, plan);
		trip.setId(55L);
		Leg leg = trip.getItinerary().getLegs().get(0);
		leg.setState(TripState.SCHEDULED);
//		trip.deriveTripState();
		String bookingRef = UrnHelper.createUrn("urn:nb:myservice:booking:", 42L);
		leg.setBookingId(bookingRef);
		String reason = "Ik wil niet meer";
		new Expectations() {{
			tripDao.find(trip.getId());
			result = trip;
		}};
		try {
			tested.removeTrip(trip.getId(), reason, true);
		} catch (BusinessException ex) {
			fail("Unexpected exception: " + ex);
		}
		assertTrue(trip.isDeleted());
		assertEquals(TripState.CANCELLED, leg.getState());
		new Verifications() {{
			BookingCancelledEvent event;
			bookingCancelledEvent.fire(event = withCapture());
			assertEquals(reason, event.getCancelReason());
			assertSame(leg, event.getLeg());
			assertSame(trip, event.getTrip());
		}};
	}
}
