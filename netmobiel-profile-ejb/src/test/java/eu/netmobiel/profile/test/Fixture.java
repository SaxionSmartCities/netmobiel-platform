package eu.netmobiel.profile.test;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.profile.model.Address;
import eu.netmobiel.profile.model.Place;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.UserRole;

public class Fixture {
	public static final GeoLocation placeZieuwent = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
	public static final GeoLocation placeSlingeland = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
	public static final GeoLocation placeRaboZutphen = GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966");
	public static final GeoLocation placeZieuwentRKKerk = GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542");
	public static final GeoLocation placeThuisLichtenvoorde  = GeoLocation.fromString("Rapenburgsestraat Lichtenvoorde::51.987757,6.564012");
	public static final GeoLocation placeCentrumDoetinchem = GeoLocation.fromString("Catharina Parkeergarage Doetinchem::51.9670528,6.2894002");
	public static final GeoLocation placeThuisHengelo = GeoLocation.fromString("Thuis Hengelo::52.27314, 6.78576");

	private Fixture() {
		// No instances allowed
	}

	public static Profile createProfile(String identity, String givenName, String familyName, String email, UserRole role) {
		return new Profile(identity, givenName, familyName, email, role);
	}
	
	public static Profile createDriver1() {
		return createProfile("ID1", "Carla1", "Netmobiel", "carla1@netmobiel.eu", UserRole.DRIVER);
	}
	
	public static Profile createDriver2() {
		return createProfile("ID2", "Carla2", "Netmobiel", "carla2@netmobiel.eu", UserRole.DRIVER);
	}
	public static Profile createDriver3() {
		return createProfile("ID3", "Carla3", "Netmobiel", "carla3@netmobiel.eu", UserRole.DRIVER);
	}

	public static Profile createPassenger1() {
		return createProfile("IP1", "Simon1", "Netmobiel", "simon1@netmobiel.eu", UserRole.PASSENGER);
	}
	
	public static Profile createPassenger2() {
		return createProfile("IP2", "Simon2", "Netmobiel", "simon2@netmobiel.eu", UserRole.PASSENGER);
	}

	public static Address createAddressLichtenvoorde() {
		Address ad = new Address();
		ad.setLocation(placeThuisLichtenvoorde);
		ad.setCountryCode("NL");
		ad.setLocality("Lichtenvoorde");
		ad.setStreet("Rapenburgsestraat");
		ad.setHouseNumber("33");
		ad.setPostalCode("7131CW");
		return ad;
	}

	public static Address createAddressHengelo() {
		Address ad = new Address();
		ad.setLocation(placeThuisHengelo);
		ad.setCountryCode("NL");
		ad.setLocality("Hengelo");
		ad.setStreet("Meester P.J. Troelstrastraat");
		ad.setHouseNumber("1");
		ad.setPostalCode("7556EG");
		return ad;
	}

	public static Place createPlace(Address addr) {
		Place p = new Place();
		p.setAddress(addr);
		return p;
	}
}
