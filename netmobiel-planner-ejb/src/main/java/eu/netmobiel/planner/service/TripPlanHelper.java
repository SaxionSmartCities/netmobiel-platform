package eu.netmobiel.planner.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.TraverseMode;
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
    public void assignRideToPassengerLeg(Leg leg, Ride ride) {
		leg.setDriverId(ride.getDriverRef());
		leg.setDriverName(ride.getDriver().getName());
		leg.setVehicleId(ride.getCarRef());
		leg.setVehicleLicensePlate(ride.getCar().getLicensePlate());
		leg.setVehicleName(ride.getCar().getName());
		leg.setTripId(ride.getUrn());
		leg.setTraverseMode(TraverseMode.RIDESHARE);
		// For Rideshare booking is always required.
		leg.setBookingRequired(true);
		// For Rideshare confirmation is requested from traveller and provider
		leg.setConfirmationByProviderRequested(true);
		leg.setConfirmationRequested(true);
		// Request synchronously a quote
		quoteRequestedEvent.fire(leg);
		// Quote received now
    }


}