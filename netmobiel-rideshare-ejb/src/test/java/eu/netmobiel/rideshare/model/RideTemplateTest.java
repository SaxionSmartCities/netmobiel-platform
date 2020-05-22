package eu.netmobiel.rideshare.model;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.commons.model.GeoLocation;

public class RideTemplateTest {

	private RideTemplate template;
	private Recurrence recurrence;

	private Car car;
	private User driver;
	private static GeoLocation placeZieuwent = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
	private static GeoLocation placeSlingeland = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");

	@Before
	public void createTemplate() {
		car = new Car();
		car.setId(1L);
		car.setLicensePlate("12-AB-45");
		driver = new User("1234567890", "Otto", "Normalverbraucher");
		recurrence = new Recurrence(1);
		template = new RideTemplate();
		template.setRecurrence(recurrence);
		template.setCar(car);
		template.setDriver(driver);
		template.setArrivalTime(Instant.parse("2020-05-19T12:00:00Z"));
		template.setArrivalTimePinned(false);
		template.setCarthesianBearing(1);
		template.setCarthesianDistance(2);
		template.setCO2Emission(3);
		template.setDepartureTime(Instant.parse("2020-05-19T11:00:00Z"));
		template.setDistance(4);
		template.setFrom(placeZieuwent);
		template.setMaxDetourMeters(5);
		template.setMaxDetourSeconds(6);
		template.setNrSeatsAvailable(7);
		template.setRemarks("Mijn toelichting");
		template.setTo(placeSlingeland);
	}
	
	@Test
	public void testGenerateRides() {
		recurrence.setInterval(3);
		int intervalseconds = recurrence.getInterval() * 24 * 60 * 60;

		Instant departure1 = template.getDepartureTime();
		Instant arrival1 = template.getArrivalTime();
		Instant departure2 = departure1.plusSeconds(intervalseconds);
		Instant arrival2 = arrival1.plusSeconds(intervalseconds);
		Instant departure3 = departure2.plusSeconds(intervalseconds);
		Instant arrival3 = arrival2.plusSeconds(intervalseconds);
		int horizonDays = recurrence.getInterval() + 1;
		Instant horizon = template.getDepartureTime().plusSeconds(horizonDays * 24 * 60 * 60);
		List<Ride> rides = template.generateRides(horizon);
		assertNotNull(rides);
		assertEquals(2, rides.size());
		for (Ride r: rides) {
			assertEquals(template.isArrivalTimePinned(), r.isArrivalTimePinned());
			assertEquals(template.getCarthesianBearing(), r.getCarthesianBearing());
			assertEquals(template.getCarthesianDistance(), r.getCarthesianDistance());
			assertEquals(template.getCO2Emission(), r.getCO2Emission());
			assertEquals(template.getDistance(), r.getDistance());
			assertEquals(template.getDuration(), r.getDuration());
			assertEquals(template.getFrom(), r.getFrom());
			assertEquals(template.getMaxDetourMeters(), r.getMaxDetourMeters());
			assertEquals(template.getMaxDetourSeconds(), r.getMaxDetourSeconds());
			assertEquals(template.getNrSeatsAvailable(), r.getNrSeatsAvailable());
			assertEquals(template.getRemarks(), r.getRemarks());
			assertEquals(template.getTo(), r.getTo());
			assertSame(template.getCar(), r.getCar());
			assertSame(template.getDriver(), r.getDriver());
			assertSame(template, r.getRideTemplate());
		}
		Ride r1 = rides.get(0);
		assertEquals(departure1, r1.getDepartureTime());
		assertEquals(arrival1, r1.getArrivalTime());
		Ride r2 = rides.get(1);
		assertEquals(departure2, r2.getDepartureTime());
		assertEquals(arrival2, r2.getArrivalTime());
		
		assertEquals(departure3, template.getDepartureTime());
		assertEquals(arrival3, template.getArrivalTime());
		
	}

}
