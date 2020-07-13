package eu.netmobiel.payment.client;

import static org.junit.Assert.*;

import java.io.File;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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
import eu.netmobiel.payment.client.model.PaymentLinkOptions;

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
            .addClass(PaymentClient.class)
            // Arquillian tests need the beans.xml to recognize it as a CDI application
            .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
            // Optional add dependencies or exclude modules provided by WildFly
        	.addAsWebInfResource("jboss-deployment-structure.xml")
        	// Het is me nog niet duidelijke of deze soms wel nodig is.
//        	.addAsResource("log4j.properties")
        	;
//		System.out.println(archive.toString(Formatters.VERBOSE));
        return archive;
    }

    @Inject
    private PaymentClient paymentClient;

    @Inject
    private Logger log;

    @Test
    public void testPaymentLink() {
        PaymentLinkOptions options = new PaymentLinkOptions();
        options.euroCents = 99;
        options.description = "Test payment provider";
        options.expirationMinutes = 15;
        options.merchantOrder = "Unieke NetMobiel transactie id";
        options.returnAfterCompletion = "https://emspay-test.glitch.com/ideal/return";
        PaymentLink link = paymentClient.getPaymentLink(options);
        assertNotNull(link);
        log.debug(link.toString());
        assertNotNull(link.paymentPage);
        assertNotNull(link.transactionId);
    }

    @Test
    public void testPaymentStatus() {
    	String theStatus = "Netmobiel-1234-test";
    	try {
    		/*PaymentStatus status = */paymentClient.getPaymentStatus(theStatus);
    		fail("Expected a NotFoundException");
    	} catch (NotFoundException ex) {
    		log.debug("Expected exception: " + ex.toString());
    	}
    }
}
