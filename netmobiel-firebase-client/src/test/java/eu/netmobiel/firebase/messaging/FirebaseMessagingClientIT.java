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
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.commons.util.UrnHelper;

@RunWith(Arquillian.class)
public class FirebaseMessagingClientIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	Archive<?> archive = null;
    	try {
//	    	File[] deps = Maven.configureResolver()
//					.loadPomFromFile("pom.xml")
//					.resolve("eu.netmobiel:netmobiel-commons:jar:?")
//					.withTransitivity()
//					.asFile();
	    	File[] deps = Maven.configureResolver()
					.loadPomFromFile("pom.xml")
					.importCompileAndRuntimeDependencies() 
					.resolve()
					.withTransitivity()
					.asFile();
			archive = ShrinkWrap.create(WebArchive.class, "test.war")
			    	// We want to test with the shaded jar to check if the packaging is ok!
//	       		.addAsLibrary(new File("target/netmobiel-firebase-client-shaded.jar"))
	       		.addAsLibraries(deps)
	            .addClass(FirebaseMessagingClient.class)
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

    /**
     * FCM token of net@netmobiel.net.
     */
    private static final String testFcmToken = "eFxxs0F4uEadoiqHu54Byt:APA91bHJHwXFxH3jOSUybFs7iRw48kpIHPsGM31BpzHJZPGsaa37c6SXhjjC-FiJyNcGKowwKJiySKl6AjGT0QDA0K-yjlnrqfHudiEt6wvUHYCeDC6JqR7Tcc-Ns5qPK_J5n8D3dwci";
    
    private static final boolean enableRealSendMessage = false;
    @Before
    public void prepareTest() throws Exception {
    	aSender = new NetMobielUserImpl("some-user", "Otto", "Normalverbraucher", "otto1971@hotmail.com");
    }
    
    @Test
    public void testSendMessageDryRunBadToken() throws Exception {
    	NetMobielMessage msg = new TestMessage(999L, "a body", Instant.now(), aSender);
    	try {
    		client.send("someFcmToken", msg, true);
    		fail("Expected an exception due to invalid FCM token");
    	} catch (SystemException ex) {
    		assertEquals("The registration token is not a valid FCM registration token", ex.getCause().getMessage());
    	}
    }

    @Test
    public void testSendMessageRealToken() throws Exception {
    	NetMobielMessage msg = new TestMessage(1000L, "Dit is een Netmobiel test van Jaap, stuur even appje als je dit ontvangt", Instant.now(), aSender);
    	try {
    		client.send(testFcmToken, msg, !enableRealSendMessage);
    	} catch (SystemException ex) {
    		fail(ex.toString());
    	}
    }
    
    @Test
    public void testPublishMessage() {
    	try {
	    	NetMobielMessage msg = new TestMessage(999L, "a body", Instant.now(), aSender);
	    	client.publish("systemTopic", msg, true);
    	} catch (Exception ex) {
    		fail("Got exception");
    	}
    }

    private static class TestMessage implements NetMobielMessage {
    	private Long id;
    	private String body;
    	private Instant creationTime;
        private NetMobielUser sender;

        public TestMessage(Long id, String body, Instant creationTime, NetMobielUser sender) {
        	this.id = id;
        	this.body = body;
        	this.creationTime = creationTime;
        	this.sender = sender;
        }

		@Override
        public String getUrn() {
        	return UrnHelper.createUrn("urn:nb:cm:message:", this.id);
        }
        
		@Override
		public String getBody() {
			return body;
		}

		@Override
		public Instant getCreatedTime() {
			return creationTime;
		}

		@Override
		public NetMobielUser getSender() {
			return sender;
		}
        
    }
}
