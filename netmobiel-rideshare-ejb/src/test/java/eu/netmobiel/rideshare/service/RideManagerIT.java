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
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.rideshare.event.BookingSettledEvent;
import eu.netmobiel.rideshare.filter.RideFilter;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
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
import eu.netmobiel.rideshare.model.TimeUnit;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.test.Fixture;
import eu.netmobiel.rideshare.test.RideshareIntegrationTestBase;

@RunWith(Arquillian.class)
public class RideManagerIT extends RideshareIntegrationTestBase {
	
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
//	            .addAsResource("logging.properties")
                .addPackages(true, RideDao.class.getPackage())
                .addPackage(BookingSettledEvent.class.getPackage())
	            .addClass(RideItineraryHelper.class)
	            .addClass(IdentityHelper.class)
	            .addClass(EventListenerHelper.class)
	            .addClass(RideManager.class);
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private RideManager rideManager;
    @Inject
    private RideItineraryHelper rideItineraryHelper;

    private RideshareUser driver1;
    private Car car1;
    private RideshareUser passenger1;
    @Inject
    private EventListenerHelper eventListenerHelper;

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
		flush();
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
		flush();
		rideManager.removeRide(rideId, null, null);
		Long count = em.createQuery("select count(r) from Ride r where id = :id", Long.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertEquals(0L, count.longValue());
		try {
			expectFailure();
			rideManager.removeRide(rideId, null, null);
			fail("Expected a NotFoundException");
		} catch (Exception ex) {
			assertTrue(ex instanceof NotFoundException);
		}
		assertEquals(0, eventListenerHelper.getRideRemovedEventCount());
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
		Booking b = Fixture.createBooking(rdb, passenger1, departureTime, arrivalTime, "trip-1");
		b.setState(BookingState.CONFIRMED);
		em.persist(b);
		flush();
		rideManager.removeRide(rideId, null, null);
		Long count = em.createQuery("select count(r) from Ride r where r.id = :id and r.deleted = true", Long.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertEquals(1L, count.longValue());
		eventListenerHelper.reset();
		// Test starts here
		flush();
		try {
			expectFailure();
			rideManager.removeRide(rideId, null, null);
			fail("Expected a SoftRemovedException");
		} catch (Exception ex) {
			assertTrue(ex instanceof SoftRemovedException);
		}
		assertEquals(0, eventListenerHelper.getRideRemovedEventCount());
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
		flush();
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
		flush();
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
		flush();
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
		flush();
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
		rideItineraryHelper.updateRideItinerary(r1);
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
		flush();
		assertNotNull(rideId);
		Ride r1 = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertNotNull(r1);
		assertEquals(1, r1.getLegs().size());
		Booking b = Fixture.createBooking(r1, passenger1, Fixture.placeZieuwentRKKerk, departureTime, Fixture.placeSlingeland, arrivalTime, "trip-1");
		b.setState(BookingState.CONFIRMED);
		assertNotEquals(r1.getFrom(), b.getPickup());
		assertEquals(r1.getTo(), b.getDropOff());
		em.persist(b);
		flush();
		r1 = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		rideItineraryHelper.updateRideItinerary(r1);
		flush();
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
    
    @Test
    public void getRideDetail() throws Exception {
		Long rideId = createRecurrentRides(1);
		assertNotNull(rideId);
		flush();
    	Ride rut = rideManager.getRide(rideId);
    	flush();
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertFalse(em.contains(rut));
    	assertNotNull(rut);
    	assertNotNull(rut.getRideTemplate());
    	assertNotNull(rut.getRideTemplate().getRecurrence().getInterval());
    	assertNotNull(rut.getCar());
    	assertTrue(puu.isLoaded(rut, Ride_.CAR));
    	assertNotNull(rut.getCar().getLicensePlate());
    	assertNotNull(rut.getCarRef());
    	assertNotNull(rut.getDriver());
    	assertNotNull(rut.getDriver().getManagedIdentity());
    	assertNotNull(rut.getDriverRef());
    	assertNotNull(rut.getBookings());
    	assertEquals(0, rut.getBookings().size());
    	assertNotNull(rut.getLegs());
    	assertEquals(1, rut.getLegs().size());
    	
		Booking booking = Fixture.createBooking(rut, passenger1, Fixture.placeZieuwentRKKerk, rut.getDepartureTime(), Fixture.placeSlingeland, rut.getArrivalTime(), "trip-1");
		booking.setState(BookingState.CONFIRMED);
		em.persist(booking);
		flush();
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rut.getId())
				.getSingleResult();
		rideItineraryHelper.updateRideItinerary(rdb);
		// Now the ride is recalculated. Verify the leg and booking attributes
		flush();
    	rut = rideManager.getRide(rideId);
    	flush();
    	assertFalse(em.contains(rut));
    	assertNotNull(rut);
    	assertTrue(puu.isLoaded(rut, Ride_.BOOKINGS));
    	assertNotNull(rut.getBookings());
    	assertEquals(1, rut.getBookings().size());
    	assertNotNull(rut.getLegs());
    	assertEquals(2, rut.getLegs().size());
    	// The stops are never fetched, they are part of the legs
    	rut.getLegs().forEach(leg -> assertFalse(puu.isLoaded(leg, Leg_.BOOKINGS)));
    	rut.getBookings().forEach(b -> assertTrue(puu.isLoaded(b, Booking_.PASSENGER)));
    	rut.getBookings().forEach(b -> assertTrue(puu.isLoaded(b, Booking_.LEGS)));
    	rut.getBookings().forEach(b -> assertFalse(puu.isLoaded(b, Booking_.RIDE)));
    	rut.getBookings().forEach(b -> b.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.ID))));
    	rut.getBookings().forEach(b -> b.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.FROM))));
    	rut.getBookings().forEach(b -> b.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.TO))));
    }

    @Test
    public void listRides() throws Exception {
    	int nrRides = 2;
		Long rideId = createRecurrentRides(nrRides);
		assertNotNull(rideId);
		flush();
    	RideFilter filter = new RideFilter(driver1, null, null);
    	filter.validate();
    	Cursor cursor = new Cursor(10, 0);
    	PagedResult<Ride> ruts = rideManager.listRides(filter, cursor);
    	flush();
    	assertEquals(nrRides, ruts.getTotalCount().intValue());

    	
    	
    	Ride rut = ruts.getData().get(0);
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertFalse(em.contains(rut));
    	assertNotNull(rut);
    	assertTrue(puu.isLoaded(rut, Ride_.BOOKINGS));
    	assertTrue(puu.isLoaded(rut, Ride_.CAR));
    	assertTrue(puu.isLoaded(rut, Ride_.DRIVER));
    	assertFalse(puu.isLoaded(rut, Ride_.LEGS));
    	assertTrue(puu.isLoaded(rut, Ride_.RIDE_TEMPLATE));

    	assertEquals(0, rut.getBookings().size());
    	assertNotNull(rut.getCar().getLicensePlate());
    	assertNotNull(rut.getCarRef());
//    	assertNotNull(rut.getDriver().getManagedIdentity());
//    	assertNotNull(rut.getDriverRef());
//    	assertEquals(1, rut.getLegs().size());
    	assertNotNull(rut.getRideTemplate());
    	assertNotNull(rut.getRideTemplate().getRecurrence().getInterval());
    	
    	// Verify sorting
    	assertTrue(nrRides >= 2);
    	assertTrue(ruts.getData().get(0).getDepartureTime().isBefore(ruts.getData().get(1).getDepartureTime()));
    	filter.setSortDir(SortDirection.ASC);
    	ruts = rideManager.listRides(filter, cursor);
    	assertTrue(ruts.getData().get(0).getDepartureTime().isBefore(ruts.getData().get(1).getDepartureTime()));
    	filter.setSortDir(SortDirection.DESC);
    	ruts = rideManager.listRides(filter, cursor);
    	assertTrue(ruts.getData().get(0).getDepartureTime().isAfter(ruts.getData().get(1).getDepartureTime()));

    }

    @Test
    public void searchRides() throws Exception {
    	int nrRides = 2;
		Long rideId = createRecurrentRides(nrRides);
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		assertNotNull(rideId);
		flush();
    	PagedResult<Ride> ruts = rideManager.search(Fixture.placeZieuwentRKKerk, Fixture.placeSlingeland, 
    			rdb.getDepartureTime().minusSeconds(3600), rdb.getArrivalTime().plusSeconds(3600), 1, false, 10, 0);
    	flush();
    	assertEquals(1, ruts.getTotalCount().intValue());
    	Ride rut = ruts.getData().get(0);
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertFalse(em.contains(rut));
    	assertNotNull(rut);
    	assertFalse(puu.isLoaded(rut, Ride_.BOOKINGS));
    	assertTrue(puu.isLoaded(rut, Ride_.CAR));
    	assertTrue(puu.isLoaded(rut, Ride_.DRIVER));
    	assertFalse(puu.isLoaded(rut, Ride_.LEGS));
    	assertFalse(puu.isLoaded(rut, Ride_.RIDE_TEMPLATE));

    	assertNotNull(rut.getCar().getLicensePlate());
    	assertNotNull(rut.getCarRef());
    	assertNotNull(rut.getDriver().getManagedIdentity());
    	assertNotNull(rut.getDriverRef());
    }

    @Test
    public void instantiateRides() throws Exception {
		Long rideId = createRecurrentRides(1);
		Ride rdb = em.createQuery("from Ride where id = :id", Ride.class)
				.setParameter("id", rideId)
				.getSingleResult();
		RideTemplate rt = rdb.getRideTemplate();
		assertNotNull(rideId);
//		flush();
//		rt = em.find(RideTemplate.class, rt.getId());
		assertEquals(1,  rt.getRecurrence().getInterval().intValue());
		assertEquals(TimeUnit.DAY,  rt.getRecurrence().getUnit());
		LocalDate horizon = rt.getRecurrence().getLocalHorizon();
		horizon = horizon.plusDays(7);
		rt.getRecurrence().setLocalHorizon(horizon);
		flush();
		Long count = em.createQuery("select count(r) from Ride r where r.rideTemplate = :template", Long.class)
				.setParameter("template", rt)
				.getSingleResult();
		assertEquals(1, count.intValue());
		rideManager.instantiateRecurrentRides();
		count = em.createQuery("select count(r) from Ride r where r.rideTemplate = :template", Long.class)
				.setParameter("template", rt)
				.getSingleResult();
		assertEquals(1 + 7, count.intValue());
		
    }
}
