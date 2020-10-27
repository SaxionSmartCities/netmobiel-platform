package eu.netmobiel.rideshare.service;


import static org.junit.Assert.*;

import java.time.Instant;

import javax.inject.Inject;
import javax.persistence.PersistenceUnitUtil;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.User_;
import eu.netmobiel.rideshare.event.BookingSettledEvent;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Booking_;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Leg_;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.Ride_;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.LegDao;
import eu.netmobiel.rideshare.repository.OpenTripPlannerDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.StopDao;
import eu.netmobiel.rideshare.repository.RideshareUserDao;
import eu.netmobiel.rideshare.repository.mapping.LegMapper;
import eu.netmobiel.rideshare.test.Fixture;
import eu.netmobiel.rideshare.test.RideshareIntegrationTestBase;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@RunWith(Arquillian.class)
public class BookingManagerIT extends RideshareIntegrationTestBase {
	
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
	            .addClass(BookingDao.class)
	            .addClass(RideDao.class)
	            .addClass(RideshareUserDao.class)
	            .addClass(LegDao.class)
	            .addClass(StopDao.class)
	            .addClass(OpenTripPlannerDao.class)
	            .addPackage(LegMapper.class.getPackage())
	            .addPackage(BookingSettledEvent.class.getPackage())
	            .addClass(EventListenerHelper.class)
	            .addClass(RideItineraryHelper.class)
	            .addClass(BookingManager.class);
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private RideItineraryHelper rideItineraryHelper;
    @Inject
    private BookingManager bookingManager;
    
    @Inject
    private EventListenerHelper eventListenerHelper;

    private RideshareUser driver1;
    private Car car1;
    private RideshareUser passenger1;

    @Override
    public boolean isSecurityRequired() {
    	return true;
    }

    @Override
    protected void insertData() throws Exception {
        driver1 = Fixture.createUser(loginContextDriver);
		em.persist(driver1);

		car1 = Fixture.createCarFordThunderbird(driver1);
		em.persist(car1);

		passenger1 = Fixture.createUser(loginContextPassenger);
		em.persist(passenger1);
	
		eventListenerHelper.reset();
    }


    @Test
    public void createAutoConfirmedBooking() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
		Long rideId = r.getId();
		assertNotNull(rideId);
		flush();
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime(), "my-trip");
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
		assertNotNull(bookingRef);
		flush();
		Booking b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertNotNull(b.getRide());
		assertNotNull(b.getPassenger());
		assertNotNull(b.getLegs());
		assertEquals(0, b.getLegs().size());
		// Auto confirm
		assertEquals(BookingState.CONFIRMED, b.getState());
		// The hook for the driver notification is called
		assertNotNull(eventListenerHelper.getLastBookingCreatedEvent());
		assertEquals(1, eventListenerHelper.getBookingCreatedEventCount());
		// The itinerary is stale
		assertNotNull(eventListenerHelper.getLastRideItineraryStaleEvent());
		assertEquals(1, eventListenerHelper.getRideItineraryStaleEventCount());
    }		

    @Test
    public void createBooking_Multiple() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
		Long rideId = r.getId();
		assertNotNull(rideId);
		flush();
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime(), "trip-1");
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
		assertNotNull(bookingRef);
		flush();
		try {
			expectFailure();
			Booking booking2 = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime(), "trip-2");
			bookingManager.createBooking(r.getRideRef(), passenger1, booking2);
			fail("Expected exception");
		} catch (CreateException ex) {
			
		}
    }		

    @Test
    public void getBookingDetail() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime(), "trip-1");
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
    	flush();
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", r.getId())
				.getSingleResult();
		rideItineraryHelper.updateRideItinerary(rdb);
    	flush();
    	Booking but = bookingManager.getBooking(RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef));
    	flush();

    	// Now test the presence of the required fields
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertFalse(em.contains(but));
    	assertNotNull(but);
    	assertNotNull(but.getArrivalTime());
    	assertNotNull(but.getBookingRef());
    	assertNull(but.getCancelledByDriver());
    	assertNull(but.getCancelReason());
    	assertNotNull(but.getDepartureTime());
    	assertNotNull(but.getDropOff());
    	assertNotNull(but.getNrSeats());
    	assertNotNull(but.getPickup());
    	assertNotNull(but.getState());
    	assertTrue(puu.isLoaded(but, Booking_.LEGS));
    	assertTrue(puu.isLoaded(but, Booking_.PASSENGER));
    	assertTrue(puu.isLoaded(but.getPassenger(), User_.ID));
    	assertTrue(puu.isLoaded(but.getPassenger(), User_.GIVEN_NAME));
    	assertTrue(puu.isLoaded(but.getPassenger(), User_.FAMILY_NAME));
    	assertTrue(puu.isLoaded(but.getPassenger(), User_.MANAGED_IDENTITY));
    	assertTrue(puu.isLoaded(but, Booking_.RIDE));
    	Ride ride = but.getRide();
    	assertTrue(puu.isLoaded(ride, Ride_.ID));
    	assertTrue(puu.isLoaded(ride, Ride_.DRIVER));
    	assertTrue(puu.isLoaded(ride.getDriver(), User_.ID));
    	assertTrue(puu.isLoaded(ride.getDriver(), User_.GIVEN_NAME));
    	assertTrue(puu.isLoaded(ride.getDriver(), User_.FAMILY_NAME));
    	assertTrue(puu.isLoaded(ride.getDriver(), User_.MANAGED_IDENTITY));
    	
    	assertEquals(1, but.getLegs().size());
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.ID)));
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.FROM)));
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.TO)));
    	
    	// The following should not be there, but they are.
