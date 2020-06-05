package eu.netmobiel.planner.repository;


import static org.junit.Assert.*;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.Resources;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.converter.TripStateConverter;
import eu.netmobiel.planner.repository.helper.WithinPredicate;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@RunWith(Arquillian.class)
public class TripDaoIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	File[] deps = Maven.configureResolver()
				.loadPomFromFile("pom.xml")
				.importCompileAndRuntimeDependencies() 
				.resolve()
				.withTransitivity()
				.asFile();
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(deps)
                .addPackages(true, PlannerUrnHelper.class.getPackage())
                .addPackages(true, Trip.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, TripStateConverter.class.getPackage())
                .addPackages(true, WithinPredicate.class.getPackage())
            .addClass(TripDao.class)
            .addClass(Resources.class)
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private TripDao tripDao;

    @PersistenceContext(unitName = "pu-planner")
    private EntityManager em;
    
    @Inject
    private UserTransaction utx;
    
    @Inject
    private Logger log;
    
	private static GeoLocation placeZieuwent = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
	private static GeoLocation placeSlingeland = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
	private static GeoLocation placeRaboZuthphen = GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966");
	private static GeoLocation placeZieuwentRKKerk = GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542");
	@Before
    public void preparePersistenceTest() throws Exception {
        clearData();
        insertData();
        startTransaction();
    }
    
    private void clearData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Dumping old records...");
        em.createQuery("delete from Trip").executeUpdate();
        em.createQuery("delete from User").executeUpdate();
        utx.commit();
    }

    private Trip createEmptyTrip(User traveller, GeoLocation from, String departureTimeIso, GeoLocation to, String arrivalTimeIso) {
        Trip trip = new Trip();
    	Instant departureTime = OffsetDateTime.parse(departureTimeIso).toInstant();
    	Instant arrivalTime = OffsetDateTime.parse(arrivalTimeIso).toInstant();
//    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	trip.setTraveller(traveller);
    	trip.setFrom(from);
    	trip.setTo(to);
    	trip.setState(TripState.PLANNING);
    	trip.setDepartureTime(departureTime);
    	trip.setArrivalTime(arrivalTime);
    	trip.setDuration(Math.toIntExact(Duration.between(departureTime, arrivalTime).getSeconds()));
    	return trip;
    }
    
    private void insertData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Inserting records...");
        User t1 = new User("T1", "Simon1", "Netmobiel");
        User t2 = new User("T2", "Simon2", "Netmobiel");
        User t3 = new User("T3", "Simon3", "Netmobiel");
    	List<User> users = new ArrayList<>();
    	users.add(t1);
    	users.add(t2);
    	users.add(t3);
        for (User user : users) {
			em.persist(user);
		}
    	List<Trip> trips = new ArrayList<>();
    	trips.add(createEmptyTrip(t1, placeZieuwent, "2020-03-21T13:00:00Z", placeSlingeland, "2020-03-21T15:00:00Z"));
    	trips.add(createEmptyTrip(t2, placeZieuwent, "2020-03-21T14:00:00Z", placeRaboZuthphen, "2020-03-21T16:00:00Z"));
    	trips.add(createEmptyTrip(t3, placeSlingeland, "2020-03-21T10:00:00Z", placeRaboZuthphen, "2020-03-21T12:00:00Z"));
        for (Trip t : trips) {
			em.persist(t);
		}
        utx.commit();
        // clear the persistence context (first-level cache)
        em.clear();
    }

    private void startTransaction() throws Exception {
        utx.begin();
        em.joinTransaction();
    }

    @After
    public void commitTransaction() throws Exception {
        utx.commit();
    }
    
//    private List<Trip> findTrips(String sender) {
//        log.debug("Selecting (using JPQL)...");
//        List<Message> messages = em.createQuery(
//        		"select m from Message m where m.sender.managedIdentity = :sender order by m.creationTime desc",
//        		Message.class)
//        		.setParameter("sender", sender)
//        		.getResultList();
//        log.debug("Found " + messages.size() + " messages (using JPQL)");
//        return messages;
//    }

    @Test
    public void listShoutOutTrips() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T09:00:00Z").toInstant();
    	PagedResult<Long> tripIds = tripDao.findShoutOutTrips(placeZieuwentRKKerk, startTime, 10000, 50000, 0, 0);
    	assertNotNull(tripIds);
    	assertEquals(2, tripIds.getTotalCount().intValue());
    	tripIds = tripDao.findShoutOutTrips(placeZieuwentRKKerk, startTime, 10000, 50000, 10, 0);
    	
//    	log.info("Found " + tripIds.getCount() + " trips");
//    	List<Trip> trips= tripDao.fetch(tripIds.getData(), Trip.LIST_TRIPS_ENTITY_GRAPH);
//    	for (Trip t : trips) {
//			log.info(t.toString());
//		}
    	
    }
    
    @Test
    public void listShoutOutTripsStartTime() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T15:00:00Z").toInstant();
    	PagedResult<Long> tripIds = tripDao.findShoutOutTrips(placeZieuwentRKKerk, startTime, 10000, 50000, 0, 0);
    	assertNotNull(tripIds);
    	assertEquals(0, tripIds.getTotalCount().intValue());
    }

    @Test
    public void listShoutOutTripsStartTime2() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T13:30:00Z").toInstant();
    	PagedResult<Long> tripIds = tripDao.findShoutOutTrips(placeZieuwentRKKerk, startTime, 10000, 50000, 0, 0);
    	assertNotNull(tripIds);
    	assertEquals(1, tripIds.getTotalCount().intValue());
    }
    @Test
    public void listShoutOutTripsAll() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T09:00:00Z").toInstant();
    	PagedResult<Long> tripIds = tripDao.findShoutOutTrips(placeZieuwentRKKerk, startTime, 50000, 50000, 0, 0);
    	assertNotNull(tripIds);
    	assertEquals(3, tripIds.getTotalCount().intValue());
    }
    @Test
    public void listShoutOutTripsNearby() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T09:00:00Z").toInstant();
    	PagedResult<Long> tripIds = tripDao.findShoutOutTrips(placeZieuwentRKKerk, startTime, 1000, 50000, 0, 0);
    	assertNotNull(tripIds);
    	assertEquals(2, tripIds.getTotalCount().intValue());
    }

    @Test
    public void listShoutOutTripsNearby2() {
    	final Instant startTime = OffsetDateTime.parse("2020-03-21T09:00:00Z").toInstant();
    	PagedResult<Long> tripIds = tripDao.findShoutOutTrips(placeZieuwentRKKerk, startTime, 1000, 20000, 0, 0);
    	assertNotNull(tripIds);
    	assertEquals(1, tripIds.getTotalCount().intValue());
    }
}
