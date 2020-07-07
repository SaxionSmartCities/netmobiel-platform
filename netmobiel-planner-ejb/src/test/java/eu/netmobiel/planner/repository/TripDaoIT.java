package eu.netmobiel.planner.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.persistence.PersistenceUnitUtil;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Itinerary_;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Leg_;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.Trip_;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.test.Fixture;
import eu.netmobiel.planner.test.PlannerIntegrationTestBase;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@RunWith(Arquillian.class)
public class TripDaoIT  extends PlannerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(TripDao.class)
            ;
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private TripDao tripDao;
    @SuppressWarnings("unused")
	@Inject
    private Logger log;
    
    private User user1;
    private User user2;
    private User user3;
    private TripPlan plan0;
    private TripPlan plan1;
    private TripPlan plan2;
    private TripPlan plan3;
    private Trip trip0;
    private Trip trip1;
    private Trip trip2;
    private Trip trip3;

   
	@Override
    protected void insertData() throws Exception {
        user1 = new User("T1", "Simon1", "Netmobiel");
        user2 = new User("T2", "Simon2", "Netmobiel");
        user3 = new User("T3", "Simon3", "Netmobiel");
    	List<User> users = new ArrayList<>();
    	users.add(user1);
		em.persist(user1);
    	users.add(user2);
		em.persist(user2);
    	users.add(user3);
		em.persist(user3);
    	plan0 = Fixture.createRidesharePlan(user1, "2020-03-19T13:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-19T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:354");
		em.persist(plan0);
    	plan1 = Fixture.createRidesharePlan(user1, "2020-03-20T13:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-20T15:00:00Z", false, 60 * 35, "urn:nb:rs:ride:364");
		em.persist(plan1);
    	plan2 = Fixture.createRidesharePlan(user2, "2020-03-21T15:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-21T17:00:00Z", false, 60 * 35, "urn:nb:rs:ride:374");
		em.persist(plan2);
    	plan3 = Fixture.createRidesharePlan(user1, "2020-03-22T17:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-22T19:00:00Z", false, 60 * 35, "urn:nb:rs:ride:384");
		em.persist(plan3);

		trip0 = Fixture.createTrip(user1, plan0);
		trip0.setDeleted(true);
		trip0.setCancelReason("Sorry, verkeerde dag");
		em.persist(trip0);
		trip1 = Fixture.createTrip(user1, plan1);
		em.persist(trip1);
		trip2 = Fixture.createTrip(user2, plan2);
		em.persist(trip2);
		trip3 = Fixture.createTrip(user3, plan3);
		em.persist(trip3);

	}

	@Test
	public void testLoadTrip_Default() throws Exception {
		Trip trip = em.find(Trip.class, trip1.getId());
		assertNotNull(trip);
		assertEquals(trip.getId(), PlannerUrnHelper.getId(Trip.URN_PREFIX, trip.getTripRef()));
		flush();
		
    	trip = tripDao.find(trip.getId()).orElseThrow(() -> new IllegalStateException("Should have an ID by now"));
		flush();
    	// Check default fetch schema
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertFalse(em.contains(trip));
    	assertNotNull(trip);
    	assertFalse(puu.isLoaded(trip, Trip_.ITINERARY));
    	assertFalse(puu.isLoaded(trip, Trip_.TRAVELLER));
	}

	@Test
	public void testLoadTrip_Detailed() throws Exception {
    	// Check loading of the detailed object 
    	Trip trip = tripDao.loadGraph(trip1.getId(), Trip.DETAILED_ENTITY_GRAPH).orElseThrow(() -> new IllegalStateException("Should have an ID by now"));
    	flush();
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertFalse(em.contains(trip));
    	assertNotNull(trip);
    	assertTrue(puu.isLoaded(trip, Trip_.ITINERARY));
    	assertTrue(puu.isLoaded(trip, Trip_.TRAVELLER));

    	Itinerary it = trip.getItinerary();
    	assertTrue(puu.isLoaded(it, Itinerary_.LEGS));
    	assertFalse(puu.isLoaded(it, Itinerary_.STOPS));
    	assertFalse(puu.isLoaded(it, Itinerary_.TRIP_PLAN));
    	
    	List<Leg> legs = it.getLegs();
    	assertEquals(1, legs.size());
    	Leg leg = legs.get(0);
    	assertTrue(puu.isLoaded(leg, Leg_.FROM));
    	assertTrue(puu.isLoaded(leg, Leg_.TO));
    	assertFalse(puu.isLoaded(leg, Leg_.PLANNER_REPORT));
    	assertTrue(puu.isLoaded(leg, Leg_.GUIDE_STEPS));
	}
	
	@Test
	public void testLoadTrip_MyLegs() throws Exception {
    	// Check loading of the legs-only object, assuming they are mine 
    	Trip trip = tripDao.loadGraph(trip1.getId(), Trip.MY_LEGS_ENTITY_GRAPH).orElseThrow(() -> new IllegalStateException("Should have an ID by now"));
    	flush();
    	assertFalse(em.contains(trip));
    	assertNotNull(trip);
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertTrue(puu.isLoaded(trip, Trip_.ITINERARY));
    	assertFalse(puu.isLoaded(trip, Trip_.TRAVELLER));

    	Itinerary it = trip.getItinerary();
    	assertTrue(puu.isLoaded(it, Itinerary_.LEGS));
    	assertFalse(puu.isLoaded(it, Itinerary_.STOPS));
    	assertFalse(puu.isLoaded(it, Itinerary_.TRIP_PLAN));
    	
    	List<Leg> legs = it.getLegs();
    	assertEquals(1, legs.size());
    	Leg leg = legs.get(0);
    	assertTrue(puu.isLoaded(leg, Leg_.FROM));
    	assertTrue(puu.isLoaded(leg, Leg_.TO));
    	assertFalse(puu.isLoaded(leg, Leg_.PLANNER_REPORT));
    	assertFalse(puu.isLoaded(leg, Leg_.GUIDE_STEPS));
	}

