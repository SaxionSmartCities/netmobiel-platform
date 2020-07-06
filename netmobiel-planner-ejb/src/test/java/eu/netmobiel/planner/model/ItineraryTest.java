package eu.netmobiel.planner.model;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.test.Fixture;

public class ItineraryTest {
    private static final Logger log = LoggerFactory.getLogger(ItineraryTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAppend() {
		Leg leg1 = new Leg();
		Stop stop1A = new Stop(Fixture.placeThuisLichtenvoorde, Instant.parse("2020-07-03T14:00:00Z"), null);
		Stop stop1B = new Stop(Fixture.placeZieuwentRKKerk, null, Instant.parse("2020-07-03T14:30:00Z"));
		leg1.setFrom(stop1A);
		leg1.setTo(stop1B);
		leg1.setTraverseMode(TraverseMode.CAR);
		leg1.setDistance(15000);
		leg1.setDuration(Math.toIntExact(Duration.between(stop1A.getDepartureTime(), stop1B.getArrivalTime()).getSeconds()));

		Itinerary it1 = new Itinerary();
		it1.getLegs().add(leg1);
		it1.getStops().add(stop1A);
		it1.getStops().add(stop1B);
		it1.updateCharacteristics();
		log.debug("testAppend - Itinerary 1: " + it1.toString());

		Leg leg2 = new Leg();
		Stop stop2A = new Stop(Fixture.placeZieuwentRKKerk, Instant.parse("2020-07-03T14:35:00Z"), null);
		Stop stop2B = new Stop(Fixture.placeRaboZutphen, null, Instant.parse("2020-07-03T15:15:00Z"));
		leg2.setFrom(stop2A);
		leg2.setTo(stop2B);
		leg2.setTraverseMode(TraverseMode.CAR);
		leg2.setDistance(32000);
		leg2.setDuration(Math.toIntExact(Duration.between(stop2A.getDepartureTime(), stop2B.getArrivalTime()).getSeconds()));

		Itinerary it2 = new Itinerary();
		it2.getLegs().add(leg2);
		it2.getStops().add(stop2A);
		it2.getStops().add(stop2B);
		it2.updateCharacteristics();
		log.debug("testAppend - Itinerary 2: " + it2.toString());
		
		Itinerary it3 = it1.append(it2);
		assertEquals(2, it3.getLegs().size());
		assertEquals(3, it3.getStops().size());
		log.debug("testAppend - Itinerary 3: " + it3.toString());
		
		Leg leg3_1 = it3.getLegs().get(0);
		assertEquals(stop1A, leg3_1.getFrom());
		assertNotSame(stop1B, leg3_1.getTo());
		// The appended itinerary is modified to reflect the new situation
		assertEquals(stop2A.getArrivalTime(), leg3_1.getTo().getArrivalTime());
		Leg leg3_2 = it3.getLegs().get(1);
		assertSame(leg3_1.getTo(), leg3_2.getFrom());
	}

	@Test
	public void testPrepend() {
		Leg leg1 = new Leg();
		Stop stop1A = new Stop(Fixture.placeThuisLichtenvoorde, Instant.parse("2020-07-03T14:00:00Z"), null);
		Stop stop1B = new Stop(Fixture.placeZieuwentRKKerk, null, Instant.parse("2020-07-03T14:30:00Z"));
		leg1.setFrom(stop1A);
		leg1.setTo(stop1B);
		leg1.setTraverseMode(TraverseMode.CAR);
		leg1.setDistance(15000);
		leg1.setDuration(Math.toIntExact(Duration.between(stop1A.getDepartureTime(), stop1B.getArrivalTime()).getSeconds()));

		Itinerary it1 = new Itinerary();
		it1.getLegs().add(leg1);
		it1.getStops().add(stop1A);
		it1.getStops().add(stop1B);
		it1.updateCharacteristics();
		log.debug("testPrepend - Itinerary 1: " + it1.toString());

		Leg leg2 = new Leg();
		Stop stop2A = new Stop(Fixture.placeZieuwentRKKerk, Instant.parse("2020-07-03T14:35:00Z"), null);
		Stop stop2B = new Stop(Fixture.placeRaboZutphen, null, Instant.parse("2020-07-03T15:15:00Z"));
		leg2.setFrom(stop2A);
		leg2.setTo(stop2B);
		leg2.setTraverseMode(TraverseMode.CAR);
		leg2.setDistance(32000);
		leg2.setDuration(Math.toIntExact(Duration.between(stop2A.getDepartureTime(), stop2B.getArrivalTime()).getSeconds()));

		Itinerary it2 = new Itinerary();
		it2.getLegs().add(leg2);
		it2.getStops().add(stop2A);
		it2.getStops().add(stop2B);
		it2.updateCharacteristics();
		log.debug("testPrepend - Itinerary 2: " + it2.toString());

		Itinerary it3 = it2.prepend(it1);
		assertEquals(2, it3.getLegs().size());
		assertEquals(3, it3.getStops().size());
		log.debug("testPrepend - Itinerary 3: " + it3.toString());
		
		Leg leg3_1 = it3.getLegs().get(0);
		Leg leg3_2 = it3.getLegs().get(1);
		assertEquals(stop1A, leg3_1.getFrom());
		assertSame(stop1B, leg3_1.getTo());
		// The prepended itinerary is modified to reflect the new situation
		assertEquals(stop1B.getArrivalTime(), leg3_2.getFrom().getArrivalTime());
		assertSame(leg3_1.getTo(), leg3_2.getFrom());
	}

	@Test
	public void testshiftItinerayTimimg() {
		Itinerary it = new Itinerary();
    	it.setStops(new ArrayList<>());
    	it.setLegs(new ArrayList<>());

    	Stop stop1 = new Stop(Fixture.placeThuisLichtenvoorde);
    	stop1.setDepartureTime(Instant.parse("2020-07-06T14:00:00Z"));
    	it.getStops().add(stop1);
    	
    	Stop stop2 = new Stop(Fixture.placeZieuwent);
    	stop2.setArrivalTime(Instant.parse("2020-07-06T14:15:00Z"));
    	stop2.setDepartureTime(stop2.getArrivalTime().plusSeconds(5 * 60));
    	it.getStops().add(stop2);

    	Stop stop3 = new Stop(Fixture.placeRaboZutphen);
    	stop3.setArrivalTime(Instant.parse("2020-07-06T15:00:00Z"));
    	stop3.setDepartureTime(stop3.getArrivalTime().plusSeconds(5 * 60));
    	it.getStops().add(stop3);

    	Stop stop4 = new Stop(Fixture.placeRozenkwekerijZutphen);
    	stop4.setArrivalTime(Instant.parse("2020-07-06T15:15:00Z"));
    	it.getStops().add(stop4);

    	Leg leg1 = new Leg();
    	it.getLegs().add(leg1);
    	leg1.setDistance(10000);
    	leg1.setDuration(Math.toIntExact(Duration.between(stop1.getDepartureTime(), stop2.getArrivalTime()).getSeconds()));
    	leg1.setFrom(stop1);		
    	leg1.setTo(stop2);

    	Leg leg2 = new Leg();
    	it.getLegs().add(leg2);
    	leg2.setDistance(30000);
    	leg2.setDuration(Math.toIntExact(Duration.between(stop2.getDepartureTime(), stop3.getArrivalTime()).getSeconds()));
    	leg2.setFrom(stop2);		
    	leg2.setTo(stop3);

    	Leg leg3 = new Leg();
    	it.getLegs().add(leg3);
    	leg3.setDistance(10000);
    	leg3.setDuration(Math.toIntExact(Duration.between(stop3.getDepartureTime(), stop4.getArrivalTime()).getSeconds()));
    	leg3.setFrom(stop3);		
    	leg3.setTo(stop4);

    	it.getLegs().forEach(leg -> leg.setTraverseMode(TraverseMode.CAR));
    	it.updateCharacteristics();

    	assertEquals(stop1.getDepartureTime(), it.getDepartureTime());
    	assertEquals(stop4.getArrivalTime(), it.getArrivalTime());
    	log.debug("testshiftItinerayTimimg before: " + it.toString());
    	boolean useAsArrivalTime = false;
    	GeoLocation refLoc = useAsArrivalTime ? stop3.getLocation() : stop2.getLocation();
    	Instant targetTime = Instant.parse("2020-07-06T15:00:00Z");
    	it.shiftItineraryTiming(refLoc, targetTime, useAsArrivalTime);
    	log.debug("testshiftItinerayTimimg after 1: " + it.toString());
    	assertEquals(targetTime, stop2.getDepartureTime());
    	assertEquals(stop1.getDepartureTime(), it.getDepartureTime());
    	assertEquals(stop4.getArrivalTime(), it.getArrivalTime());
    	
    	useAsArrivalTime = true;
    	refLoc = useAsArrivalTime ? stop3.getLocation() : stop2.getLocation();
    	targetTime = Instant.parse("2020-07-06T16:00:00Z");
    	it.shiftItineraryTiming(refLoc, targetTime, useAsArrivalTime);
    	log.debug("testshiftItinerayTimimg after 2: " + it.toString());
    	assertEquals(targetTime, stop3.getArrivalTime());
    	assertEquals(stop1.getDepartureTime(), it.getDepartureTime());
    	assertEquals(stop4.getArrivalTime(), it.getArrivalTime());
    	
    	targetTime = Instant.parse("2020-07-06T14:00:00Z");
    	it.shiftItineraryTiming(refLoc, targetTime, useAsArrivalTime);
    	log.debug("testshiftItinerayTimimg after 2: " + it.toString());
    	assertEquals(targetTime, stop3.getArrivalTime());
    	assertEquals(stop1.getDepartureTime(), it.getDepartureTime());
    	assertEquals(stop4.getArrivalTime(), it.getArrivalTime());
	}

}
