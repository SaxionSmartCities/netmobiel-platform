package eu.netmobiel.profile.client;


import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.Properties;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.representations.AccessTokenResponse;
import org.slf4j.Logger;

import eu.netmobiel.profile.api.model.Profile;

@RunWith(Arquillian.class)
public class ProfileClientIT {
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
            .addClass(ProfileClient.class)
            .addClass(Jackson2ObjectMapperContextResolver.class)
            // Arquillian tests need the beans.xml to recognize it as a CDI application
            .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
            // Take car of removing the default json provider, because we use jackson everywhere (unfortunately).
            // Also add keycloak client
        	.addAsWebInfResource("jboss-deployment-structure.xml")
        	.addAsResource("log4j.properties")
        	.addAsResource("test-setup.properties");
//		System.out.println(archive.toString(Formatters.VERBOSE));
        return archive;
    }
   
    @Inject
    private ProfileClient client;

    @SuppressWarnings("unused")
	@Inject
    private Logger log;

    private Properties testSetupProperties;
    
    @Before
    public void prepare() throws Exception {
        testSetupProperties = new Properties();
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-setup.properties")){
			testSetupProperties.load(inputStream);
		}
    }

    @Test
    public void testGetFcmToken() throws Exception {
		String fcmtoken = client.getFirebaseToken(testSetupProperties.getProperty("managedIdentity"));
    	assertNotNull(fcmtoken);
    	assertEquals(testSetupProperties.getProperty("fcmToken"), fcmtoken);
    }

    @Test
    public void testGetFcmToken_MyToken() throws Exception {
    	AccessTokenResponse accessTokenResponse = client.getAccessToken(testSetupProperties.getProperty("username"), testSetupProperties.getProperty("password"));
		String fcmtoken = client.getFirebaseToken(accessTokenResponse.getToken(), testSetupProperties.getProperty("managedIdentity"));
    	assertNotNull(fcmtoken);
    	assertEquals(testSetupProperties.getProperty("fcmToken"), fcmtoken);
    }

    @Test
    public void testGetFcmToken_NoToken() throws Exception {
    	try {
			client.getFirebaseToken(null, testSetupProperties.getProperty("managedIdentity"));
			fail("Expected Exception");
    	} catch (Exception ex) {
    		assertTrue(ex instanceof SecurityException);
    		assertTrue(ex.getMessage().contains("403"));
    	}
    }

    @Test
    public void testGetFcmToken_InvalidAccessToken() throws Exception {
    	try {
			client.getFirebaseToken("very-bad-token", testSetupProperties.getProperty("managedIdentity"));
			fail("Expected Exception");
    	} catch (Exception ex) {
    		assertTrue(ex instanceof SecurityException);
    		assertTrue(ex.getMessage().contains("403"));
    	}
    }
    
    @Test
    public void testGetProfile() throws Exception {
    	client.clearToken();
		Profile profile = client.getProfile(testSetupProperties.getProperty("managedIdentity"));
    	assertNotNull(profile);
    	assertEquals(testSetupProperties.getProperty("managedIdentity"), profile.getId());
    }

    @Test
    public void testGetAccessToken() throws Exception {
    	client.clearToken();
    	String accessToken = client.getServiceAccountAccessToken();
    	assertNotNull(accessToken);
    	Instant tokenExpiration = client.getProfileTokenExpiration();
    	String accessToken2 = client.getServiceAccountAccessToken();
    	assertTrue(tokenExpiration == client.getProfileTokenExpiration());
    	assertEquals(accessToken, accessToken2);
    	client.clearToken();
    	accessToken2 = client.getServiceAccountAccessToken();
    	// This is a different object now
    	assertTrue(tokenExpiration != client.getProfileTokenExpiration());
    	assertNotEquals(accessToken, accessToken2);
    }
}
