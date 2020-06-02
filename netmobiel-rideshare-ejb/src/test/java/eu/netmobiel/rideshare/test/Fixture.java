package eu.netmobiel.rideshare.test;

import java.time.Instant;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.CarType;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

public class Fixture {
	public static final GeoLocation placeZieuwent = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
	public static final GeoLocation placeSlingeland = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
	public static final GeoLocation placeRaboZutphen = GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966");
	public static final GeoLocation placeZieuwentRKKerk = GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542");
	public static final GeoLocation placeThuisLichtenvoorde  = GeoLocation.fromString("Rapenburgsestraat Lichtenvoorde::51.987757,6.564012");
	public static final GeoLocation placeCentrumDoetinchem = GeoLocation.fromString("Catharina Parkeergarage Doetinchem::51.9670528,6.2894002");

	private Fixture() {
		// No instances allowed
	}

	public static User createUser(String identity, String givenName, String familyName) {
		return new User(identity, givenName, familyName);
	}
	
	public static User createUser(LoginContext loginContext) {
        Subject subject = loginContext.getSubject();
        @SuppressWarnings("rawtypes")
		Set<KeycloakPrincipal> ps = subject.getPrincipals(KeycloakPrincipal.class);
        @SuppressWarnings("unchecked")
		KeycloakPrincipal<KeycloakSecurityContext> p = ps.iterator().next();
        return createUser(p.getKeycloakSecurityContext().getToken());
	}

	public static User createUser(AccessToken token) {
		return new User(token.getSubject(), token.getGivenName(), token.getFamilyName());
	}

	public static User createUser1() {
		return createUser("ID1", "Carla1", "Netmobiel");
	}
	
	public static User createUser2() {
		return createUser("ID2", "Carla2", "Netmobiel");
	}
	public static User createUser3() {
		return createUser("ID3", "Carla3", "Netmobiel");
	}

	public static Car createCarVolvo(User driver) {
		Car c = new Car();
		c.setBrand("Volvo");
		c.setCo2Emission(200);
		c.setColor("ROOD");
		c.setDriver(driver);
		c.setLicensePlate("12-AB-345");
		c.setModel("V70");
		c.setNrDoors(5);
		c.setNrSeats(5);
		c.setRegistrationCountry("NL");
		c.setRegistrationYear(2009);
		c.setType(CarType.ESTATE);
		return c;
	}
	public static Car createCarBMW(User driver) {
		Car c = new Car();
		c.setBrand("BMW");
		c.setCo2Emission(0);
		c.setColor("ZWART");
		c.setDriver(driver);
		c.setLicensePlate("34-CD-567");
		c.setModel("Z4");
		c.setNrDoors(2);
		c.setNrSeats(2);
		c.setRegistrationCountry("NL");
		c.setRegistrationYear(2012);
		c.setType(CarType.CONVERTIBLE);
		return c;
	}
	
	public static RideTemplate createTemplate(Car car, Instant departureTime, Instant horizon) {
		RideTemplate t = new RideTemplate();
		t.setCar(car);
		t.setDriver(car.getDriver());
		t.setDepartureTime(departureTime);
		t.setArrivalTime(departureTime.plusSeconds(60 * 60));
		t.setFrom(placeSlingeland);
		t.setTo(placeZieuwent);
		t.setRecurrence(new Recurrence(1, horizon));
		return t;
	}

	public static Car createCarFordThunderbird(User driver) {
		Car c = new Car();
		c.setBrand("Ford");
		c.setColor("WIT");
		c.setDriver(driver);
		c.setLicensePlate("00-11-AF");
		c.setModel("Thunderbird");
		c.setNrDoors(2);
		c.setNrSeats(1);
		c.setRegistrationCountry("NL");
		c.setRegistrationYear(1964);
		c.setType(CarType.CONVERTIBLE);
		return c;
	}
	public static Ride createRide(RideTemplate template, Instant departureTime) {
		Ride r = template.createRide();
		r.setDepartureTime(departureTime);
		r.setArrivalTime(departureTime.plusSeconds(60 * 60));
		return r;
	}

	public static Ride createRide(Car car, Instant departureTime, Instant arrivalTime) {
		Ride r = new Ride();
		r.setCarRef(RideshareUrnHelper.createUrn(Car.URN_PREFIX, car.getId()));
		r.setCar(car);
		r.setDriver(car.getDriver());
		if (departureTime != null) {
			r.setDepartureTime(departureTime);
			r.setArrivalTime(departureTime.plusSeconds(60 * 60));
			r.setArrivalTimePinned(false);
		} else {
			r.setArrivalTime(arrivalTime);
			r.setDepartureTime(arrivalTime.minusSeconds(60 * 60));
			r.setArrivalTimePinned(true);
		}
		r.setFrom(placeZieuwent);
		r.setTo(placeSlingeland);
		r.setMaxDetourMeters(5000);
		r.setNrSeatsAvailable(3);
		return r;
	}
	
	/**
	 * Creates a complete ride object, intended to insert directly in database.
	 * @param car
	 * @param departureTime
	 * @param arrivalTime
	 * @return
	 */
	public static Ride createRideObject(Car car, Instant departureTime, Instant arrivalTime) {
		Ride r = createRide(car, departureTime, arrivalTime);
		r.setCar(car);
		r.setDriver(car.getDriver());
		if (departureTime != null) {
			r.setArrivalTime(departureTime.plusSeconds(60 * 60));
		} else {
			r.setDepartureTime(arrivalTime.minusSeconds(60 * 60));
		}
		return r;
	}
	
	public static Ride createCompleteRide(Car car, Instant departureTime, Instant arrivalTime) {
		Ride r = Fixture.createRide(car, departureTime, arrivalTime);
		r.setDriver(car.getDriver());
		r.setCar(car);
		if (departureTime == null) {
			r.setDepartureTime(arrivalTime.minusSeconds(60 * 60));
		} else {
			r.setArrivalTime(departureTime.plusSeconds(60 * 60));
		}
		return r;
	}
	public static Booking createBooking(Ride r, User p, Instant departureTime, Instant arrivalTime) {
		Booking b = new Booking(r, p, Fixture.placeZieuwentRKKerk, Fixture.placeSlingeland, 1);
		b.setDepartureTime(departureTime);
		b.setArrivalTime(arrivalTime);
		return b;
	}
	public static Booking createBooking(Ride r, User p, GeoLocation pickup, Instant departureTime, GeoLocation dropoff, Instant arrivalTime) {
		Booking b = new Booking(r, p, pickup, dropoff, 1);
		b.setDepartureTime(departureTime);
		b.setArrivalTime(arrivalTime);
		return b;
	}
}
