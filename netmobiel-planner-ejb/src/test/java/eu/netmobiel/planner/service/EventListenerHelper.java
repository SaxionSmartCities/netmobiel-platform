package eu.netmobiel.planner.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import eu.netmobiel.commons.model.event.BookingCancelledEvent;
import eu.netmobiel.commons.model.event.BookingRequestedEvent;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.TripPlan;

/**
 * Help class for listening to events in the Planner classes. 
 * 
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
@Logging
public class EventListenerHelper {
    private BookingRequestedEvent lastBookingRequestedEvent;
    private int bookingRequestedEventCount;
    private BookingCancelledEvent lastBookingCancelledEvent;
    private int bookingCancelledEventCount;
    private TripPlan lastTripPlan;
    private int TripPlanCount;
    
    public void reset() {
		lastBookingCancelledEvent = null;
		bookingCancelledEventCount = 0;
		lastBookingRequestedEvent = null;
		bookingRequestedEventCount = 0;
		lastTripPlan = null;
		TripPlanCount = 0;
    }

    public void onBookingRequestedEvent(@Observes BookingRequestedEvent bce) {
    	lastBookingRequestedEvent = bce;
    	bookingRequestedEventCount++;
    }

	public void onBookingCancelledEvent(@Observes BookingCancelledEvent bce) {
    	lastBookingCancelledEvent = bce;
    	bookingCancelledEventCount++;
    }
    
	public void onTripPlan(@Observes TripPlan plan) {
    	lastTripPlan = plan;
    	TripPlanCount++;
    }

	public BookingRequestedEvent getLastBookingRequestedEvent() {
		return lastBookingRequestedEvent;
	}

	public int getBookingRequestedEventCount() {
		return bookingRequestedEventCount;
	}

	public BookingCancelledEvent getLastBookingCancelledEvent() {
		return lastBookingCancelledEvent;
	}

	public int getBookingCancelledEventCount() {
		return bookingCancelledEventCount;
	}

	public TripPlan getLastTripPlan() {
		return lastTripPlan;
	}

	public int getTripPlanCount() {
		return TripPlanCount;
	}
	
}
