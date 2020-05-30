package eu.netmobiel.rideshare.service;


import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.PersistenceUnitUtil;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SoftRemovedException;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Booking_;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Leg;
import eu.netmobiel.rideshare.model.Leg_;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideBase;
import eu.netmobiel.rideshare.model.RideScope;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.Ride_;
import eu.netmobiel.rideshare.model.Stop;
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
    	assertEquals(1, but.getLegs().size());
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.ID)));
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.FROM)));
    	but.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.TO)));
    	
    	assertTrue(puu.isLoaded(but, Booking_.PASSENGER));
    	assertTrue(puu.isLoaded(but.getPassenger(), User_.ID));
    	assertTrue(puu.isLoaded(but.getPassenger(), User_.GIVEN_NAME));
    	assertTrue(puu.isLoaded(but.getPassenger(), User_.FAMILY_NAME));
    	assertTrue(puu.isLoaded(but.getPassenger(), User_.MANAGED_IDENTITY));
    	
    	assertTrue(puu.isLoaded(but, Booking_.RIDE));
    	assertTrue(puu.isLoaded(but.getRide(), Ride_.ID));
    	// The following should not be there, but they are.
//    	assertFalse(puu.isLoaded(but.getRide(), Ride_.ARRIVAL_TIME));
//    	assertFalse(puu.isLoaded(but.getRide(), Ride_.LEGS));
    }

}
