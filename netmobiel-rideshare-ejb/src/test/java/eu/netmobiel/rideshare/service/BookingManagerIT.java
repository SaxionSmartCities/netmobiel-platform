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
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Booking_;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Leg_;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.Ride_;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.model.User_;
import eu.netmobiel.rideshare.test.Fixture;
import eu.netmobiel.rideshare.test.RideshareIntegrationTestBase;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@RunWith(Arquillian.class)
public class BookingManagerIT extends RideshareIntegrationTestBase {
	
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
	            .addClass(RideItineraryHelper.class)
	            .addClass(BookingManager.class);
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private RideItineraryHelper rideItineraryHelper;
    @Inject
    private BookingManager bookingManager;

    private User driver1;
    private Car car1;
    private User passenger1;


    @Override
    protected void insertData() throws Exception {
        driver1 = Fixture.createUser(loginContextDriver);
		em.persist(driver1);

		car1 = Fixture.createCarFordThunderbird(driver1);
		em.persist(car1);

		passenger1 = Fixture.createUser(loginContextPassenger);
		em.persist(passenger1);
    }

    @Test
    public void createBooking() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
		Long rideId = r.getId();
		assertNotNull(rideId);
		flush();
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime());
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
		assertEquals(1, b.getLegs().size());
    }		

    @Test
    public void createBooking_Multiple() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
		Long rideId = r.getId();
		assertNotNull(rideId);
		flush();
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime());
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
		assertNotNull(bookingRef);
		flush();
		try {
			Booking booking2 = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime());
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
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime());
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
    	flush();
    	Booking but = bookingManager.getBooking(RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef));
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", r.getId())
				.getSingleResult();
		rideItineraryHelper.updateRideItinerary(rdb);
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
    	assertTrue(puu.isLoaded(but.getRide(), Ride_.ID));

    	assertEquals(1, but.getLegs().size());
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.ID)));
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.FROM)));
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.TO)));
    	
    	// The following should not be there, but they are.
//    	assertFalse(puu.isLoaded(but.getRide(), Ride_.ARRIVAL_TIME));
//    	assertFalse(puu.isLoaded(but.getRide(), Ride_.LEGS));
    }

    @Test
    public void removeBooking() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
		Long rideId = r.getId();
		assertNotNull(rideId);
		flush();
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime());
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
		assertNotNull(bookingRef);
		flush();
		Booking b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertFalse(b.isDeleted());
		String reason = "Afspraak is verplaatst";
		flush();
		
		bookingManager.removeBooking(passenger1, b.getId(), reason);
		flush();
		b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertTrue(b.isDeleted());
		assertFalse(Boolean.TRUE == b.getCancelledByDriver());
		assertEquals(reason, b.getCancelReason());
		r = b.getRide();
		assertEquals(1, r.getLegs().size());
    }


    @Test
    public void removeBooking_ByDriver() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
		Long rideId = r.getId();
		assertNotNull(rideId);
		flush();
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime());
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
		assertNotNull(bookingRef);
		flush();
		Booking b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertFalse(b.isDeleted());
		String reason = "Ik rij niet meer, ik hoef er niet meer te zijn";
		flush();
		
		bookingManager.removeBooking(driver1, b.getId(), reason);
		flush();
		b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
		assertTrue(b.isDeleted());
		assertTrue(Boolean.TRUE == b.getCancelledByDriver());
		assertEquals(reason, b.getCancelReason());
		r = b.getRide();
		assertEquals(1, r.getLegs().size());
    }

    @Test
    public void listBookings() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createCompleteRide(car1, departureTime, null);
    	rideItineraryHelper.saveNewRide(r);
    	rideItineraryHelper.updateRideItinerary(r);
		Long rideId = r.getId();
		assertNotNull(rideId);
		flush();
		Booking booking = Fixture.createBooking(r, passenger1, Fixture.placeZieuwentRKKerk, r.getDepartureTime(), Fixture.placeSlingeland, r.getArrivalTime());
		String bookingRef = bookingManager.createBooking(r.getRideRef(), passenger1, booking);
		assertNotNull(bookingRef);
		flush();
		Booking b = em.createQuery("from Booking where id = :id", Booking.class)
				.setParameter("id", RideshareUrnHelper.getId(Booking.URN_PREFIX, bookingRef))
				.getSingleResult();
		assertNotNull(b);
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
