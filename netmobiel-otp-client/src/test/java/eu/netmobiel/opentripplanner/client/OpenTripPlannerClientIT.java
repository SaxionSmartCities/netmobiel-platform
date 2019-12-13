package eu.netmobiel.opentripplanner.client;


import static org.junit.Assert.*;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

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
import eu.netmobiel.opentripplanner.api.model.TraverseMode;
import eu.netmobiel.opentripplanner.api.model.TripPlan;

@RunWith(Arquillian.class)
public class OpenTripPlannerClientIT {
    @Deployment
    public static Archive<?> createTestArchive() {
//    	File[] deps = Maven.configureResolver()
//				.loadPomFromFile("pom.xml")
//				.importCompileAndRuntimeDependencies() 
//				.resolve()
//				.withTransitivity()
//				.asFile();
		File[] deps2 = Maven.configureResolver()
//				.workOffline()
				.loadPomFromFile("pom.xml")
				.resolve("eu.netmobiel:commons")
				.withTransitivity()
				.asFile();
		Archive<?> archive = ShrinkWrap.create(WebArchive.class, "test.war")
//       		.addAsLibraries(deps)
       		.addAsLibraries(deps2)
            .addPackage(Leg.class.getPackage())
            .addClass(OpenTripPlannerClient.class)
            .addClass(Jackson2ObjectMapperContextResolver.class)
            .addClass(Resources.class)
            // Arquillian tests need the beans.xml to recognize it as a CDI application
            .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
            // Take car of removing the default json provider, because we use jackson everywhere (unfortunately).
        	.addAsWebInfResource("jboss-deployment-structure.xml")
        	.addAsResource("log4j.properties");
//        log.debug(archive.toString(Formatters.VERBOSE));
        return archive;
    }

    @Inject
    private OpenTripPlannerClient client;

    @Inject
    private Logger log;

    private void assertPlan(GeoLocation fromPlace, GeoLocation toPlace, LocalDate date, LocalTime time, TripPlan plan) {
        assertEquals(date, plan.date.atZone(ZoneId.systemDefault()).toLocalDate());
        assertEquals(time, plan.date.atZone(ZoneId.systemDefault()).toLocalTime());
        assertEquals(fromPlace.getLabel(), plan.from.name);
        assertEquals(fromPlace.getLatitude(), plan.from.lat);
        assertEquals(fromPlace.getLongitude(), plan.from.lon);
        assertEquals(toPlace.getLabel(), plan.to.name);
        assertEquals(toPlace.getLatitude(), plan.to.lat);
        assertEquals(toPlace.getLongitude(), plan.to.lon);
    }
    @Test
    public void testPlanDeparture() throws Exception {
    	log.debug("testPlanDeparture");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.parse("2019-12-01");
    	LocalTime time = LocalTime.parse("20:00:00");
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 3;
    	TripPlan plan = client.createPlan(fromPlace, toPlace, date, time, useTimeAsArriveBy, modes, false, maxWalkDistance, null, maxItineraries);
    	
        assertNotNull(plan);
        log.debug(plan.toString());
        assertPlan(fromPlace, toPlace, date, time, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(time, it.startTime.atZone(ZoneId.systemDefault()).toLocalTime());
        assertEquals(1, it.legs.size());
    }

    @Test
    public void testPlanArrival() throws Exception {
    	log.debug("testPlanArrival");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.parse("2019-12-01");
    	LocalTime time = LocalTime.parse("20:00:00");
    	boolean useTimeAsArriveBy = true;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 3;
    	TripPlan plan = client.createPlan(fromPlace, toPlace, date, time, useTimeAsArriveBy, modes, false, maxWalkDistance, null, maxItineraries);

    	assertNotNull(plan);
        log.debug(plan.toString());
        assertPlan(fromPlace, toPlace, date, time, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(time, it.endTime.atZone(ZoneId.systemDefault()).toLocalTime());
        assertEquals(1, it.legs.size());
    }

    @Test
    public void testPlanDepartureVia() throws Exception {
    	log.debug("testPlanDepartureVia");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.parse("2019-12-01");
    	LocalTime time = LocalTime.parse("20:00:00");
    	GeoLocation[] via = new GeoLocation[] { GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966") }; 
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 3;
    	TripPlan plan = client.createPlan(fromPlace, toPlace, date, time, useTimeAsArriveBy, modes, false, maxWalkDistance, via, maxItineraries);

    	assertNotNull(plan);
        log.debug(plan.toString());
        assertPlan(fromPlace, toPlace, date, time, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(time, it.startTime.atZone(ZoneId.systemDefault()).toLocalTime());
        assertEquals(2, it.legs.size());
    }

    @Test
    public void testPlanArrivalVia() throws Exception {
    	log.debug("testPlanArrivalVia");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.parse("2019-12-01");
    	LocalTime time = LocalTime.parse("20:00:00");
    	GeoLocation[] via = new GeoLocation[] { GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966") }; 
    	boolean useTimeAsArriveBy = true;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 3;
    	TripPlan plan = client.createPlan(fromPlace, toPlace, date, time, useTimeAsArriveBy, modes, false, maxWalkDistance, via, maxItineraries);
        assertNotNull(plan);
        log.debug(plan.toString());
        assertPlan(fromPlace, toPlace, date, time, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertEquals(time, it.endTime.atZone(ZoneId.systemDefault()).toLocalTime());
        assertEquals(2, it.legs.size());
    }

    @Test
    public void testPlanDepartureTransit() throws Exception {
    	log.debug("testPlanDepartureTransit");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.parse("2019-12-01");
    	LocalTime time = LocalTime.parse("20:00:00");
    	boolean useTimeAsArriveBy = false;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 1;
    	TripPlan plan = client.createPlan(fromPlace, toPlace, date, time, useTimeAsArriveBy, modes, false, maxWalkDistance, null, maxItineraries);

    	assertNotNull(plan);
        log.debug(plan.toString());
        assertPlan(fromPlace, toPlace, date, time, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertTrue("Depart at or after indicated time", time.isBefore(it.startTime.atZone(ZoneId.systemDefault()).toLocalTime()));
    }

    @Test
    public void testPlanArrivalTransit() throws Exception {
    	log.debug("testPlanArrivalTransit");
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	LocalDate date = LocalDate.parse("2019-12-01");
    	LocalTime time = LocalTime.parse("20:00:00");
    	boolean useTimeAsArriveBy = true;
    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK }; 
    	Integer maxWalkDistance = 2000;
    	Integer maxItineraries = 1;
    	TripPlan plan = client.createPlan(fromPlace, toPlace, date, time, useTimeAsArriveBy, modes, false, maxWalkDistance, null, maxItineraries);

    	assertNotNull(plan);
        log.debug(plan.toString());
        assertPlan(fromPlace, toPlace, date, time, plan);
        assertEquals(1, plan.itineraries.size());
        Itinerary it = plan.itineraries.get(0);
        assertTrue("Arrive at or before indicated time", time.isAfter(it.endTime.atZone(ZoneId.systemDefault()).toLocalTime()));
    }
}
