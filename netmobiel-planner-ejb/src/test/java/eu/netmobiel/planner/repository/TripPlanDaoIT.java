package eu.netmobiel.planner.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripPlan_;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.test.Fixture;
import eu.netmobiel.planner.test.PlannerIntegrationTestBase;

@RunWith(Arquillian.class)
public class TripPlanDaoIT extends PlannerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(TripPlanDao.class)
            ;
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private TripPlanDao tripPlanDao;
    @Inject
    private Logger log;
    

    private PlannerUser simon1;
    private PlannerUser simon2;
    private PlannerUser simon3;
    private PlannerUser carla1;

    
	@Override
    protected void insertData() throws Exception {
        simon1 = new PlannerUser("T1", "Simon1", "Netmobiel");
        simon2 = new PlannerUser("T2", "Simon2", "Netmobiel");
        simon3 = new PlannerUser("T3", "Simon3", "Netmobiel");
        carla1 = new PlannerUser("T4", "Carla1", "Netmobiel");	// Chauffeur
    	List<PlannerUser> users = new ArrayList<>();
    	users.add(simon1);
    	users.add(simon2);
    	users.add(simon3);
    	users.add(carla1);
        for (PlannerUser user : users) {
			em.persist(user);
		}
    	List<TripPlan> plans = new ArrayList<>();
    	plans.add(Fixture.createShoutOutTripPlan(simon1, "2020-03-18T10:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-19T13:00:00Z", false, 24 * 60 * 60 * 3600L));
    	plans.add(Fixture.createShoutOutTripPlan(simon1, "2020-03-20T10:00:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-03-21T13:00:00Z", false, null));
    	plans.add(Fixture.createShoutOutTripPlan(simon2, "2020-03-21T09:00:00Z", Fixture.placeZieuwent, Fixture.placeRaboZutphen, "2020-03-21T14:00:00Z", false, null));
    	plans.add(Fixture.createShoutOutTripPlan(simon3, "2020-03-21T09:30:00Z", Fixture.placeSlingeland, Fixture.placeRaboZutphen, "2020-03-21T10:00:00Z", false, null));
        for (TripPlan t : plans) {
			em.persist(t);
		}
    }

    @Test
    public void saveLargeTripPlan() {
    	try {
	    	TripPlan plan = Fixture.createTransitPlan(simon1);
	    	tripPlanDao.save(plan); 
	    	flush();
	    	plan = tripPlanDao.find(plan.getId()).orElseThrow(() -> new IllegalStateException("Should have an ID by now"));
	    	flush();
	    	// Check default fetch schema
	    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
	    	assertFalse(em.contains(plan));
	    	assertNotNull(plan);
	    	assertFalse(puu.isLoaded(plan, TripPlan_.ITINERARIES));
	    	assertFalse(puu.isLoaded(plan, TripPlan_.PLANNER_REPORTS));
	    	assertFalse(puu.isLoaded(plan, TripPlan_.TRAVELLER));
	    	assertFalse(puu.isLoaded(plan, TripPlan_.TRAVERSE_MODES));
	    	
	    	// Check loading of the detailed object 
	    	plan = tripPlanDao.loadGraph(plan.getId(), TripPlan.DETAILED_ENTITY_GRAPH).orElseThrow(() -> new IllegalStateException("Should have an ID by now"));
	    	flush();
	    	assertFalse(em.contains(plan));
	    	assertNotNull(plan);
	    	assertTrue(puu.isLoaded(plan, TripPlan_.ITINERARIES));
	    	assertFalse(puu.isLoaded(plan, TripPlan_.PLANNER_REPORTS));
	    	assertTrue(puu.isLoaded(plan, TripPlan_.TRAVELLER));
	    	assertTrue(puu.isLoaded(plan, TripPlan_.TRAVERSE_MODES));

	    	Collection<Itinerary> its = plan.getItineraries();
	    	assertEquals(1, its.size());
	    	Itinerary it = its.iterator().next();
	    	assertTrue(puu.isLoaded(it, Itinerary_.LEGS));
	    	assertFalse(puu.isLoaded(it, Itinerary_.STOPS));
	    	assertTrue(puu.isLoaded(it, Itinerary_.TRIP_PLAN)); 	// Apparently implicitly loaded, because this field refers to the loaded plan
	    	
	    	List<Leg> legs = it.getLegs();
	    	assertEquals(5, legs.size());
	    	Leg leg = legs.get(2);	// A large one with guide steps
	    	assertTrue(puu.isLoaded(leg, Leg_.FROM));
	    	assertTrue(puu.isLoaded(leg, Leg_.TO));
	    	assertFalse(puu.isLoaded(leg, Leg_.PLANNER_REPORT));
	    	assertFalse(puu.isLoaded(leg, Leg_.GUIDE_STEPS));

	    	// Check loading of the shout-out plans 
	    	plan = tripPlanDao.loadGraph(plan.getId(), TripPlan.SHOUT_OUT_ENTITY_GRAPH).orElseThrow(() -> new IllegalStateException("Should have an ID by now"));
	    	flush();
	    	assertFalse(em.contains(plan));
	    	assertNotNull(plan);
	    	assertTrue(puu.isLoaded(plan, TripPlan_.ITINERARIES));
	    	assertFalse(puu.isLoaded(plan, TripPlan_.PLANNER_REPORTS));
	    	assertTrue(puu.isLoaded(plan, TripPlan_.TRAVELLER));
	    	assertTrue(puu.isLoaded(plan, TripPlan_.TRAVERSE_MODES));
	    } catch (Exception ex) {
    		log.error("Unexpected exception", ex);
    		fail("Unexpected exception: " + ex.toString());
    	}
    }

    @Test
    public void listTripPlans_All_Sorting() {
    	PlannerUser traveller = null;
    	PlanType planType = null;
    	Instant since = null;
    	Instant until = null; 
		Boolean inProgressOnly = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertNotNull(planIds);
    	assertEquals(4, planIds.getCount());
    	List<TripPlan> plans = tripPlanDao.loadGraphs(planIds.getData(), null, TripPlan::getId);
    	assertNotNull(plans);
    	assertTrue(IntStream
    			.range(0, plans.size() - 1)
    			.allMatch(i -> plans.get(i).getTravelTime().isBefore(plans.get(i + 1).getTravelTime()))
    	);
    	
    	sortDirection = SortDirection.ASC;
    	planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	List<TripPlan> plans2 = tripPlanDao.loadGraphs(planIds.getData(), null, TripPlan::getId);
    	assertTrue(IntStream
    			.range(0, plans2.size() - 1)
    			.allMatch(i -> plans2.get(i).getTravelTime().isBefore(plans2.get(i + 1).getTravelTime()))
    	);

    	sortDirection = SortDirection.DESC;
    	planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	List<TripPlan> plans3 = tripPlanDao.loadGraphs(planIds.getData(), null, TripPlan::getId);
    	assertTrue(IntStream
    			.range(0, plans3.size() - 1)
    			.allMatch(i -> plans3.get(i).getTravelTime().isAfter(plans3.get(i + 1).getTravelTime()))
    	);
    
    }

    @Test
    public void listTripPlans_InProgressOnly() {
    	PlannerUser traveller = null;
    	PlanType planType = null;
    	Instant since = null;
    	Instant until = null; 
		Boolean inProgressOnly = true;
		SortDirection sortDirection = null;
    	PagedResult<Long> planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertNotNull(planIds);
    	assertEquals(3, planIds.getCount());
    }
    
    @Test
    public void listTripPlans_ByUser() {
    	PlannerUser traveller = simon1;
    	PlanType planType = null;
    	Instant since = null;
    	Instant until = null; 
		Boolean inProgressOnly = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertNotNull(planIds);
    	assertEquals(2, planIds.getCount());
    }
    
    @Test
    public void listTripPlans_ByUserSince() {
    	PlannerUser traveller = simon1;
    	PlanType planType = null;
    	Instant since = Instant.parse("2020-03-19T12:00:00Z");
    	Instant until = null; 
		Boolean inProgressOnly = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertNotNull(planIds);
    	assertEquals(2, planIds.getCount());
    	
    	since = Instant.parse("2020-03-20T12:00:00Z");
    	planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertEquals(1, planIds.getCount());

    	since = Instant.parse("2020-03-22T12:00:00Z");
    	planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertEquals(0, planIds.getCount());
    }

    @Test
    public void listTripPlans_ByUserUntil() {
    	PlannerUser traveller = simon1;
    	PlanType planType = null;
    	Instant since = null;
    	Instant until = Instant.parse("2020-03-19T12:00:00Z"); 
		Boolean inProgressOnly = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertNotNull(planIds);
    	assertEquals(0, planIds.getCount());
    	
    	until = Instant.parse("2020-03-20T12:00:00Z");
    	planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertEquals(1, planIds.getCount());

    	until = Instant.parse("2020-03-22T12:00:00Z");
    	planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertEquals(2, planIds.getCount());
    }

    @Test
    public void listTripPlans_ByType() throws Exception {
    	TripPlan plan = Fixture.createTransitPlan(simon1);
    	tripPlanDao.save(plan); 
    	flush();
    	PlannerUser traveller = null;
    	PlanType planType = PlanType.REGULAR;
    	Instant since = null;
    	Instant until = null; 
		Boolean inProgressOnly = null;
		SortDirection sortDirection = null;
    	PagedResult<Long> planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertNotNull(planIds);
    	assertEquals(1, planIds.getCount());

    	planType = PlanType.SHOUT_OUT;
    	planIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 10, 0);
    	assertEquals(4, planIds.getCount());

    }
    
    @Test
    public void listShoutOutTripPlans() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T09:00:00Z").toInstant();
    	PagedResult<Long> planIds = tripPlanDao.findShoutOutPlans(carla1, Fixture.placeZieuwentRKKerk, startTime, 10000, 50000, 0, 0);
    	assertNotNull(planIds);
    	assertEquals(2, planIds.getTotalCount().intValue());
    	planIds = tripPlanDao.findShoutOutPlans(carla1, Fixture.placeZieuwentRKKerk, startTime, 10000, 50000, 10, 0);
    	