//    	assertFalse(puu.isLoaded(but.getRide(), Ride_.ARRIVAL_TIME));
//    	assertFalse(puu.isLoaded(but.getRide(), Ride_.LEGS));
    }

    private String prepareSimpleBooking() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
		Long rideId = r.getId();
		assertNotNull(rideId);
		flush();
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime(), "trip-1");
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
		assertNotNull(bookingRef);
		flush();
		Booking b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertFalse(b.isDeleted());
		flush();
		// Reset all the counters.
		eventListenerHelper.reset();
    	return b.getBookingRef();
    }
    
    @Test
    public void removeBookingByPassengerFromRideshare() throws Exception {
    	String bookingRef = prepareSimpleBooking();
		String reason = "Afspraak is verplaatst";
		
		bookingManager.removeBooking(bookingRef, reason, false, true);
		flush();
		Booking b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertTrue(b.isDeleted());
		assertFalse(Boolean.TRUE.equals(b.getCancelledByDriver()));
		assertEquals(reason, b.getCancelReason());
		
		assertEquals(1, eventListenerHelper.getBookingCancelledEventCount());
		assertEquals(1, eventListenerHelper.getBookingRemovedEventCount());
		assertEquals(1, eventListenerHelper.getRideItineraryStaleEventCount());
    }

    @Test
    public void removeBookingByPassengerNotFromRideshare() throws Exception {
    	String bookingRef = prepareSimpleBooking();
		String reason = "Afspraak is verplaatst";
		
		bookingManager.removeBooking(bookingRef, reason, false, false);
		flush();
		Booking b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertTrue(b.isDeleted());
		assertFalse(Boolean.TRUE.equals(b.getCancelledByDriver()));
		assertEquals(reason, b.getCancelReason());
		
		assertEquals(0, eventListenerHelper.getBookingCancelledEventCount());
		assertEquals(1, eventListenerHelper.getBookingRemovedEventCount());
		assertEquals(1, eventListenerHelper.getRideItineraryStaleEventCount());
    }

    @Test
    public void removeBookingByDriverFromRideshare() throws Exception {
    	String bookingRef = prepareSimpleBooking();
		String reason = "Ik rij niet meer, ik hoef er niet meer te zijn";
		
		bookingManager.removeBooking(bookingRef, reason, true, true);
		flush();
		Booking b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertTrue(b.isDeleted());
		assertTrue(Boolean.TRUE.equals(b.getCancelledByDriver()));
		assertEquals(reason, b.getCancelReason());
		
		assertEquals(1, eventListenerHelper.getBookingCancelledEventCount());
		assertEquals(0, eventListenerHelper.getBookingRemovedEventCount());
		assertEquals(1, eventListenerHelper.getRideItineraryStaleEventCount());
    }

    @Test
    public void listBookings() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
		Long rideId = r.getId();
		assertNotNull(rideId);
		flush();
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime(), "trip-1");
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
		assertNotNull(bookingRef);
		flush();
		flush();
		Booking b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertNotNull(b.getRide());
		assertNotNull(rideItineraryHelper);
    	rideItineraryHelper.updateRideItinerary(b.getRide());
		flush();
		
		PagedResult<Booking> page = bookingManager.listBookings(passenger1.getId(), null, null, 10, 0);
		flush();
		assertNotNull(page);
		assertEquals(1, page.getTotalCount().intValue());
		
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	Booking but = page.getData().get(0);
    	assertFalse(em.contains(but));
    	assertNotNull(but);
    	assertTrue(puu.isLoaded(but, Booking_.PASSENGER));
    	assertTrue(puu.isLoaded(but.getPassenger(), User_.ID));
    	assertTrue(puu.isLoaded(but, Booking_.RIDE));
    	assertTrue(puu.isLoaded(but.getRide(), Ride_.ID));
    	assertTrue(puu.isLoaded(but, Booking_.LEGS));
    	assertEquals(1, but.getLegs().size());
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.ID)));
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.FROM)));
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.TO)));

    	// Since
		page = bookingManager.listBookings(passenger1.getId(), departureTime.minusSeconds(60), null, 0, 0);
		assertNotNull(page);
		assertEquals(1, page.getTotalCount().intValue());

		page = bookingManager.listBookings(passenger1.getId(), departureTime.plusSeconds(60), null, 0, 0);
		assertNotNull(page);
		assertEquals(0, page.getTotalCount().intValue());

		// until
		page = bookingManager.listBookings(passenger1.getId(), null, departureTime.minusSeconds(60), 0, 0);
		assertNotNull(page);
		assertEquals(0, page.getTotalCount().intValue());

		page = bookingManager.listBookings(passenger1.getId(), null, departureTime.plusSeconds(60), 0, 0);
		assertNotNull(page);
		assertEquals(1, page.getTotalCount().intValue());
    }
    
}
