package eu.netmobiel.planner.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.rideshare.model.Ride;

/**
 * A helper for building a trip plan.
 *  
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
@Logging
public class TripPlanHelper {

    @Inject
    private Event<Leg> quoteRequestedEvent;

    /**
     * Assign rideshare attributes to the car/rideshare legs. This functionality should probably be put closer to the rideshare service itself.
     * @param leg The rideshare leg for the passenger 
     * @param ride The ride carrying the passenger. 
     */
    public void assignRideToPassengerLeg(TripPlan plan, Leg leg, Ride ride) {
    	// Use the driver keycloak urn for easier use in shout-out and by client.
		leg.setDriverId(ride.getDriver().getKeyCloakUrn());
		leg.setDriverName(ride.getDriver().getName());
		leg.setVehicleId(ride.getCarRef());
		leg.setVehicleLicensePlate(ride.getCar().getLicensePlate());
		leg.setVehicleName(ride.getCar().getName());
		leg.setTripId(ride.getUrn());
		Integer emission = ride.getCar().getCo2Emission();
		if (emission == null) {
			// Probably an old car, lot's of CO2 and really bad fumes, assume 1:10, 1 liter benzine about 2.3 kg CO2.
			// Source: https://www.co2emissiefactoren.nl/wp-content/uploads/2022/08/CO2emissiefactoren-2022-2015-dd-14-7-2022.pdf
			emission = 230; // [g / vehicle km]
		}
		// Calculate the emission rate for the car for the total number of occupied seats, including the driver. 
		leg.setCo2EmissionRate(emission / (plan.getNrSeats() + 1));

		assignFareToRideshareLeg(leg);
		
		// For Rideshare booking is always required.
		leg.setBookingRequired(true);
		// For Rideshare confirmation is requested from traveller and provider
		leg.setConfirmationByProviderRequested(true);
		leg.setConfirmationRequested(true);
    }

    /**
     * Assign rideshare attributes to the car/rideshare legs. This functionality should probably be put closer to the rideshare service itself.
     * @param leg The rideshare leg for the passenger 
     */
    public void assignFareToRideshareLeg(Leg leg) {
		leg.setTraverseMode(TraverseMode.RIDESHARE);
		// Request synchronously a quote
		quoteRequestedEvent.fire(leg);
		// Quote received now
    }
}