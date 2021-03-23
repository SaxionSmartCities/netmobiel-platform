package eu.netmobiel.profile.model;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.commons.model.NetMobielUserImpl;

public class ProfileTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void createProfile_Default() {
		Profile p = new Profile();
		assertNotNull(p.getConsent());
		assertNotNull(p.getNotificationOptions());
		assertNull(p.getSearchPreferences());
		assertNull(p.getRidesharePreferences());
	}

	@Test
	public void createProfile_NetMobielPassenger() {
		NetMobielUserImpl user = new NetMobielUserImpl(null, "Jaap", "Reitsma", "j.reitsma@saxion.nl");
		Profile p = new Profile(user, UserRole.PASSENGER);
		assertNotNull(p.getConsent());
		assertNotNull(p.getNotificationOptions());
		assertNotNull(p.getSearchPreferences());
		assertNull(p.getRidesharePreferences());
	}

	@Test
	public void createProfile_NetMobielDriver() {
		NetMobielUserImpl user = new NetMobielUserImpl(null, "Jaap", "Reitsma", "j.reitsma@saxion.nl");
		Profile p = new Profile(user, UserRole.DRIVER);
		assertNotNull(p.getConsent());
		assertNotNull(p.getNotificationOptions());
		assertNull(p.getSearchPreferences());
		assertNotNull(p.getRidesharePreferences());
	}

	@Test
	public void createProfile_NetMobielBoth() {
		NetMobielUserImpl user = new NetMobielUserImpl(null, "Jaap", "Reitsma", "j.reitsma@saxion.nl");
		Profile p = new Profile(user, UserRole.BOTH);
		assertNotNull(p.getConsent());
		assertNotNull(p.getNotificationOptions());
		assertNotNull(p.getSearchPreferences());
		assertNotNull(p.getRidesharePreferences());
	}
}
