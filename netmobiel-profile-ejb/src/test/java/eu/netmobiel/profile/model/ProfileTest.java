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
	public void createProfileDefault() {
		Profile p = new Profile();
		assertNotNull(p.getConsent());
		assertNotNull(p.getNotificationOptions());
		assertNotNull(p.getSearchPreferences());
		assertNull(p.getRidesharePreferences());
	}

	@Test
	public void createProfileNetMobielUser() {
		NetMobielUserImpl user = new NetMobielUserImpl(null, "Jaap", "Reitsma", "j.reitsma@saxion.nl");
		Profile p = new Profile(user);
		assertNotNull(p.getConsent());
		assertNotNull(p.getNotificationOptions());
		assertNotNull(p.getSearchPreferences());
		assertNull(p.getRidesharePreferences());
	}
}
