package eu.netmobiel.rideshare.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Coordinate;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.opentripplanner.api.model.PlanResponse;
import eu.netmobiel.opentripplanner.api.model.TraverseMode;
import eu.netmobiel.opentripplanner.client.OpenTripPlannerClient;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Leg;
import eu.netmobiel.rideshare.model.PlannerReport;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideshareResult;
import eu.netmobiel.rideshare.model.ToolType;
import eu.netmobiel.rideshare.repository.mapping.LegMapper;

@ApplicationScoped
public class OpenTripPlannerDao {
	public static final Integer OTP_MAX_WALK_DISTANCE = 500;

	@Inject
    private Logger log;
    
    @Inject
    private OpenTripPlannerClient otpClient;
    
    @Inject
    private LegMapper otplegMapper;

    /**
     * Call the OpenTripPlanner to create a trip plan for a ride with optionally a single booking.
     * Due to the booking restriction, at most three legs are created. If the passenger's pickup and/or drop-off
     * location coincides with the driver's departure and/or arrival location, then will be only two or even just one leg.
     * @param ride The ride to calculate the legs for.
     * @param via The locations in between the driver's departure and arrival.
     * @return An array of legs, organised as a single graph.  
     * @throws NotFoundException When no itinerary could be found. 
     * @throws BadRequestException When the planner cannot plan due to the combination of parameters.
     */
    public Leg[] createItinerary(Ride ride, List<GeoLocation> via) throws NotFoundException, BadRequestException {
    	Instant travelTime = ride.isArrivalTimePinned() ? ride.getArrivalTime() : ride.getDepartureTime();
    	// If there are bookings, then there are probably, but not necessarily, intermediate stops, for 1 booking that can be 0, 1 or 2 stops. 
   		
    	PlanResponse result = otpClient.createPlan(null, ride.getFrom(), ride.getTo(), 
    			travelTime, ride.isArrivalTimePinned(), new TraverseMode[] { TraverseMode.CAR }, 
    			false, OTP_MAX_WALK_DISTANCE, null, via.toArray(new GeoLocation[via.size()]), 1);
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
		Leg[] legs = otplegMapper.legsToLegs(result.plan.itineraries.get(0).legs.toArray(new eu.netmobiel.opentripplanner.api.model.Leg[] {}));
		return legs;
    }

