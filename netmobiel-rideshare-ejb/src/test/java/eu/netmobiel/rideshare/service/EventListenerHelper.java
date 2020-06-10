package eu.netmobiel.rideshare.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;

import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.annotation.Updated;
import eu.netmobiel.commons.model.event.BookingCancelledEvent;
import eu.netmobiel.commons.model.event.BookingConfirmedEvent;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;

/**
 * Help class for listening to events in the Rideshare classes. 
 * 
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
@Logging
public class EventListenerHelper {
    private BookingConfirmedEvent lastBookingConfirmedEvent;
    private int bookingConfirmedEventCount;
    private BookingCancelledEvent lastBookingCancelledEvent;
    private int bookingCancelledEventCount;
    private Booking lastBookingCreatedEvent;
    private int bookingCreatedEventCount;
    private Booking lastBookingRemovedEvent;
    private int bookingRemovedEventCount;
    private Ride lastRideItineraryStaleEvent;
    private int rideItineraryStaleEventCount;
    private Ride lastRideRemovedEvent;
    private int rideRemovedEventCount;
    
    public void reset() {
		lastBookingCancelledEvent = null;
		bookingCancelledEventCount = 0;
		lastBookingConfirmedEvent = null;
		bookingConfirmedEventCount = 0;
		lastBookingCreatedEvent = null;
		bookingCreatedEventCount = 0;
		lastBookingRemovedEvent = null;
		bookingRemovedEventCount = 0;
		lastRideItineraryStaleEvent = null;
		rideItineraryStaleEventCount = 0;
		lastRideRemovedEvent = null;
		rideRemovedEventCount = 0;
    	
    }
    public void onBookingConfirmedEvent(@Observes BookingConfirmedEvent bce) {
    	lastBookingConfirmedEvent = bce;
    	bookingCancelledEventCount++;
    }

    public void onBookingCancelledEvent(@Observes BookingCancelledEvent bce) {
    	lastBookingCancelledEvent = bce;
    	bookingCancelledEventCount++;
    }
    public void onBookingCreatedEvent(@Observes(notifyObserver = Reception.ALWAYS) @Created Booking b) {
    	lastBookingCreatedEvent = b;
    	bookingCreatedEventCount++;
    }
    public void onBookingRemovedEvent(@Observes @Removed Booking b) {
    	lastBookingRemovedEvent = b;
    	bookingRemovedEventCount++;
    }
    public void onRideItineraryStaleEvent(@Observes @Updated Ride r) {
    	lastRideItineraryStaleEvent = r;
    	rideItineraryStaleEventCount++;
    }
    public void onRideRemovedEvent(@Observes @Removed Ride r) {
    	lastRideRemovedEvent = r;
    	rideRemovedEventCount++;
    }

    public BookingConfirmedEvent getLastBookingConfirmedEvent() {
		return lastBookingConfirmedEvent;
	}
	public int getBookingConfirmedEventCount() {
		return bookingConfirmedEventCount;
	}
	public BookingCancelledEvent getLastBookingCancelledEvent() {
		return lastBookingCancelledEvent;
	}
	public int getBookingCancelledEventCount() {
		return bookingCancelledEventCount;
	}
	public Booking getLastBookingCreatedEvent() {
		return lastBookingCreatedEvent;
	}
	public int getBookingCreatedEventCount() {
		return bookingCreatedEventCount;
	}
	public Booking getLastBookingRemovedEvent() {
		return lastBookingRemovedEvent;
	}
	public int getBookingRemovedEventCount() {
		return bookingRemovedEventCount;
	}
	public Ride getLastRideItineraryStaleEvent() {
		return lastRideItineraryStaleEvent;
	}
	public int getRideItineraryStaleEventCount() {
		return rideItineraryStaleEventCount;
	}
	public Ride getLastRideRemovedEvent() {
		return lastRideRemovedEvent;
	}
	public int getRideRemovedEventCount() {
		return rideRemovedEventCount;
	}
    
}
