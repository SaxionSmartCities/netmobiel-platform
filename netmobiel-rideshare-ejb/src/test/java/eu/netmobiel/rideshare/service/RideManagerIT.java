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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SoftRemovedException;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Leg;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideBase;
import eu.netmobiel.rideshare.model.RideScope;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.Stop;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.test.Fixture;
import eu.netmobiel.rideshare.test.RideshareIntegrationTestBase;

@RunWith(Arquillian.class)
public class RideManagerIT extends RideshareIntegrationTestBase {
	
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
//	            .addAsResource("logging.properties")
	            .addClass(RideManager.class);
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private RideManager rideManager;

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

    private void verifyRideBase(RideBase r, RideBase rdb, Instant departureTime, Instant arrivalTime) {
    	if (departureTime != null) {
    		assertEquals(departureTime, rdb.getDepartureTime());
    		assertNotNull(rdb.getArrivalTime());
    		assertFalse(rdb.isArrivalTimePinned());
    	} else {
    		assertNotNull(rdb.getDepartureTime());
    		assertEquals(arrivalTime, rdb.getArrivalTime());
    		assertTrue(rdb.isArrivalTimePinned());
    	}
		assertEquals(r.getFrom(), rdb.getFrom());
		assertEquals(r.getTo(), rdb.getTo());
		assertEquals(r.getCar().getId(), rdb.getCar().getId());
		assertEquals(r.getDriver().getId(), rdb.getCar().getDriver().getId());
		assertNotNull(rdb.getCarthesianBearing());
		assertNotNull(rdb.getCarthesianDistance());
		if (car1.getCo2Emission() != null) {
			assertNotNull(rdb.getCO2Emission());
		} else {
			assertNull(rdb.getCO2Emission());
		}
		assertNotNull(rdb.getCarRef());
		assertNotNull(rdb.getDistance());
		assertNotNull(rdb.getDriverRef());
		assertNotNull(rdb.getDuration());
		assertEquals(r.getMaxDetourMeters(), rdb.getMaxDetourMeters());
		assertEquals(r.getMaxDetourSeconds(), rdb.getMaxDetourSeconds());
		assertEquals(r.getNrSeatsAvailable(), rdb.getNrSeatsAvailable());
		assertNull(rdb.getRemarks());
		assertNotNull(rdb.getShareEligibility());
    }

    private void checkRideConsistency(Ride rdb) {
		assertNotNull(rdb.getBookings());
		assertEquals(0, rdb.getBookings().size());

		assertNotNull(rdb.getLegs());
		assertEquals(1, rdb.getLegs().size());
		Leg leg = rdb.getLegs().get(0);
		assertNotNull(leg);
		assertNotNull(leg.getBookings());
		assertEquals(0, leg.getBookings().size());
		assertEquals(rdb.getDistance(), leg.getDistance());
		assertEquals(rdb.getDuration(), leg.getDuration());
		assertEquals(Integer.valueOf(0), leg.getLegIx());
		assertEquals(rdb.getArrivalTime(), leg.getEndTime());
		assertEquals(rdb.getDepartureTime(), leg.getStartTime());
		assertNotNull(leg.getFrom());
		assertEquals(rdb.getFrom(), leg.getFrom().getLocation());
		assertEquals(rdb.getDepartureTime(), leg.getFrom().getDepartureTime());
		assertNotNull(leg.getTo());
		assertEquals(rdb.getTo(), leg.getTo().getLocation());
		assertEquals(rdb.getArrivalTime(), leg.getTo().getArrivalTime());
		assertNull(leg.getFrom().getArrivalTime());
		assertNull(leg.getTo().getDepartureTime());
		assertNotNull(leg.getLegGeometry());
		assertNotNull(leg.getLegGeometryEncoded());

		assertNotNull(rdb.getStops());
		assertEquals(2, rdb.getStops().size());
    }