    /**
     * Call the OpenTripPlanner to create a trip plan for a ride with optionally a single booking.
     * Due to the booking restriction, at most three legs are created. If the passenger's pickup and/or drop-off
     * location coincides with the driver's departure and/or arrival location, then will be only two or even just one leg.
     * @param ride The ride to calculate the legs for.
     * @param pickup The pickup location of the passenger.
     * @param dropOff The dropOff location of the passenger.
     * @return An array of legs, organised as a single graph.  
     * @throws NotFoundException When no itinerary could be found. 
     * @throws BadRequestException When the planner cannot plan due to the combination of parameters.
     */
    public Leg[] createItinerary(Ride ride, GeoLocation pickup, GeoLocation dropOff) throws NotFoundException, BadRequestException {
    	final List<GeoLocation> via = new ArrayList<>();
		via.add(pickup);
 		via.add(dropOff);
 		return createItinerary(ride, via);
    }

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
    			.filter(b -> b.getState() == BookingState.CONFIRMED || b.getState() == BookingState.PROPOSED)
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
   		return createItinerary(ride, via);
    }
    
    /**
     * Call the OpenTripPlanner to create a trip plan for a ride with optionally a single booking.
     * Due to the booking restriction, at most three legs are created. If the passenger's pickup and/or drop-off
     * location coincides with the driver's departure and/or arrival location, then will be only two or even just one leg.
     * @param ride The ride to calculate the legs for.
     * @param via The locations in between the driver's departure and arrival.
     * @return An array of legs, organised as a single graph.  
     * @throws NotFoundException When no itinerary could be found. 
     * @throws BadRequestException When the planner cannot plan due to the combination of parameters.
     */
    @SuppressWarnings("resource")
	public RideshareResult createSharedRide(Instant now, Ride orgRide, List<GeoLocation> via) {
    	Instant travelTime = orgRide.isArrivalTimePinned() ? orgRide.getArrivalTime() : orgRide.getDepartureTime();
    	// If there are bookings, then there are probably, but not necessarily, intermediate stops, for 1 booking that can be 0, 1 or 2 stops. 
    	PlannerReport report = new PlannerReport();
    	report.setTravelTime(travelTime);
    	report.setUseAsArrivalTime(orgRide.isArrivalTimePinned());
    	report.setFrom(orgRide.getFrom());
    	report.setTo(orgRide.getTo());
    	report.setMaxResults(1);
    	report.setLenientSearch(false);
    	report.setRequestGeometry(GeometryHelper.createLines(
    			orgRide.getFrom().getPoint().getCoordinate(), 
    			orgRide.getTo().getPoint().getCoordinate(), 
    			via.stream().map(loc -> loc.getPoint().getCoordinate()).toArray(Coordinate[]::new)));
    	report.setToolType(ToolType.OPEN_TRIP_PLANNER);
    	report.setViaLocations(via);
    	RideshareResult rideshareResult = new RideshareResult(report);
    	long start = System.currentTimeMillis();
    	try {
    		final int maxResults = 1;
	    	PlanResponse result = otpClient.createPlan(null, orgRide.getFrom(), orgRide.getTo(), 
	    			travelTime, orgRide.isArrivalTimePinned(), new TraverseMode[] { TraverseMode.CAR }, 
	    			false, OTP_MAX_WALK_DISTANCE, null, via.toArray(new GeoLocation[via.size()]), maxResults);
			if (result.error != null) {
				String msg = String.format("OTP Planner Error: %s - %s", result.error.message, result.error.msg);
				if (result.error.missing != null && result.error.missing.size() > 0) {
					msg = String.format("%s Missing parameters [ %s ]", msg, String.join(",", result.error.missing));
				}
				report.setErrorText(msg);
				report.setErrorVendorCode(result.error.message.name());
				report.setStatusCode(result.error.message.getStatus().getStatusCode());
			} else {
				report.setStatusCode(Response.Status.OK.getStatusCode());
				Leg[] legs = otplegMapper.legsToLegs(result.plan.itineraries.get(0).legs.toArray(new eu.netmobiel.opentripplanner.api.model.Leg[] {}));
	    		List<Leg> newLegs = Arrays.asList(legs);
	    		Ride ride = new Ride();
		    	newLegs.forEach(leg -> ride.addLeg(leg));
		    	ride.addStop(newLegs.get(0).getFrom());
		    	newLegs.forEach(leg -> ride.addStop(leg.getTo()));
		    	
		    	ride.setId(orgRide.getId());
		    	ride.setCar(orgRide.getCar());
		    	ride.setDriver(orgRide.getDriver());
		    	ride.setDepartureTime(ride.getLegs().get(0).getStartTime());
	    		ride.setArrivalTime(ride.getLegs().get(ride.getLegs().size() - 1).getEndTime());
	    		ride.setFrom(ride.getStops().get(0).getLocation());
	    		ride.setTo(ride.getStops().get(ride.getStops().size() - 1).getLocation());
	        	rideshareResult.setPage(new PagedResult<>(Collections.singletonList(ride), 10, 0, 1L));
				report.setNrResults(maxResults);

			}
		} catch (NotFoundException ex) {
			report.setErrorText(ex.getMessage());
			report.setErrorVendorCode(ex.getVendorCode());
			report.setStatusCode(Response.Status.NOT_FOUND.getStatusCode());
		} catch (WebApplicationException ex) {
			report.setErrorText(ex.getMessage());
			report.setStatusCode(ex.getResponse().getStatus());
		} catch (Exception ex) {
			report.setErrorText(String.join(" - ", ExceptionUtil.unwindExceptionMessage("Error calling OTP", ex)));
			report.setErrorVendorCode(ex.getClass().getSimpleName());
			report.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
		}
    	report.setExecutionTime(System.currentTimeMillis() - start);
		return rideshareResult;
    }

}
