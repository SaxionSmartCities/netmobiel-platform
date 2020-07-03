package eu.netmobiel.planner.model;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

}
