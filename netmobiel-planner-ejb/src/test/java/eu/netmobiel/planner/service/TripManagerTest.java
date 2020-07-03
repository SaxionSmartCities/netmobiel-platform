package eu.netmobiel.planner.service;

import static org.junit.Assert.*;

import java.time.Instant;

import javax.enterprise.event.Event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.model.event.BookingCancelledEvent;
import eu.netmobiel.commons.model.event.BookingRequestedEvent;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.ItineraryDao;
import eu.netmobiel.planner.repository.TripDao;
import eu.netmobiel.planner.test.Fixture;
import eu.netmobiel.planner.util.PlannerUrnHelper;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;

public class TripManagerTest {
	
	private Logger log = LoggerFactory.getLogger(TripManagerTest.class);
	
	@Tested
	private TripManager tested;
	@Injectable
	private Logger logger;
	@Injectable
    private TripDao tripDao;

	@Injectable
    private ItineraryDao itineraryDao;
    
	@Injectable
    private Event<BookingRequestedEvent> bookingRequestedEvent;

	@Injectable
    private Event<BookingCancelledEvent> bookingCancelledEvent;

	private User traveller;
	
	@Before
	public void setUp() throws Exception {
		traveller = new User("ID1", "Pietje", "Puk", "pietje@puk.me");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testListTrips() {
		TripState state = TripState.SCHEDULED;
		Instant since = Instant.parse("2020-06-24T00:00:00Z");
		Instant until = Instant.parse("2020-06-25T00:00:00Z");
		Boolean deletedToo = true;
		SortDirection sortDir = SortDirection.ASC;
		Integer maxResults = 9;
		Integer offset = 1;
		new Expectations() {{
			tripDao.findTrips(traveller, state, since, until, deletedToo, sortDir, 0, 0);
			result = PagedResult.empty();
		}};
		try {
			tested.listTrips(traveller, state, since, until, deletedToo, sortDir, maxResults, offset);
		} catch (ApplicationException ex) {
			fail("Unexpected exception: " + ex);
		}
		new Verifications() {{
			// Verify call to DAO. No results returned, so no second call.
			tripDao.findTrips(traveller, state, since, until, deletedToo, sortDir, 0, 0);
			times = 1;
//			tripDao.findTrips(traveller, state, since, until, deletedToo, sortDir, maxResults, offset);
//			times = 1;
		}};
	}

	@Test
	public void testCreateTrip_NoItinerary() {
		boolean autobook = true;
		Trip input = new Trip();
		try {
			tested.createTrip(traveller, input, autobook);
			fail("Expected exception: BadRequest");
		} catch (NotFoundException ex) {
			fail("Unexpected exception: " + ex);
		} catch (BadRequestException ex) {
			log.debug("Anticipated exception: " + ex);
		}
	}

	@Test
	public void testCreateTrip_ItineraryNotFound() {
		boolean autobook = true;
		Trip trip = new Trip(PlannerUrnHelper.createUrn(Itinerary.URN_PREFIX, 23L));
		new Expectations() {{
		}};
		
		try {
			tested.createTrip(traveller, trip, autobook);
			fail("Expected exception: NotFound");
		} catch (NotFoundException ex) {
			log.debug("Anticipated exception: " + ex);
		} catch (BadRequestException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

	@Test
	public void testCreateTrip_NonBookable() {
		boolean autobook = true;
		Long itineraryId = 23L;
		Long tripId = 55L;
		TripPlan plan = Fixture.createTransitPlan(traveller);
		plan.getItineraries().get(0).setId(itineraryId);
		Trip trip = new Trip(PlannerUrnHelper.createUrn(Itinerary.URN_PREFIX, itineraryId));
		trip.setId(tripId);
		new Expectations() {{
			itineraryDao.find(itineraryId);
			result = plan.getItineraries().get(0);
			tripDao.save(trip);
		}};
		try {
			Long id = tested.createTrip(traveller, trip, autobook);
			assertEquals(tripId, id);
			assertEquals(itineraryId, trip.getItinerary().getId());
			assertEquals(TripState.SCHEDULED, trip.getState());
			assertEquals(plan.getFrom(), trip.getFrom());
			assertEquals(plan.getNrSeats(), trip.getNrSeats());
			assertEquals(plan.getTo(), trip.getTo());
			assertEquals(plan.getTraveller(), trip.getTraveller());
		} catch (ApplicationException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

	@Test
	public void testCreateTrip_Bookable() {
		boolean autobook = true;
		Long itineraryId = 23L;
		Long tripId = 55L;
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		plan.getItineraries().get(0).setId(itineraryId);
		Trip trip = new Trip(PlannerUrnHelper.createUrn(Itinerary.URN_PREFIX, itineraryId));
		assertTrue(plan.getItineraries().get(0).getLegs().get(0).isBookingRequired());
		trip.setId(tripId);
		new Expectations() {{
			itineraryDao.find(itineraryId);
			result = plan.getItineraries().get(0);
			tripDao.save(trip);
		}};
		try {
			Long id = tested.createTrip(traveller, trip, autobook);
			assertEquals(tripId, id);
			assertEquals(itineraryId, trip.getItinerary().getId());
			assertEquals(TripState.BOOKING, trip.getState());
			assertEquals(plan.getFrom(), trip.getFrom());
			assertEquals(plan.getNrSeats(), trip.getNrSeats());
			assertEquals(plan.getTo(), trip.getTo());
			assertEquals(plan.getTraveller(), trip.getTraveller());
		} catch (ApplicationException ex) {
			fail("Unexpected exception: " + ex);
		}
		new Verifications() {{
			Leg leg = trip.getItinerary().getLegs().get(0);
			BookingRequestedEvent event;
			bookingRequestedEvent.fire(event = withCapture());
			assertEquals(leg.getTo().getArrivalTime(), event.getArrivalTime());
			assertEquals(leg.getFrom().getDepartureTime(), event.getDepartureTime());
			assertEquals(leg.getTo().getLocation(), event.getDropOff());
			assertEquals(leg.getFrom().getLocation(), event.getPickup());
			assertEquals(trip.getNrSeats(), event.getNrSeats());
			assertEquals(trip.getTraveller(), event.getTraveller());
			// The transport provider trip reference
			assertEquals(leg.getTripId(), event.getProviderTripRef());
			// The netmobiel trip reference
			assertEquals(trip.getTripRef(), event.getTravellerTripRef());
		}};
	}

	@Test
	public void testAssignBookingReference() {
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, 
				Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		Trip trip = Fixture.createTrip(traveller, plan);
		trip.setId(55L);
		trip.setState(TripState.BOOKING);
		Leg leg = trip.getItinerary().getLegs().get(0);
		leg.setState(TripState.BOOKING);
		assertNull(leg.getBookingId());
		assertNotNull(leg.getTripId());
		String bookingRef = UrnHelper.createUrn("urn:nb:myservice:booking", 42L);
		new Expectations() {{
			tripDao.find(trip.getId());
			result = trip;
		}};
		try {
			tested.assignBookingReference(trip.getTripRef(), leg.getTripId(), bookingRef, true);
		} catch (UpdateException ex) {
			fail("Unexpected exception: " + ex);
		}
		assertEquals(bookingRef, leg.getBookingId());
		assertEquals(TripState.SCHEDULED, trip.getState());
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
		String bookingRef = UrnHelper.createUrn("urn:nb:myservice:booking", 42L);
		new Expectations() {{
			tripDao.find(trip.getId());
			result = trip;
		}};
		try {
			tested.assignBookingReference(trip.getTripRef(), leg.getTripId(), bookingRef, true);
			fail("Expected UpdateException because of invalid state");
		} catch (UpdateException ex) {
			log.debug("Anticipated exception: " + ex);
		}
	}

	@Test
	public void testCancelBooking() {
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		Trip trip = Fixture.createTrip(traveller, plan);
		trip.setId(55L);
		Leg leg = trip.getItinerary().getLegs().get(0);
		String bookingRef = UrnHelper.createUrn("urn:nb:myservice:booking", 42L);
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
			assertEquals(TripState.CANCELLED, trip.getState());
			assertEquals(TripState.CANCELLED, leg.getState());
		} catch (NotFoundException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

	@Test
	public void testGetTrip_Found() {
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		Trip trip = Fixture.createTrip(traveller, plan);
		trip.setId(55L);
		new Expectations() {{
			tripDao.find(trip.getId());
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
			tested.removeTrip(34L, null);
			fail("Expected exception: NotFound");
		} catch (NotFoundException ex) {
			log.debug("Anticipated exception: " + ex);
		}
	}

	@Test
	public void testRemoveTrip_Booked() {
		TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		Trip trip = Fixture.createTrip(traveller, plan);
		trip.setId(55L);
		Leg leg = trip.getItinerary().getLegs().get(0);
		leg.setState(TripState.SCHEDULED);
		trip.updateTripState();
		String bookingRef = UrnHelper.createUrn("urn:nb:myservice:booking", 42L);
		leg.setBookingId(bookingRef);
		String reason = "Ik wil niet meer";
		new Expectations() {{
			tripDao.find(trip.getId());
			result = trip;
		}};
		try {
			tested.removeTrip(trip.getId(), reason);
		} catch (NotFoundException ex) {
			fail("Unexpected exception: " + ex);
		}
		assertTrue(trip.isDeleted());
		assertEquals(TripState.CANCELLED, trip.getState());
		assertEquals(TripState.CANCELLED, leg.getState());
		new Verifications() {{
			BookingCancelledEvent event;
			bookingCancelledEvent.fire(event = withCapture());
			assertEquals(reason, event.getCancelReason());
			assertEquals(bookingRef, event.getBookingRef());
			assertEquals(traveller, event.getTraveller());
			assertEquals(trip.getTripRef(), event.getTravellerTripRef());
			assertEquals(false, event.isCancelledByDriver());
			assertEquals(false, event.isCancelledFromTransportProvider());
		}};
	}
}
