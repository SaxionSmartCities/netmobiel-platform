package eu.netmobiel.opentripplanner.client;


import static org.junit.Assert.*;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.opentripplanner.api.model.Itinerary;
import eu.netmobiel.opentripplanner.api.model.Leg;
import eu.netmobiel.opentripplanner.api.model.PlanResponse;
import eu.netmobiel.opentripplanner.api.model.TraverseMode;
import eu.netmobiel.opentripplanner.api.model.TripPlan;

@RunWith(Arquillian.class)
public class OpenTripPlannerClientIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	File[] deps = Maven.configureResolver()
				.loadPomFromFile("pom.xml")
				.importCompileAndRuntimeDependencies() 
				.resolve()
				.withTransitivity()
				.asFile();
		Archive<?> archive = ShrinkWrap.create(WebArchive.class, "test.war")
       		.addAsLibraries(deps)
            .addPackage(Leg.class.getPackage())
            .addClass(OpenTripPlannerClient.class)
            .addClass(Jackson2ObjectMapperContextResolver.class)
            // Arquillian tests need the beans.xml to recognize it as a CDI application
            .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
            // Take car of removing the default json provider, because we use jackson everywhere (unfortunately).
        	.addAsWebInfResource("jboss-deployment-structure.xml")
        	.addAsResource("log4j.properties");
//		System.out.println(archive.toString(Formatters.VERBOSE));
        return archive;
    }

    @Inject
    private OpenTripPlannerClient client;

    @Inject
    private Logger log;

    private static void assertPlan(GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, TripPlan plan) {
        assertEquals(travelTime, plan.date);
        assertEquals(fromPlace.getLabel(), plan.from.name);
        assertEquals(fromPlace.getLatitude(), plan.from.lat);
        assertEquals(fromPlace.getLongitude(), plan.from.lon);
        assertEquals(toPlace.getLabel(), plan.to.name);
        assertEquals(toPlace.getLatitude(), plan.to.lat);
        assertEquals(toPlace.getLongitude(), plan.to.lon);
    }