//	@Test
//	public void testSingleReferenceItinerary() throws Exception {
//		try {
//			// An itinerary can only be used by a single trip.
//			Trip trip = createTrip(user1, plan1);
//			em.persist(trip);
//			flush();
//			fail("Expected SQL exception");
//		} catch (Exception ex) {
//			log.info("Anticipated exception: " + ex.toString());
//		}
//	}

    @Test
    public void listTrips_All_Sorting() {
    	User traveller = null;
    	TripState state = null;
    	Instant since = null;
    	Instant until = null; 
		Boolean deletedToo = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertNotNull(tripIds);
    	assertEquals(3, tripIds.getCount());
    	List<Trip> trips = tripDao.fetch(tripIds.getData(), null, Trip::getId);
    	assertNotNull(trips);
    	assertTrue(IntStream
    			.range(0, trips.size() - 1)
    			.allMatch(i -> trips.get(i).getItinerary().getDepartureTime().isBefore(trips.get(i + 1).getItinerary().getDepartureTime()))
    	);
    	
    	sortDirection = SortDirection.ASC;
    	tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	List<Trip> trips2 = tripDao.fetch(tripIds.getData(), null, Trip::getId);
    	assertTrue(IntStream
    			.range(0, trips2.size() - 1)
    			.allMatch(i -> trips2.get(i).getItinerary().getDepartureTime().isBefore(trips2.get(i + 1).getItinerary().getDepartureTime()))
    	);

    	sortDirection = SortDirection.DESC;
    	tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	List<Trip> trips3 = tripDao.fetch(tripIds.getData(), null, Trip::getId);
    	assertTrue(IntStream
    			.range(0, trips3.size() - 1)
    			.allMatch(i -> trips3.get(i).getItinerary().getDepartureTime().isAfter(trips3.get(i + 1).getItinerary().getDepartureTime()))
    	);
    
    }
    
    @Test
    public void listTrips_ByState() throws Exception {
    	User traveller = null;
    	TripState state = TripState.PLANNING;
    	Instant since = null;
    	Instant until = null; 
		Boolean deletedToo = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertNotNull(tripIds);
    	assertEquals(0, tripIds.getCount());
    	state = TripState.SCHEDULED;
    	tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertEquals(3, tripIds.getCount());
    }

    @Test
    public void listTrips_InProgressOnly() {
    	User traveller = null;
    	TripState state = null;
    	Instant since = null;
    	Instant until = null; 
		Boolean deletedToo = true;
		SortDirection sortDirection = null;
    	PagedResult<Long> tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertNotNull(tripIds);
    	assertEquals(4, tripIds.getCount());
    }
    
    @Test
    public void listTrips_ByUser() {
    	User traveller = user1;
    	TripState state = null;
    	Instant since = null;
    	Instant until = null; 
		Boolean deletedToo = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertNotNull(tripIds);
    	assertEquals(1, tripIds.getCount());
    }
    
    @Test
    public void listTrips_Since() {
    	User traveller = null;
    	TripState state = null;
    	Instant since = Instant.parse("2020-03-19T12:00:00Z");
    	Instant until = null; 
		Boolean deletedToo = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertNotNull(tripIds);
    	assertEquals(3, tripIds.getCount());
    	
    	since = Instant.parse("2020-03-20T12:00:00Z");
    	tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertEquals(3, tripIds.getCount());

    	since = Instant.parse("2020-03-22T12:00:00Z");
    	tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertEquals(1, tripIds.getCount());
    }

    @Test
    public void listTrips_Until() {
    	User traveller = null;
    	TripState state = null;
    	Instant since = null;
    	Instant until = Instant.parse("2020-03-19T12:00:00Z"); 
		Boolean deletedToo = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertNotNull(tripIds);
    	assertEquals(0, tripIds.getCount());
    	
    	until = Instant.parse("2020-03-20T12:00:00Z");
    	tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertEquals(0, tripIds.getCount());

    	until = Instant.parse("2020-03-22T12:00:00Z");
    	tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 10, 0);
    	assertEquals(2, tripIds.getCount());
    }


}
