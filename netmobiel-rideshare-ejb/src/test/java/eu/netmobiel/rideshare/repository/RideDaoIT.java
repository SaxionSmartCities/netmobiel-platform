package eu.netmobiel.rideshare.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.rideshare.filter.RideFilter;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideState;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.test.Fixture;
import eu.netmobiel.rideshare.test.RideshareIntegrationTestBase;

@RunWith(Arquillian.class)
public class RideDaoIT extends RideshareIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(RideDao.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private RideDao rideDao;

    @SuppressWarnings("unused")
	@Inject
    private Logger log;

    private RideshareUser driver1;
    private Car car1;
    private RideshareUser passenger1;


    protected void insertData() throws Exception {
        driver1 = Fixture.createDriver1();
		em.persist(driver1);

		car1 = Fixture.createCarVolvo(driver1);
		em.persist(car1);

		passenger1= Fixture.createPassenger1();
		em.persist(passenger1);
    }

    private void saveNewRide(Ride r) {
    	rideDao.save(r);
    	r.getStops().forEach(stop -> em.persist(stop));
    	r.getLegs().forEach(leg -> em.persist(leg));
    }

    public void findRidesBeyondTemplateSetup(int depShift, int expectedCount) {
    	Instant departureTime = Instant.parse("2020-06-01T00:00:00Z");
    	RideTemplate t = Fixture.createTemplate(car1, departureTime, null);
    	em.persist(t);

    	Ride r1 = Fixture.createRide(t, departureTime.plusSeconds(depShift));
    	saveNewRide(r1);
    	
    	List<Ride> rides = rideDao.findRidesBeyondTemplate(t);
    	assertNotNull(rides);
    	assertEquals(expectedCount, rides.size());
    }

    @Test
    public void findRidesBeyondTemplate_None() {
    	Instant departureTime = Instant.parse("2020-06-01T00:00:00Z");
    	RideTemplate t = Fixture.createTemplate(car1, departureTime, null);
    	em.persist(t);

    	List<Ride> rides = rideDao.findRidesBeyondTemplate(t);
    	assertNotNull(rides);
    	assertEquals(0, rides.size());
    }

    @Test
    public void findRidesBeyondTemplate_Before() {
    	findRidesBeyondTemplateSetup(-120 * 60, 0);
    }

    @Test
    public void findRidesBeyondTemplate_ArrivalOverlap() {
    	findRidesBeyondTemplateSetup(-5 * 60, 1);
    }

    @Test
    public void findRidesBeyondTemplate_DepartureOverlap() {
    	findRidesBeyondTemplateSetup(5 * 60, 1);
    }

    @Test
    public void findRidesBeyondTemplate_After() {
    	findRidesBeyondTemplateSetup(120 * 60, 1);
    }

    @Test
    public void listRides_NoFilter() {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);
    	RideFilter filter = new RideFilter(driver1, null, null);
    	PagedResult<Long> rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());
    }

    @Test
    public void listRides_Since() throws Exception {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);
    	
    	Instant since = Instant.parse("2020-06-01T00:00:00Z");
    	RideFilter filter = new RideFilter(driver1, since, null);
    	filter.validate();
    	PagedResult<Long> rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());

    	since = Instant.parse("2020-06-03T00:00:00Z");
    	filter.setSince(since);
    	filter.validate();
    	rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(0, rides.getTotalCount().intValue());
    }

    @Test
    public void listRides_Until() throws Exception {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);

    	Instant until = Instant.parse("2020-06-01T00:00:00Z");
    	RideFilter filter = new RideFilter(driver1, null, until);
    	filter.validate();
    	PagedResult<Long> rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(0, rides.getTotalCount().intValue());

    	until = Instant.parse("2020-06-03T00:00:00Z");
    	filter.setUntil(until);
    	filter.validate();
    	rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());
    }

    @Test
    public void listRides_RideState() throws Exception {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);

    	RideFilter filter = new RideFilter(driver1, null, null, RideState.SCHEDULED, null);
    	filter.validate();
    	PagedResult<Long> rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());

    	filter.setRideState(RideState.COMPLETED);
    	filter.validate();
    	rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(0, rides.getTotalCount().intValue());
    }

    @Test
    public void listRides_BookingState() throws Exception {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);

    	RideFilter filter = new RideFilter(driver1, null, null, null, BookingState.REQUESTED);
    	filter.validate();
    	PagedResult<Long> rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(0, rides.getTotalCount().intValue());

    }

    @Test
    public void listRides_Deleted() throws Exception {
    	Instant departureTime = Instant.parse("2020-06-02T00:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, null);
    	saveNewRide(r1);

    	RideFilter filter = new RideFilter(driver1, null, null);
    	filter.validate();
    	PagedResult<Long> rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());

    	r1.setDeleted(true);
    	flush();
    	rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(0, rides.getTotalCount().intValue());

    	filter.setDeletedToo(true);
    	filter.validate();
    	rides = rideDao.findByDriver(filter, Cursor.COUNTING_CURSOR);
    	assertNotNull(rides);
    	assertEquals(1, rides.getTotalCount().intValue());
    }

    private void testSimpleSearch(GeoLocation from, GeoLocation to, Instant earliestDeparture, Instant latestArrival, boolean lenient, int expectedCount) {
    	PagedResult<Long> result = rideDao.search(from, to, 60, earliestDeparture, latestArrival, 1, lenient, 1, 0, 0);
    	assertNotNull(result);
    	assertEquals(expectedCount, result.getTotalCount().intValue());
    }

    private void testSimpleSearch(GeoLocation from, GeoLocation to, Instant earliestDeparture, Instant latestArrival, boolean lenient, Integer maxBookings, int expectedCount) {
    	PagedResult<Long> result = rideDao.search(from, to, 60, earliestDeparture, latestArrival, 1, lenient, maxBookings, 0, 0);
    	assertNotNull(result);
    	assertEquals(expectedCount, result.getTotalCount().intValue());
    }

    @Test
    public void testSearchRides_Leniet() throws Exception {
//      * 2.1 lenient = false: The ride departs after <code>earliestDeparture</code> and arrives before <code>latestArrival</code>;
//      * 2.2 lenient = true: The ride arrives after <code>earliestDeparture</code> and departs before <code>latestArrival</code>;

    	Instant departureTime = Instant.parse("2020-06-02T12:00:00Z");
    	Instant arrivalTime = Instant.parse("2020-06-02T13:00:00Z");
    	Ride r1 = Fixture.createRide(car1, Fixture.placeThuisLichtenvoorde, departureTime, Fixture.placeCentrumDoetinchem, arrivalTime);
    	saveNewRide(r1);
    	flush();

    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.minusSeconds(120 * 60), departureTime.minusSeconds(60 * 60), false, 0);
    	
    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.minusSeconds(15 * 60), arrivalTime.plusSeconds(15 * 60), false, 1);

    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.plusSeconds(15 * 60), arrivalTime.plusSeconds(15 * 60), false, 0);

    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.minusSeconds(15 * 60), arrivalTime.minusSeconds(15 * 60), false, 0);

    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.plusSeconds(15 * 60), arrivalTime.minusSeconds(15 * 60), false, 0);
    	
    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, arrivalTime.plusSeconds(60 * 60), arrivalTime.plusSeconds(120 * 60), false, 0);


    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.minusSeconds(120 * 60), departureTime.minusSeconds(60 * 60), true, 0);
    	
    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.minusSeconds(15 * 60), arrivalTime.plusSeconds(15 * 60), true, 1);

    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.plusSeconds(15 * 60), arrivalTime.plusSeconds(15 * 60), true, 1);

    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.minusSeconds(15 * 60), arrivalTime.minusSeconds(15 * 60), true, 1);

    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.plusSeconds(15 * 60), arrivalTime.minusSeconds(15 * 60), true, 1);
    	
    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, arrivalTime.plusSeconds(60 * 60), arrivalTime.plusSeconds(120 * 60), true, 0);

    }
    
    @Test
    public void testSearchRides_Bearing() throws Exception {
    	// Passenger and driver should travel in same direction
    	Instant departureTime = Instant.parse("2020-06-02T12:00:00Z");
    	Instant arrivalTime = Instant.parse("2020-06-02T13:00:00Z");
    	Ride r1 = Fixture.createRide(car1, Fixture.placeThuisLichtenvoorde, departureTime, Fixture.placeCentrumDoetinchem, arrivalTime);
    	saveNewRide(r1);
    	flush();

    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.minusSeconds(15 * 60), arrivalTime.plusSeconds(15 * 60), false, 1);
    	testSimpleSearch(Fixture.placeSlingeland, Fixture.placeZieuwent, departureTime.minusSeconds(15 * 60), arrivalTime.plusSeconds(15 * 60), false, 0);
    }

    @Test
    public void testSearchRides_Detour() throws Exception {
    	// Passenger and driver should travel in same direction
    	Instant departureTime = Instant.parse("2020-06-02T12:00:00Z");
    	Instant arrivalTime = Instant.parse("2020-06-02T13:00:00Z");
    	Ride r1 = Fixture.createRide(car1, Fixture.placeThuisLichtenvoorde, departureTime, Fixture.placeCentrumDoetinchem, arrivalTime);
    	r1.setMaxDetourMeters(500);
    	r1.updateShareEligibility();
    	saveNewRide(r1);
    	flush();

    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, null, null, false, 0);
    	// NOTE: Check with pgAdmin the ride. pgAdmin has a feature in the data output window to display the geometry. Works really well.
    }

    @Test
    public void testSearchRides_Bookings() throws Exception {
    	// Passenger and driver should travel in same direction
    	Instant departureTime = Instant.parse("2020-06-02T12:00:00Z");
    	Instant arrivalTime = Instant.parse("2020-06-02T13:00:00Z");
    	Ride r1 = Fixture.createRide(car1, Fixture.placeThuisLichtenvoorde, departureTime, Fixture.placeCentrumDoetinchem, arrivalTime);
    	saveNewRide(r1);
    	flush();
    	
    	// Can find a ride
    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, departureTime.plusSeconds(15 * 60), arrivalTime.minusSeconds(10 * 60), true, 1);

    	Booking b1 = Fixture.createBooking(r1, passenger1, Fixture.placeZieuwent, departureTime.plusSeconds(15 * 60), Fixture.placeSlingeland, arrivalTime.minusSeconds(10 * 60), "trip-1");
    	b1.setState(BookingState.CONFIRMED);
    	em.persist(b1);
    	flush();
    	// No rides found, there is a booking
    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, null, null, true, 1, 0);
    	// nevermind the bookings, find all
    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, null, null, true, null, 1);

    	b1.setState(BookingState.CANCELLED);
    	em.merge(b1);
    	// Can find a ride again, it is a deleted booking
    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, null, null, true, 1, 1);

    	Booking b2 = Fixture.createBooking(r1, passenger1, Fixture.placeZieuwent, departureTime.plusSeconds(15 * 60), Fixture.placeSlingeland, arrivalTime.minusSeconds(10 * 60), "trip-2");
    	b2.setState(BookingState.CONFIRMED);
    	em.persist(b2);
    	flush();
    	// No rides found, there is a booking again
    	testSimpleSearch(Fixture.placeZieuwent, Fixture.placeSlingeland, null, null, true, 1, 0);
    }

    @Test
    public void existsTemporalOverlap() throws Exception {
    	Instant departureTime = Instant.parse("2020-06-02T11:00:00Z");
    	Ride r1 = Fixture.createRideObject(car1, departureTime, departureTime.plusSeconds(3600));
    	em.persist(r1);

    	// Completely before
    	Ride r2 = Fixture.createRideObject(car1, r1.getDepartureTime().minusSeconds(7200), r1.getArrivalTime().minusSeconds(7200));
    	em.persist(r2);
    	flush();
    	assertFalse(rideDao.existsTemporalOverlap(r2));

    	// Arrival in overlap
    	r2 = rideDao.find(r2.getId()).orElseThrow(() -> new IllegalStateException("No such ride: "));
    	r2.setDepartureTime(r1.getDepartureTime().minusSeconds(1800));
    	r2.setArrivalTime(r1.getArrivalTime().minusSeconds(1800));
    	flush();
    	assertTrue(rideDao.existsTemporalOverlap(r2));

    
    	// Complete overlap
    	r2 = rideDao.find(r2.getId()).orElseThrow(() -> new IllegalStateException("No such ride: "));
    	r2.setDepartureTime(r1.getDepartureTime().minusSeconds(1800));
    	r2.setArrivalTime(r1.getArrivalTime().plusSeconds(1800));
    	flush();
    	assertTrue(rideDao.existsTemporalOverlap(r2));

    	// Complete containment
    	r2 = rideDao.find(r2.getId()).orElseThrow(() -> new IllegalStateException("No such ride: "));
    	r2.setDepartureTime(r1.getDepartureTime().plusSeconds(300));
    	r2.setArrivalTime(r1.getArrivalTime().minusSeconds(300));
    	flush();
    	assertTrue(rideDao.existsTemporalOverlap(r2));
   
    	// Departure in overlap
    	r2 = rideDao.find(r2.getId()).orElseThrow(() -> new IllegalStateException("No such ride: "));
    	r2.setDepartureTime(r1.getDepartureTime().plusSeconds(1800));
    	r2.setArrivalTime(r1.getArrivalTime().plusSeconds(1800));
    	flush();
    	assertTrue(rideDao.existsTemporalOverlap(r2));
    	
    	// Completely before
    	r2 = rideDao.find(r2.getId()).orElseThrow(() -> new IllegalStateException("No such ride: "));
    	flush();
    	r2.setDepartureTime(r1.getDepartureTime().plusSeconds(7200));
    	r2.setArrivalTime(r1.getArrivalTime().plusSeconds(7200));
    	assertFalse(rideDao.existsTemporalOverlap(r2));

   }
    


}
