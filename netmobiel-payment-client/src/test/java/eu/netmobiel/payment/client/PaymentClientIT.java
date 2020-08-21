package eu.netmobiel.payment.client;

import static org.junit.Assert.*;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.payment.client.model.PaymentLink;
import eu.netmobiel.payment.client.model.PaymentLinkStatus;
import eu.netmobiel.payment.client.model.PaymentOrder;

@RunWith(Arquillian.class)
public class PaymentClientIT {
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
            .addPackage(PaymentLink.class.getPackage())
            .addClass(Jackson2ObjectMapperContextResolver.class)
            .addClass(PaymentClient.class)
            // Arquillian tests need the beans.xml to recognize it as a CDI application
            .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
            // Optional add dependencies or exclude modules provided by WildFly
        	.addAsWebInfResource("jboss-deployment-structure.xml")
        	// Add if controlling log4j locally 
//        	.addAsResource("log4j.properties")
        	;
//		System.out.println(archive.toString(Formatters.VERBOSE));
        return archive;
    }

    @Inject
    private PaymentClient paymentClient;

    @Inject
    private Logger log;

    private static String lastPaymentLinkId;
    private static final String wellknownOrderId = "4bf9f6fa-eb1d-4737-aebb-836328f91c7f";
    private static final String wellknownPaymentLinkId = "a76dcc53-0362-409b-b35a-c664b88d174b";
    
    @Test
    @InSequence(1)
    public void testCreatePaymentLink() {
        PaymentLink options = new PaymentLink();
        options.amount = 99;
        options.description = "NetMobiel payment provider test";
        options.expirationPeriod = Duration.ofMinutes(15);
        options.merchantOrderId = "NetMobiel-" + DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        options.returnUrl = "https://otp.netmobiel.eu/";
        try {
	        PaymentLink link = paymentClient.createPaymentLink(options);
	        assertNotNull(link);
	        log.debug(link.toString());
	        assertNotNull(link.id);
	        assertNotNull(link.paymentUrl);
	        lastPaymentLinkId = link.id;
        } catch (Exception ex) {
    		log.error("Failed to create payment link - " + ex.toString(), ex);
        	fail("Unexpected exception: " + ex.toString());
        }
    }

    @Test
    @InSequence(2)
    public void testPaymentStatus_NotFound() {
    	String id = "Netmobiel-1234-test";
    	try {
    		/*PaymentStatus status = */paymentClient.getPaymentLink(id);
    		fail("Expected a NotFoundException");
    	} catch (NotFoundException ex) {
    		log.debug("Expected exception: " + ex.toString());
    	}
    }
    
    @Test
    @InSequence(3)
    public void testGetPaymentLink() {
        log.debug("Fetch last payment link: " + lastPaymentLinkId);
        if (lastPaymentLinkId != null) {
        	try {
				PaymentLink link = paymentClient.getPaymentLink(lastPaymentLinkId);
			    assertNotNull(link);
			    log.debug(link.toString());
			    assertEquals(PaymentLinkStatus.NEW, link.status);
        	} catch (Exception ex) {
        		log.error("Failed to retrieve payment link - " + ex.toString(), ex);
        		fail("Failed to retrieve payment link");
        	}
        }
    }

    @Test
    @InSequence(4)
    public void testGetPaymentOrder() {
    	try {
			PaymentOrder order = paymentClient.getPaymentOrder(wellknownOrderId);
		    assertNotNull(order);
		    log.debug(order.toString());
		    assertEquals(order.id, wellknownOrderId);
		    assertEquals(order.relatedPaymentLinkId, wellknownPaymentLinkId);
    	} catch (Exception ex) {
    		log.error("Failed to retrieve payment order - " + ex.toString(), ex);
    		fail("Failed to retrieve payment order");
    	}
    }

    @Test
    @InSequence(5)
    public void testGetPaymentLinkByOrder() {
    	try {
			PaymentLink link = paymentClient.getPaymentLinkByOrderId(wellknownOrderId);
		    assertNotNull(link);
		    log.debug(link.toString());
		    assertEquals(link.id, wellknownPaymentLinkId);
    	} catch (Exception ex) {
    		log.error("Failed to retrieve payment link - " + ex.toString(), ex);
    		fail("Failed to retrieve payment link");
    	}
    }
}