//    	log.info("Found " + planIds.getCount() + " trips");
//    	List<Trip> trips= tripDao.fetch(planIds.getData(), Trip.LIST_TRIPS_ENTITY_GRAPH);
//    	for (Trip t : trips) {
//			log.info(t.toString());
//		}
    	
    }

    @Test
    public void listShoutOutTripPlansStartTime() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T15:00:00Z").toInstant();
    	PagedResult<Long> planIds = tripPlanDao.findShoutOutPlans(carla1, Fixture.placeZieuwentRKKerk, startTime, 10000, 50000, 0, 0);
    	assertNotNull(planIds);
    	assertEquals(0, planIds.getTotalCount().intValue());
    }

    @Test
    public void listShoutOutTripPlansStartTime2() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T13:30:00Z").toInstant();
    	PagedResult<Long> planIds = tripPlanDao.findShoutOutPlans(carla1, Fixture.placeZieuwentRKKerk, startTime, 10000, 50000, 0, 0);
    	assertNotNull(planIds);
    	assertEquals(1, planIds.getTotalCount().intValue());
    }

    @Test
    public void listShoutOutTripPlansAll() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T09:00:00Z").toInstant();
    	PagedResult<Long> planIds = tripPlanDao.findShoutOutPlans(carla1, Fixture.placeZieuwentRKKerk, startTime, 50000, 50000, 0, 0);
    	assertNotNull(planIds);
    	assertEquals(3, planIds.getTotalCount().intValue());
    }

    @Test
    public void listShoutOutTripPlansAll_NotMine() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T09:00:00Z").toInstant();
    	PagedResult<Long> planIds = tripPlanDao.findShoutOutPlans(simon1, Fixture.placeZieuwentRKKerk, startTime, 50000, 50000, 0, 0);
    	assertNotNull(planIds);
    	assertEquals(2, planIds.getTotalCount().intValue());
    	
    }

    @Test
    public void listShoutOutTripPlansNearby() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T09:00:00Z").toInstant();
    	PagedResult<Long> planIds = tripPlanDao.findShoutOutPlans(carla1, Fixture.placeZieuwentRKKerk, startTime, 1000, 50000, 0, 0);
    	assertNotNull(planIds);
    	assertEquals(2, planIds.getTotalCount().intValue());
    }

    @Test
    public void listShoutOutTripPlansNearby2() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T09:00:00Z").toInstant();
    	PagedResult<Long> planIds = tripPlanDao.findShoutOutPlans(carla1, Fixture.placeZieuwentRKKerk, startTime, 1000, 20000, 0, 0);
    	assertNotNull(planIds);
    	assertEquals(1, planIds.getTotalCount().intValue());
    }
    
    
}
