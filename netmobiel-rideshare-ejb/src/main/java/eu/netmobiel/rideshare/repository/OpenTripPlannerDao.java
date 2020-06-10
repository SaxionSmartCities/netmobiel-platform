package eu.netmobiel.rideshare.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.opentripplanner.api.model.PlanResponse;
import eu.netmobiel.opentripplanner.api.model.TraverseMode;
import eu.netmobiel.opentripplanner.client.OpenTripPlannerClient;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Leg;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.repository.mapping.LegMapper;

@ApplicationScoped
public class OpenTripPlannerDao {
	private static final Integer OTP_MAX_WALK_DISTANCE = 500;

	@Inject
    private Logger log;
    
    @Inject
    private OpenTripPlannerClient otpClient;
    
    @Inject
    private LegMapper tripPlanMapper;


    /**
     * Call the OpenTripPlanner to create a trip plan for a ride with optionally a single booking.
     * Due to the booking restriction, at most three legs are created. If the passenger's pickup and/or drop-off
     * location coincides with the driver's departure and/or arrival location, then will be only two or even just one leg.
     * @param ride The ride to calculate the legs for.
     * @return An array of legs, organised as a single graph.  
     * @throws NotFoundException When no itinerary could be found. 
     * @throws BadRequestException When the planner cannot plan due to the combination of parameters.
     */
    public Leg[] createItinerary(Ride ride) throws NotFoundException, BadRequestException {
    	List<Booking> bookings = ride.getBookings().stream()
    			.filter(b -> b.getState() == BookingState.CONFIRMED)
    			.collect(Collectors.toList());
    	if (bookings.size() > 1) {
    		throw new IllegalStateException("Only 1 active booking is allowed per ride");
    	}

    	final List<GeoLocation> via = new ArrayList<>();
    	//TODO With more bookings it is necessary to verify the sequence of pickups and dropoffs 
   		for (Booking b: bookings) {
   			via.add(b.getPickup());
   			via.add(b.getDropOff());
		}
    	Instant travelTime = ride.isArrivalTimePinned() ? ride.getArrivalTime() : ride.getDepartureTime();
    	// If there are bookings, then there are probably, but not necessarily, intermediate stops, for 1 booking that can be 0, 1 or 2 stops. 
   		
    	PlanResponse result = otpClient.createPlan(ride.getFrom(), ride.getTo(), 
    			travelTime, ride.isArrivalTimePinned(), new TraverseMode[] { TraverseMode.CAR }, 
    			false, OTP_MAX_WALK_DISTANCE, via.toArray(new GeoLocation[via.size()]), 1);
		if (result.error != null) {
			String msg = String.format("OTP Planner Error: %s - %s", result.error.message, result.error.msg);
			if (result.error.missing != null && result.error.missing.size() > 0) {
				msg = String.format("%s Missing parameters [ %s ]", msg, String.join(",", result.error.missing));
			}
			if (result.error.message.getStatus().getStatusCode() >= 500) {
				throw new SystemException(msg, result.error.message.name());
			} else if (result.error.message.getStatus() == Response.Status.NOT_FOUND) {
				throw new NotFoundException(msg, result.error.message.name());
			} else {
				throw new BadRequestException(msg, result.error.message.name());
			}
		}
    	if (log.isDebugEnabled()) {
        	log.debug("Create plan for ride: \n" + result.plan.toString());
    	}
		Leg[] legs = tripPlanMapper.legsToLegs(result.plan.itineraries.get(0).legs.toArray(new eu.netmobiel.opentripplanner.api.model.Leg[] {}));
		return legs;
    }
    
}
