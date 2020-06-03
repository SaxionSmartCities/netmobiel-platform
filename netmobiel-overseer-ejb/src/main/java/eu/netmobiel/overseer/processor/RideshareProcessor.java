package eu.netmobiel.overseer.processor;

import javax.annotation.security.RunAs;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.rideshare.model.Booking;

/**
 * Stateless bean for the management of the ledger.
 * 
 * TODO: Security
 * 
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
@RunAs("system") 
public class RideshareProcessor {
	
    @Inject
    private PublisherService publisherService;

    @Inject
    private Logger logger;
    
//    @Asynchronous
    public void onBookingCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Created Booking newBooking) {
    	logger.info("New booking created: " + newBooking.toString());
    }

    public void onBookingRemoved(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Removed Booking booking) {
    	logger.info(String.format("Booking cancelled by %s because '%s' %s", 
    			Boolean.TRUE == booking.getCancelledByDriver() ? "Driver" : "Passenger",
    			booking.getCancelReason(),
    			booking.toString()));
    }
}
