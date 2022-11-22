package eu.netmobiel.overseer.processor;

import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;

import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.service.RideManager;

/**
 * Stateless bean for issueing quotes on travel costs etc.
 *  
 * @author Jaap Reitsma
 *
 */
		
@Stateless
public class QuoteProcessor {
	public static final int MINIMUM_FARE = 1;
	public static final int CREDITS_PER_KILOMETER = 1;
	
	public void onQuoteRequested(@Observes(during = TransactionPhase.IN_PROGRESS) Leg leg) {
    	if (RideManager.AGENCY_ID.equals(leg.getAgencyId())) {
    		if (leg.getDistance() == null) {
    			throw new IllegalStateException(String.format("Cannot calculate fare for %s, no distance defined!",leg.toString()));
    		}
    		leg.setFareInCredits(Math.toIntExact(Math.round((leg.getDistance() / 1000.0) * CREDITS_PER_KILOMETER)));
    		leg.setFareInCredits(Math.max(leg.getFareInCredits(), MINIMUM_FARE));
    	} else {
    		throw new IllegalStateException(String.format("Cannot calculate fare, agency does not support credits: %s (%s)", leg.getAgencyId(), leg.getAgencyName()));
    	}
    }

	public void onQuoteRequested(@Observes(during = TransactionPhase.IN_PROGRESS) Booking booking) {
    	int distance = booking.getLegs().stream().collect(Collectors.summingInt(eu.netmobiel.rideshare.model.Leg::getDistance));
		booking.setFareInCredits(Math.max(MINIMUM_FARE, 
										  Math.toIntExact(Math.round((distance / 1000.0) * CREDITS_PER_KILOMETER))));
    }
}
