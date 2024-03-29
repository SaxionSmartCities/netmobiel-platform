package eu.netmobiel.rideshare.test;

import java.time.Instant;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.CarType;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideState;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.RideshareUser;

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

	public static RideshareUser createUser(String identity, String givenName, String familyName) {
		return new RideshareUser(identity, givenName, familyName);
	}
	
	public static RideshareUser createUser(LoginContext loginContext) {
        Subject subject = loginContext.getSubject();
        @SuppressWarnings("rawtypes")
		Set<KeycloakPrincipal> ps = subject.getPrincipals(KeycloakPrincipal.class);
        @SuppressWarnings("unchecked")
		KeycloakPrincipal<KeycloakSecurityContext> p = ps.iterator().next();
        return createUser(p.getKeycloakSecurityContext().getToken());
	}

	public static RideshareUser createUser(AccessToken token) {
		return new RideshareUser(token.getSubject(), token.getGivenName(), token.getFamilyName());
	}

	public static RideshareUser createDriver1() {
		return createUser("ID1", "Carla1", "Netmobiel");
	}
	
	public static RideshareUser createDriver2() {
		return createUser("ID2", "Carla2", "Netmobiel");
	}
	public static RideshareUser createDriver3() {
		return createUser("ID3", "Carla3", "Netmobiel");
	}

	public static RideshareUser createPassenger1() {
		return createUser("IP1", "Simon1", "Netmobiel");
	}
	
	public static RideshareUser createPassenger2() {
		return createUser("IP2", "Simon2", "Netmobiel");
	}

	public static Car createCarVolvo(RideshareUser driver) {
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
    	c.setLicensePlateRaw(Car.unformatPlate(c.getLicensePlate()));
		return c;
	}
	public static Car createCarBMW(RideshareUser driver) {
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
    	c.setLicensePlateRaw(Car.unformatPlate(c.getLicensePlate()));
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

	public static Car createCarFordThunderbird(RideshareUser driver) {
		Car c = new Car();
		c.setBrand("Ford");
		c.setColor("WIT");
		c.setDriver(driver);
		c.setLicensePlate("00-11-AF");
		c.setModel("Thunderbird");
		c.setNrDoors(2);
		c.setNrSeats(2);
		c.setRegistrationCountry("NL");
		c.setRegistrationYear(1964);
		c.setType(CarType.CONVERTIBLE);
    	c.setLicensePlateRaw(Car.unformatPlate(c.getLicensePlate()));
		return c;
	}
	public static Ride createRide(RideTemplate template, Instant departureTime) {
		Ride r = template.createRide();
		r.setDepartureTime(departureTime);
		r.setArrivalTime(departureTime.plusSeconds(60 * 60));
		r.setState(RideState.SCHEDULED);
		return r;
	}

	public static Ride createRide(Car car, Instant departureTime, Instant arrivalTime) {
		return createRide(car, placeZieuwent, departureTime, placeSlingeland, arrivalTime);
	}

	public static Ride createRide(Car car, GeoLocation from, Instant departureTime, GeoLocation to, Instant arrivalTime) {
		Ride r = new Ride();
		r.setCarRef(UrnHelper.createUrn(Car.URN_PREFIX, car.getId()));
		r.setCar(car);
		r.setDriver(car.getDriver());
		r.setDepartureTime(departureTime);
		r.setArrivalTime(arrivalTime);
		r.setArrivalTimePinned(false);
		if (departureTime == null && arrivalTime == null) {
			throw new IllegalArgumentException();
		}
		if (departureTime == null) {
			r.setDepartureTime(arrivalTime.minusSeconds(60 * 60));
			r.setArrivalTimePinned(true);
		} else if (arrivalTime == null) {
			r.setArrivalTime(departureTime.plusSeconds(60 * 60));
		}
		r.setState(RideState.SCHEDULED);
		r.setFrom(from);
		r.setTo(to);
		r.setMaxDetourMeters(10000);
		r.setNrSeatsAvailable(Math.min(3, car.getNrSeats() - 1 ));
		r.updateShareEligibility();
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
		if (departureTime == null) {
			r.setDepartureTime(arrivalTime.minusSeconds(60 * 60));
		} else {
			r.setArrivalTime(departureTime.plusSeconds(60 * 60));
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
	
	public static Booking createBooking(Ride r, RideshareUser p, Instant departureTime, Instant arrivalTime, String passengerTripRef) {
		return createBooking(r, p, Fixture.placeZieuwentRKKerk, departureTime, Fixture.placeSlingeland, arrivalTime, passengerTripRef);
	}
	
	public static Booking createBooking(Ride r, RideshareUser p, GeoLocation pickup, Instant departureTime, GeoLocation dropoff, Instant arrivalTime, String passengerTripRef) {
		Booking b = new Booking(r, p, pickup, dropoff, 1);
		b.setDepartureTime(departureTime);
		b.setArrivalTime(arrivalTime);
		b.setPassengerTripRef(passengerTripRef);
		return b;
	}
}