//    @InSequence
    @Test
    public void testPlanDeparture() throws Exception {
    	log.debug("testPlanDeparture");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 3;
    	Integer maxTransfers = null;
    	PlanResponse result = client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, null, maxItineraries);
    	TripPlan plan = result.plan;
        assertNotNull(plan);
        assertPlan(fromPlace, toPlace, travelTime, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(travelTime, it.startTime);
        assertEquals(1, it.legs.size());
        Leg leg = it.legs.get(0);
        assertEquals(it.startTime, leg.startTime);
        assertEquals(it.endTime, leg.endTime);
        assertEquals(leg.startTime, leg.from.departure);
        assertEquals(leg.endTime, leg.to.arrival);
    }

    @Test
    public void testPlanArrival() throws Exception {
    	log.debug("testPlanArrival");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    	boolean useTimeAsArriveBy = true;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 3;
    	Integer maxTransfers = null;
    	PlanResponse result = client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, null, maxItineraries);
    	TripPlan plan = result.plan;

    	assertNotNull(plan);
        assertPlan(fromPlace, toPlace, travelTime, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(travelTime, it.endTime);
        assertEquals(1, it.legs.size());
        Leg leg = it.legs.get(0);
        assertEquals(it.startTime, leg.startTime);
        assertEquals(it.endTime, leg.endTime);
        assertEquals(leg.startTime, leg.from.departure);
        assertEquals(leg.endTime, leg.to.arrival);
    }

    @Test
    public void testPlanDepartureVia() throws Exception {
    	log.debug("testPlanDepartureVia");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    	GeoLocation[] via = new GeoLocation[] { GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966") }; 
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 3;
    	Integer maxTransfers = null;
    	PlanResponse result = client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, via, maxItineraries);
    	TripPlan plan = result.plan;

    	assertNotNull(plan);
        assertPlan(fromPlace, toPlace, travelTime, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(travelTime, it.startTime);
        assertEquals(2, it.legs.size());

        Leg leg1 = it.legs.get(0);
        Leg leg2 = it.legs.get(1);
        assertEquals(it.startTime, leg1.startTime);
        assertEquals(it.endTime, leg2.endTime);
        assertEquals(leg1.startTime, leg1.from.departure);
        assertEquals(leg2.endTime, leg2.to.arrival);
        assertEquals(leg1.to.arrival, leg2.from.arrival);
        assertEquals(leg1.to.departure, leg2.from.departure);
    }

    @Test
    public void testPlanArrivalVia() throws Exception {
    	log.debug("testPlanArrivalVia");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    	GeoLocation[] via = new GeoLocation[] { GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966") }; 
    	boolean useTimeAsArriveBy = true;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
  	    Integer maxItineraries = 3;
    	Integer maxTransfers = null;
    	PlanResponse result = client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, via, maxItineraries);
    	TripPlan plan = result.plan;
        assertNotNull(plan);
        assertPlan(fromPlace, toPlace, travelTime, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(travelTime, it.endTime);
        assertEquals(2, it.legs.size());

        Leg leg1 = it.legs.get(0);
        Leg leg2 = it.legs.get(1);
        assertEquals(it.startTime, leg1.startTime);
        assertEquals(it.endTime, leg2.endTime);
        assertEquals(leg1.startTime, leg1.from.departure);
        assertEquals(leg2.endTime, leg2.to.arrival);
        assertEquals(leg1.to.arrival, leg2.from.arrival);
        assertEquals(leg1.to.departure, leg2.from.departure);
    }

    @Test
    public void testPlanDepartureTransit() throws Exception {
    	log.debug("testPlanDepartureTransit");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 1;
    	Integer maxTransfers = null;
    	PlanResponse result = client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, null, maxItineraries);
    	TripPlan plan = result.plan;

    	assertNotNull(plan);
        assertPlan(fromPlace, toPlace, travelTime, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertTrue("Depart at or after indicated time", travelTime.isBefore(it.startTime));
    }

    @Test
    public void testPlanArrivalTransit() throws Exception {
    	log.debug("testPlanArrivalTransit");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    	boolean useTimeAsArriveBy = true;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 1;
    	Integer maxTransfers = null;
    	PlanResponse result = client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, null, maxItineraries);
    	TripPlan plan = result.plan;

    	assertNotNull(plan);
        log.debug(plan.toString());
        assertPlan(fromPlace, toPlace, travelTime, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertTrue("Arrive at or before indicated time", travelTime.isAfter(it.endTime));
    }

    @Test
    public void testPlanArrivalVia2() throws Exception {
    	log.debug("testPlanArrivalVia2");
    	GeoLocation fromPlace = GeoLocation.fromString("Enschede Overijssel::52.223610,6.895510");
    	GeoLocation  toPlace = GeoLocation.fromString("Deventer Overijssel::52.251030,6.159900");
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
//    	GeoLocation[] via = new GeoLocation[] { GeoLocation.fromString("Saxion Gaming::52.219680,6.889550"), GeoLocation.fromString("Deventer::52.254055,6.167655") }; 
    	GeoLocation[] via = new GeoLocation[] { GeoLocation.fromString("Saxion Gaming::52.220,6.889550") };//52.219680 
//    	GeoLocation[] via = new GeoLocation[] { GeoLocation.fromString("Deventer::52.254055,6.167655") }; 
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR /*, TraverseMode.WALK */}; 
    	Integer maxWalkDistance = 1000;
  	    Integer maxItineraries = 1;
    	Integer maxTransfers = null;
    	PlanResponse result = client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, via, maxItineraries);
    	if (result.error != null) {
    		log.error("Planner error: " + result.error.msg);
    	}
    	TripPlan plan = result.plan;
    	// Expect an error due to traverse problems in OTP
        assertNull(plan);
        assertNotNull(result.error);
    }
    
    @Test
    public void testPlanDepartAndArriveSamePlace() throws Exception {
    	log.debug("testPlanDepartAndArriveSamePlace");
    	GeoLocation fromPlace = GeoLocation.fromString("Enschede Overijssel::52.223610,6.895510");
    	GeoLocation  toPlace = fromPlace;
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR /*, TraverseMode.WALK */}; 
    	Integer maxWalkDistance = 1000;
  	    Integer maxItineraries = 1;
    	Integer maxTransfers = null;
 	    try {
  	    	client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, null, maxItineraries);
  	    	fail("Expected NotFoundException");
  	    } catch (eu.netmobiel.commons.exception.NotFoundException ex) {
  	    	log.debug("testPlanDepartAndArriveSamePlace " + ex.toString());
  	    }
    }

    @Test
    public void testPlanArrivalViaAlmostSame() throws Exception {
    	log.debug("testPlanArrivalViaAlmostSame");
    	GeoLocation fromPlace = GeoLocation.fromString("Enschede Overijssel::52.223610,6.895510");
    	GeoLocation  toPlace = GeoLocation.fromString("Deventer Overijssel::52.251030,6.159900");
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    	GeoLocation[] via = new GeoLocation[] { 
    			GeoLocation.fromString("Enschede Overijssel::52.223610,6.895511"),
    			GeoLocation.fromString("Deventer Overijssel::52.251030,6.159901")
    	}; 
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR /*, TraverseMode.WALK */}; 
    	Integer maxWalkDistance = 1000;
  	    Integer maxItineraries = 1;
    	Integer maxTransfers = null;
    	PlanResponse result = client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, via, maxItineraries);
    	TripPlan plan = result.plan;

    	assertNotNull(plan);
        assertPlan(fromPlace, toPlace, travelTime, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(1, it.legs.size());
    }

    @Test
    public void testPlanArrivalViaOneSame() throws Exception {
    	log.debug("testPlanArrivalViaAlmostSame");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	Instant travelTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    	GeoLocation[] via = new GeoLocation[] { 
    			GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517836"),
    			GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966")
    	}; 
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR /*, TraverseMode.WALK */}; 
    	Integer maxWalkDistance = 1000;
  	    Integer maxItineraries = 1;
    	Integer maxTransfers = null;
    	PlanResponse result = client.createPlan(fromPlace, toPlace, travelTime, useTimeAsArriveBy, modes, false, maxWalkDistance, maxTransfers, via, maxItineraries);
    	TripPlan plan = result.plan;

    	assertNotNull(plan);
        assertPlan(fromPlace, toPlace, travelTime, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(2, it.legs.size());
    }
}
