package eu.netmobiel.planner.service;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.planner.repository.TripDao;
import eu.netmobiel.planner.test.PlannerIntegrationTestBase;

@RunWith(Arquillian.class)
public class TripManagerIT extends PlannerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(EventListenerHelper.class)
            .addClass(TripDao.class)
            .addClass(TripManager.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @EJB
    private TripManager tripManager;

    @Inject
    private EventListenerHelper eventListenerHelper;

    @Inject
    private Logger log;

    @Override
    protected void insertData() throws Exception {
		eventListenerHelper.reset();
    }

  @Test
  public void testDummy() throws Exception {
	  
  }
//    @Test
//    public void testCreateShoutOutTrip() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(1L);
//    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        int nrTripsStart = trips.getData().size();
//        
//        Trip trip = createShoutOutTrip("2020-01-07T14:30:00+01:00", "2020-01-07T16:30:00+01:00");
//    	Long id = tripManager.createTrip(traveller, trip, true);
//        assertNotNull(id);
//        
//    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        log.info("List trips: #" + trips.getData().size());
//        trips.getData().stream().filter(t -> t.getId() == id).findFirst().ifPresent(t -> log.debug(t.toString()));
//        assertEquals(nrTripsStart + 1, trips.getData().size());
//        
//		assertEquals(1, eventListenerHelper.getShoutOutRequestedEventCount());
//        
//    }
//
//    
//    @Test
//    public void testGetTrip() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(1L);
//        flush();
//        Trip trip = createSimpleTrip("2020-01-07T14:30:00+01:00", "2020-01-07T16:30:00+01:00");
//    	Long id = tripManager.createTrip(traveller, trip, true);
//        assertNotNull(id);
//        flush();
//    	assertFalse(em.contains(trip));
//        
//        trip = tripManager.getTrip(id);
//        assertNotNull(trip);
//        assertEquals(id, trip.getId());
//    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
//    	assertTrue(em.contains(trip));
//        flush();
//    	assertFalse(em.contains(trip));
//    	assertTrue(puu.isLoaded(trip, Trip_.LEGS));
//    	assertTrue(puu.isLoaded(trip, Trip_.STOPS));
//    	assertTrue(puu.isLoaded(trip, Trip_.TRAVELLER));
//    	trip.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.GUIDE_STEPS)));
//        assertNotNull(trip.getLegs());
//        assertEquals(1, trip.getLegs().size());
//
//    }
//    
//
//    @Test
//    public void testCreateFullTrip() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(1L);
//    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        int nrTripsStart = trips.getData().size();
//        
//        Trip trip = createLargeTrip();
//        Long id = tripManager.createTrip(traveller, trip, true);
//        assertNotNull(id);
//        
//    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        log.info("List trips: #" + trips.getData().size());
//        trips.getData().stream().filter(t -> t.getId() == id).findFirst().ifPresent(t -> log.debug(t.toString()));
//        assertEquals(nrTripsStart + 1, trips.getData().size());
//    }
//
//    private Trip createRideshareTrip(User traveller, String rideRef) {
//    	TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-01-06T13:30:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-01-07T13:30:00Z", false, 60 * 35, "urn:nb:rs:ride:354");
//    	Trip trip = Fixture.createTrip(traveller, plan);
//		return trip;
//    }
//
//    @Test
//    public void testCreateRideshareTrip_NoAutoBook() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(1L);
//    	String rideRef = "urn:nb:rs:ride:354";
//        Trip trip = createRideshareTrip(traveller, rideRef);
//
//		// Set autobook false
//    	Long id = tripManager.createTrip(traveller, trip, false);
//        assertNotNull(id);
//		Trip tripdb = em.createQuery("from Trip where id = :id", Trip.class)
//				.setParameter("id", id)
//				.getSingleResult();
//		assertEquals(TripState.PLANNING, tripdb.getState());
//		assertEquals(0, eventListenerHelper.getBookingRequestedEventCount());
//    }
//
//    @Test
//    public void testCreateRideshareTrip_AutoBook() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(1L);
//    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        int nrTripsStart = trips.getData().size();
//    	String rideRef = "urn:nb:rs:ride:354";
//        Trip trip = createRideshareTrip(rideRef);
//
//		// Set autobook true
//    	Long id = tripManager.createTrip(traveller, trip, true);
//        assertNotNull(id);
//		Trip tripdb = em.createQuery("from Trip where id = :id", Trip.class)
//				.setParameter("id", id)
//				.getSingleResult();
//		assertEquals(TripState.BOOKING, tripdb.getState());
//		assertEquals(1, eventListenerHelper.getBookingRequestedEventCount());
//        
//    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        log.info("List trips: #" + trips.getData().size());
//        trips.getData().stream().filter(t -> t.getId() == id).findFirst().ifPresent(t -> log.debug(t.toString()));
//        assertEquals(nrTripsStart + 1, trips.getData().size());
//
//    }
//
//    @Test
//    public void testAssignRideshareBookingRef() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(1L);
//    	String rideRef = "urn:nb:rs:ride:354";
//        Trip trip = createRideshareTrip(rideRef);
//		Optional<Leg> leg = trip.getItinerary().findLegByTripId(rideRef);
//		assertTrue(leg.isPresent());
//		assertNull(leg.get().getBookingId());
//
//		// Set autobook true
//    	Long id = tripManager.createTrip(traveller, trip, true);
//    	flush();
//        assertNotNull(id);
//		Trip tripdb = em.createQuery("select t from Trip t join fetch t.legs where t.id = :id", Trip.class)
//				.setParameter("id", id)
//				.getSingleResult();
//		leg = tripdb.getItinerary().findLegByTripId(rideRef);
//		assertTrue(leg.isPresent());
//		assertNull(leg.get().getBookingId());
//		flush();
//
//		String bookingRef = "urn:nb:rs:booking:12345";
//		// Assign, without confirmation yet
//    	tripManager.assignBookingReference(trip.getTripRef(), rideRef, bookingRef, false);
//		flush();
//		tripdb = em.createQuery("select t from Trip t join fetch t.legs where t.id = :id", Trip.class)
//				.setParameter("id", id)
//				.getSingleResult();
//		assertEquals(TripState.BOOKING, tripdb.getState());
//		assertEquals(1, eventListenerHelper.getBookingRequestedEventCount());
//		assertTrue(tripdb.getItinerary().findLegByTripId(rideRef).isPresent());
//		assertTrue(tripdb.getItinerary().findLegByBookingId(bookingRef).isPresent());
//    }
//
//    @Test
//    public void testRemoveRideshareTrip_WhileBooking() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(1L);
//    	String rideRef = "urn:nb:rs:ride:354";
//        Trip trip = createRideshareTrip(rideRef);
//		// Set autobook true
//    	Long id = tripManager.createTrip(traveller, trip, true);
//		String bookingRef = "urn:nb:rs:booking:12345";
//		// Assign, without confirmation yet
//    	tripManager.assignBookingReference(trip.getTripRef(), rideRef, bookingRef, false);
//		eventListenerHelper.reset();
//		flush();
//		String reason = "Ik ga toch maar niet";
//        tripManager.removeTrip(id, reason);
//		assertEquals(1, eventListenerHelper.getBookingCancelledEventCount());
//		// Trip is not hard removed.
//		assertEquals(1, em.createQuery("select count(*) from Trip where id = :id", Long.class).setParameter("id", id).getSingleResult().intValue());
//		Trip tripdb = em.createQuery("from Trip where id = :id", Trip.class)
//				.setParameter("id", id)
//				.getSingleResult();
//		assertEquals(TripState.CANCELLED, tripdb.getState());
//		assertEquals(reason, tripdb.getCancelReason());
//    }
//
//    @Test
//    public void testRemoveFullTrip() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(1L);
//    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        int nrTripsStart = trips.getData().size();
//        boolean autobook = false;
//        
//        Trip trip = createLargeTrip();
//        Long id = tripManager.createTrip(traveller, trip, autobook);
//        assertNotNull(id);
//        
//    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(nrTripsStart + 1, trips.getData().size());
//
//        tripManager.removeTrip(id, null);
//    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(nrTripsStart, trips.getData().size());
//
//        // Note it is hard deleted because the autobook is switched off
//    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, null, null);
//        assertNotNull(trips);
//        assertEquals(nrTripsStart, trips.getData().size());
//    }
//
//    @Test
//    public void testRemoveShoutOutTrip() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(1L);
//    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        int nrTripsStart = trips.getData().size();
//        boolean autobook = true;
//        
//        Trip trip = createShoutOutTrip("2020-01-07T14:30:00+01:00", "2020-01-07T16:30:00+01:00");
//        Long id = tripManager.createTrip(traveller, trip, autobook);
//        assertNotNull(id);
//        
//    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(nrTripsStart + 1, trips.getData().size());
//
//        tripManager.removeTrip(id, null);
//    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(nrTripsStart, trips.getData().size());
//
//        // Note it is hard deleted because it was still in planning status
//    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, null, null);
//        assertNotNull(trips);
//        assertEquals(nrTripsStart, trips.getData().size());
//    }
//
//
//    @Test
//    public void testListTrips() throws Exception {
//    	User traveller = new User();
//    	traveller.setId(2L);
//    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(0, trips.getData().size());
//        
//        Trip trip1 = createShoutOutTrip("2020-01-07T14:30:00+01:00", "2020-01-07T16:30:00+01:00");
//    	Long id1 = tripManager.createTrip(traveller, trip1, true);
//        assertNotNull(id1);
//        
//        Trip trip2 = createShoutOutTrip("2020-01-08T14:30:00+01:00", "2020-01-08T16:30:00+01:00");
//    	Long id2 = tripManager.createTrip(traveller, trip2, true);
//        assertNotNull(id2);
//        
//        Trip trip3 = createSimpleTrip("2020-01-09T14:30:00+01:00", "2020-01-09T16:30:00+01:00");
//        // Make it only soft-deletable. Auto book the trip so it will be scheduled.
//    	Long id3 = tripManager.createTrip(traveller, trip3, true);
//        assertNotNull(id3);
//        tripManager.removeTrip(id3, "Ik ga toch maar niet op reis");
//        
//        // List all non-deleted trips
//    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(2, trips.getData().size());
//        
//    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, null, null);
//        assertNotNull(trips);
//        assertEquals(3, trips.getData().size());
//
//        // Check order on departure time
//        assertEquals(id1, trips.getData().get(0).getId());
//        assertEquals(id2, trips.getData().get(1).getId());
//        assertEquals(id3, trips.getData().get(2).getId());
//
//        // Check explicit sorting on departure time
//    	trips = tripManager.listTrips(traveller, null, null, null, null, SortDirection.ASC, null, null);
//        assertEquals(id1, trips.getData().get(0).getId());
//        assertEquals(id2, trips.getData().get(1).getId());
//    	trips = tripManager.listTrips(traveller, null, null, null, null, SortDirection.DESC, null, null);
//        assertEquals(id2, trips.getData().get(0).getId());
//        assertEquals(id1, trips.getData().get(1).getId());
//        
//    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, 1, 0);
//        assertNotNull(trips);
//        assertEquals(1, trips.getData().size());
//        assertEquals(id1, trips.getData().get(0).getId());
//
//    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, 1, 1);
//        assertNotNull(trips);
//        assertEquals(1, trips.getData().size());
//        assertEquals(id2, trips.getData().get(0).getId());
//
//    	trips = tripManager.listTrips(traveller, TripState.PLANNING, null, null, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(2, trips.getData().size());
//
//    	trips = tripManager.listTrips(traveller, TripState.BOOKING, null, null, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(0, trips.getData().size());
//
//        Instant since = OffsetDateTime.parse("2020-01-08T00:00:00+01:00").toInstant();
//    	trips = tripManager.listTrips(traveller, null, since, null, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(1, trips.getData().size());
//        assertEquals(trips.getData().get(0).getId(), id2);
//
//        Instant until = OffsetDateTime.parse("2020-01-08T00:00:00+01:00").toInstant();
//    	trips = tripManager.listTrips(traveller, null, null, until, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(1, trips.getData().size());
//        assertEquals(trips.getData().get(0).getId(), id1);
//
//        since = OffsetDateTime.parse("2020-01-08T00:00:00+01:00").toInstant();
//        until = OffsetDateTime.parse("2020-01-09T00:00:00+01:00").toInstant();
//    	trips = tripManager.listTrips(traveller, null, since, until, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(1, trips.getData().size());
//        assertEquals(trips.getData().get(0).getId(), id2);
//
//        since = OffsetDateTime.parse("2020-01-08T14:30:00+01:00").toInstant();
//        until = OffsetDateTime.parse("2020-01-08T14:30:00+01:00").toInstant().plusMillis(1);
//    	trips = tripManager.listTrips(traveller, null, since, until, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(1, trips.getData().size());
//        assertEquals(trips.getData().get(0).getId(), id2);
//
//        since = OffsetDateTime.parse("2020-01-08T00:00:00+01:00").toInstant();
//        until = OffsetDateTime.parse("2020-01-08T14:30:00+01:00").toInstant();
//    	trips = tripManager.listTrips(traveller, null, since, until, null, null, null, null);
//        assertNotNull(trips);
//        assertEquals(0, trips.getData().size());
//
//        try {
//        	trips = tripManager.listTrips(traveller, null, null, null, null, null, 101, null);
//        	fail("Expected a BadRequest on maxResults too high");
//        } catch (BadRequestException ex) {
//        	log.debug(ex.toString());
//        }
//        try {
//        	trips = tripManager.listTrips(traveller, null, null, null, null, null, 0, null);
//        	fail("Expected a BadRequest on maxResults too low");
//        } catch (BadRequestException ex) {
//        	log.debug(ex.toString());
//        }
//        try {
//        	trips = tripManager.listTrips(traveller, null, null, null, null, null, 5, -1);
//        	fail("Expected a BadRequest on offset too low");
//        } catch (BadRequestException ex) {
//        	log.debug(ex.toString());
//        }
//
//    }
}
