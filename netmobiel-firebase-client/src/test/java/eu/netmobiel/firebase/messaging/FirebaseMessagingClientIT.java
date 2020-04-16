package eu.netmobiel.firebase.messaging;


import static org.junit.Assert.*;

import java.io.File;
import java.time.Instant;

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
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.NetMobielMessage;
import eu.netmobiel.commons.model.NetMobielUser;

@RunWith(Arquillian.class)
public class FirebaseMessagingClientIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	Archive<?> archive = null;
    	try {
	    	File[] deps = Maven.configureResolver()
					.loadPomFromFile("pom.xml")
					.resolve("eu.netmobiel:netmobiel-commons:jar:?")
					.withTransitivity()
					.asFile();
	    	// We want to test with the shaded jar to check if the packaging is ok!
			archive = ShrinkWrap.create(WebArchive.class, "test.war")
	       		.addAsLibrary(new File("target/netmobiel-firebase-client-shaded.jar"))
	       		.addAsLibraries(deps)
	            // Arquillian tests need the beans.xml to recognize it as a CDI application
	            .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
	            // Take car of removing the default json provider, because we use jackson everywhere (unfortunately).
	        	.addAsWebInfResource("jboss-deployment-structure.xml")
	        	.addAsResource("log4j.properties");
	//		System.out.println(archive.toString(Formatters.VERBOSE));
    	} catch (Throwable t) {
    		t.printStackTrace();
    	}
        return archive;
    }

    @Inject
    private FirebaseMessagingClient client;

    @SuppressWarnings("unused")
	@Inject
    private Logger log;

    private NetMobielUser aSender;
    
    @Before
    public void prepareTest() throws Exception {
    	aSender = new TestUser("some-user", "Otto", "Normalverbraucher");
    }
    
    @Test
    public void testSendMessage() throws Exception {
    	NetMobielMessage msg = new TestMessage("a body", "urn:nb:ts:Test:1234", "Test 1234", Instant.now(), aSender);
    	try {
    		client.send("someFcmToken", msg, true);
    		fail("Expected an exception due to invalid FCM token");
    	} catch (SystemException ex) {
    		assertEquals("The registration token is not a valid FCM registration token", ex.getCause().getMessage());
    	}
    }

    @Test
    public void testPublishMessage() throws Exception {
    	NetMobielMessage msg = new TestMessage("a body", "urn:nb:ts:Test:1234", "Test 1234", Instant.now(), aSender);
    	client.publish("systemTopic", msg, true);
    }

    private static class TestUser implements NetMobielUser {
        private String managedIdentity;
    	private String givenName;
    	private String familyName;
    	
    	public TestUser(String id, String givenName, String familyName) {
    		this.managedIdentity = id;
    		this.givenName = givenName;
    		this.familyName = familyName;
    	}

		public String getManagedIdentity() {
			return managedIdentity;
		}

		public String getGivenName() {
			return givenName;
		}

		public String getFamilyName() {
			return familyName;
		}
    	
    }

    private static class TestMessage implements NetMobielMessage {
    	private String body;
    	private String context;
    	private String subject;
    	private Instant creationTime;
        private NetMobielUser sender;

        public TestMessage(String body, String context, String subject, Instant creationTime, NetMobielUser sender) {
        	this.body = body;
        	this.context = context;
        	this.subject = subject;
        	this.creationTime = creationTime;
        	this.sender = sender;
        }

		public String getBody() {
			return body;
		}

		public String getContext() {
			return context;
		}

		public String getSubject() {
			return subject;
		}

		public Instant getCreationTime() {
			return creationTime;
		}

		public NetMobielUser getSender() {
			return sender;
		}
        
    }
}