    @Test
    public void createSimpleRide_Departure() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createRide(car1, departureTime, null);
		Long rideId = rideManager.createRide(r);
		assertNotNull(rideId);
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		verifyRideBase(r, rdb, departureTime, null);
		checkRideConsistency(rdb);
    }

    @Test
    public void createSimpleRide_Arrival() throws Exception {
    	Instant arrivalTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createRide(car1, null, arrivalTime);
		Long rideId = rideManager.createRide(r);
		assertNotNull(rideId);
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		verifyRideBase(r, rdb, null, arrivalTime);
		checkRideConsistency(rdb);
    }
    
    @Test
    public void createRecurrentRide_Departure() throws Exception {
    	// Choose a time around the wintertime/summertime change: March 29 2020 02:00:00 Europe/Amsterdam
    	ZoneId myZone = ZoneId.of(Recurrence.DEFAULT_TIME_ZONE);
    	LocalDateTime firstLocDep = LocalDateTime.parse("2020-03-25T10:00:00"); // A wednesday  
    	LocalDate horizon = LocalDate.parse("2020-04-02");
    	Instant departureTime = firstLocDep.atZone(myZone).toInstant();	
    	Ride r = Fixture.createRide(car1, departureTime, null);
    	RideTemplate rt = new RideTemplate(); 
    	Recurrence rc = new Recurrence(1, Recurrence.dowMask(DayOfWeek.WEDNESDAY), horizon);
    	rt.setRecurrence(rc);
    	r.setRideTemplate(rt);
		Long rideId = rideManager.createRide(r);
		assertNotNull(rideId);
		flush();
		Ride firstRide = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		verifyRideBase(r, firstRide, departureTime, null);
		checkRideConsistency(firstRide);
		
		List<Ride> rides = em.createQuery("from Ride r where r.rideTemplate = :template order by r.departureTime", Ride.class)
				.setParameter("template", firstRide.getRideTemplate())
				.getResultList();
		assertEquals(2, rides.size());
		Ride lastRide = rides.get(1);
		checkRideConsistency(lastRide);
		LocalDateTime lastLocDep = LocalDateTime.ofInstant(lastRide.getDepartureTime(), myZone);
		assertEquals(firstLocDep.plusWeeks(1), lastLocDep);
    }

    @Test
    public void removeSimpleRide_NotFound() throws Exception {
		Long rideId = 1000L;
		assertNotNull(rideId);
		Long count = em.createQuery("select count(r) from Ride r where id = :id", Long.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertEquals(0L, count.longValue());
    }

    @Test
    public void removeSimpleRide_NoBooking() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createRide(car1, departureTime, null);
		Long rideId = rideManager.createRide(r);
		assertNotNull(rideId);
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertNotNull(rdb);
//		flush();
		rideManager.removeRide(rideId, null, null);
		Long count = em.createQuery("select count(r) from Ride r where id = :id", Long.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertEquals(0L, count.longValue());
		try {
			rideManager.removeRide(rideId, null, null);
			fail("Expected a NotFoundException");
		} catch (Exception ex) {
			assertTrue(ex instanceof NotFoundException);
		}
    }

    @Test
    public void removeSimpleRide_WithBooking() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Instant arrivalTime = Instant.parse("2020-05-01T01:00:00Z");
    	Ride r = Fixture.createRide(car1, departureTime, null);
		Long rideId = rideManager.createRide(r);
		assertNotNull(rideId);
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertNotNull(rdb);
		Booking b = Fixture.createBooking(rdb, passenger1, departureTime, arrivalTime);
		em.persist(b);
		flush();
		rideManager.removeRide(rideId, null, null);
		Long count = em.createQuery("select count(r) from Ride r where r.id = :id and r.deleted = true", Long.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertEquals(1L, count.longValue());
		flush();
		try {
			rideManager.removeRide(rideId, null, null);
			fail("Expected a SoftRemovedException");
		} catch (Exception ex) {
			assertTrue(ex instanceof SoftRemovedException);
		}
    }
    
    public Long createRecurrentRides(int nrRides) throws Exception {
    	// Choose a time around the wintertime/summertime change: March 29 2020 02:00:00 Europe/Amsterdam
    	ZoneId myZone = ZoneId.of(Recurrence.DEFAULT_TIME_ZONE);
    	LocalDate tomorrow = LocalDate.now().plusDays(1L);
    	LocalDateTime firstDepartureTime = LocalDateTime.of(tomorrow, LocalTime.parse("10:00:00"));  
    	LocalDate horizon = tomorrow.plusDays(nrRides);
    	Instant departureTime = firstDepartureTime.atZone(myZone).toInstant();	
    	Ride r = Fixture.createRide(car1, departureTime, null);
    	RideTemplate rt = new RideTemplate(); 
    	Recurrence rc = new Recurrence(1, horizon);
    	rt.setRecurrence(rc);
    	r.setRideTemplate(rt);
		return rideManager.createRide(r);
    }

    @Test
    public void removeRecurrentRide_This() throws Exception {
    	int nrRides = 7;
		Long rideId = createRecurrentRides(nrRides);
		assertNotNull(rideId);
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertNotNull(rdb);
		RideTemplate template = rdb.getRideTemplate(); 
		Long count = em.createQuery("select count(r) from Ride r where r.rideTemplate = :template", Long.class)
				.setParameter("template", template)
				.getSingleResult();
		assertEquals(nrRides, count.longValue());
		
		rideManager.removeRide(rideId, null, null);
		count = em.createQuery("select count(r) from Ride r where r.rideTemplate = :template", Long.class)
				.setParameter("template", template)
				.getSingleResult();
		assertEquals(nrRides - 1, count.longValue());

		count = em.createQuery("select count(rt) from RideTemplate rt where rt = :template", Long.class)
				.setParameter("template", template)
				.getSingleResult();
		assertEquals(1, count.longValue());
}

    @Test
    public void removeRecurrentRide_ThisAndFollowing() throws Exception {
    	int nrRides = 7;
		Long rideId = createRecurrentRides(nrRides);
		assertNotNull(rideId);
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertNotNull(rdb);
		RideTemplate template = rdb.getRideTemplate(); 
		Long count = em.createQuery("select count(r) from Ride r where r.rideTemplate = :template", Long.class)
				.setParameter("template", template)
				.getSingleResult();
		assertEquals(nrRides, count.longValue());
		
		rideManager.removeRide(rideId, null, RideScope.THIS_AND_FOLLOWING);
		count = em.createQuery("select count(r) from Ride r where r.rideTemplate = :template", Long.class)
				.setParameter("template", template)
				.getSingleResult();
		assertEquals(0, count.longValue());
		count = em.createQuery("select count(rt) from RideTemplate rt where rt = :template", Long.class)
				.setParameter("template", template)
				.getSingleResult();
		assertEquals(0, count.longValue());
    }

    @Test
    public void updateRide_Identity() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createRide(car1, departureTime, null);
		Long rideId = rideManager.createRide(r);
		assertNotNull(rideId);
		Ride r1 = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertNotNull(r1);
		rideManager.onUpdateRideItinerary(r1);
		// End the transaction and start new session
		flush();
		// Now assure that no database identities have changed, i.e., no unnecessary new database objects.
		Ride r2 = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertFalse(r1 == r2);
		assertEquals(r1.getId(), r2.getId());
		assertEquals(r1.getLegs().size(), r2.getLegs().size());
		for (int i = 0; i < r1.getLegs().size(); i++) {
			Leg leg1 = r1.getLegs().get(i);
			Leg leg2 = r2.getLegs().get(i);
			assertEquals(leg1, leg2);
			assertFalse(leg1 == leg2);
		}
		assertEquals(r1.getStops().size(), r2.getStops().size());
		for (int i = 0; i < r1.getStops().size(); i++) {
			Stop stop1 = r1.getStops().get(i);
			Stop stop2 = r2.getStops().get(i);
			assertEquals(stop1, stop2);
			assertFalse(stop1 == stop2);
		}
		assertEquals(r1.getBookings().size(), r2.getBookings().size());
		for (int i = 0; i < r1.getBookings().size(); i++) {
			Booking booking1 = r1.getBookings().get(i);
			Booking booking2 = r2.getBookings().get(i);
			assertEquals(booking1, booking2);
			assertFalse(booking1 == booking2);
		}
    }

    @Test
    public void updateRide_AfterBooking() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Instant arrivalTime = Instant.parse("2020-05-01T01:00:00Z");
    	Ride r = Fixture.createRide(car1, departureTime, null);
		Long rideId = rideManager.createRide(r);
		assertNotNull(rideId);
		Ride r1 = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertNotNull(r1);
		Booking b = Fixture.createBooking(r1, passenger1, Fixture.placeZieuwentRKKerk, departureTime, Fixture.placeSlingeland, arrivalTime);
		assertNotEquals(r1.getFrom(), b.getPickup());
		assertEquals(r1.getTo(), b.getDropOff());
		em.persist(b);
		flush();
		rideManager.onUpdateRideItinerary(r1);
		Ride r2 = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertEquals(2, r2.getLegs().size());
    }
    
    @Test
    public void updateSimpleRide_Departure() throws Exception {
    	Instant departureTime = Instant.parse("2020-05-01T00:00:00Z");
    	Ride r = Fixture.createRide(car1, departureTime, null);
		Long rideId = rideManager.createRide(r);
		assertNotNull(rideId);
		flush();
		Instant oldArrivalTime =  r.getArrivalTime();
		int delay = 60 * 30;
    	departureTime = departureTime.plusSeconds(delay);
    	r = Fixture.createRide(car1, departureTime, null);
    	r.setId(rideId);
    	rideManager.updateRide(r, RideScope.THIS);
    	flush();
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertEquals(oldArrivalTime.plusSeconds(delay), rdb.getArrivalTime());
		verifyRideBase(r, rdb, departureTime, null);
		checkRideConsistency(rdb);
    }
}
