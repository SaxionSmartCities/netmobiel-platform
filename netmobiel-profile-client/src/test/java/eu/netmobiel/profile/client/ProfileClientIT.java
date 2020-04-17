package eu.netmobiel.profile.client;


import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
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
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.AccessTokenResponse;
import org.slf4j.Logger;

import eu.netmobiel.profile.api.model.Profile;

@RunWith(Arquillian.class)
public class ProfileClientIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	File[] deps = Maven.configureResolver()
				.loadPomFromFile("pom.xml")
				.importRuntimeAndTestDependencies() 
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
        	.addAsWebInfResource("jboss-deployment-structure.xml")
        	.addAsResource("log4j.properties")
        	.addAsResource("keycloak-issuer.json")
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
    private String accessToken;
    
    @Before
    public void prepare() throws Exception {
        testSetupProperties = new Properties();
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-setup.properties")){
			testSetupProperties.load(inputStream);
		}
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("keycloak-issuer.json");
    	AuthzClient authzClient = AuthzClient.create(configStream);
    	AccessTokenResponse rsp = authzClient.obtainAccessToken(testSetupProperties.getProperty("username"), testSetupProperties.getProperty("password"));
    	accessToken = rsp.getToken();
    }

    @Test
    public void testGetFcmToken() throws Exception {
		String fcmtoken = client.getFirebaseToken(accessToken, testSetupProperties.getProperty("managedIdentity"));
    	assertNotNull(fcmtoken);
    	assertEquals(testSetupProperties.getProperty("fcmToken"), fcmtoken);
    }

    @Test
    public void testGetFcmTokenAccessDenied() throws Exception {
    	try {
			client.getFirebaseToken(accessToken + "xxxx", testSetupProperties.getProperty("managedIdentity"));
			fail("Expected Exception");
    	} catch (Exception ex) {
    		assertTrue(ex instanceof SecurityException);
    		assertTrue(ex.getMessage().contains("403"));
    	}
    }
    
    @Test
    public void testGetProfile() throws Exception {
		Profile profile = client.getProfile(accessToken, testSetupProperties.getProperty("managedIdentity"));
    	assertNotNull(profile);
    	assertEquals(testSetupProperties.getProperty("managedIdentity"), profile.getId());
    }

}
