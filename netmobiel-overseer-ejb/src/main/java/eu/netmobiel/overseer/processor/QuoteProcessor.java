package eu.netmobiel.overseer.processor;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.rideshare.service.RideManager;

/**
 * Stateless bean for issueing quotes on travel costs etc.
 *  
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
public class QuoteProcessor {
	public static final int MINIMUM_FARE = 1;
	public static final int CREDITS_PER_KILOMETER = 1;
	
    @Inject
    private Logger logger;
    
//    @Asynchronous
    public void onQuoteRequested(@Observes(during = TransactionPhase.IN_PROGRESS) Leg leg) {
    	if (RideManager.AGENCY_ID.equals(leg.getAgencyId())) {
    		if (leg.getDistance() == null) {
    			logger.warn("Cannot calculate fare, no distance defined!");
    		} else {
        		leg.setFareInCredits(Math.toIntExact(Math.round((leg.getDistance() / 1000.0) * CREDITS_PER_KILOMETER)));
        		leg.setFareInCredits(Math.max(leg.getFareInCredits(), MINIMUM_FARE));
    		}
    	} else {
			logger.warn(String.format("Cannot calculate fare, agency does not support credits: %s (%s)", leg.getAgencyId(), leg.getAgencyName()));
    	}
    }

}
