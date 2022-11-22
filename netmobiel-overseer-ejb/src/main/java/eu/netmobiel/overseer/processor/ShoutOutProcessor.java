package eu.netmobiel.overseer.processor;

import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.event.RequestConversationEvent;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.event.ShoutOutResolvedEvent;
import eu.netmobiel.planner.event.TravelOfferEvent;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.TripPlanManager;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.ProfileManager;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.service.RideManager;

/**
 * Stateless bean for the management of the high-level shout-out process, involving multiple modules.
 *  
 * The Overseer is intended as a kind of message handler. In case of distributed modules, the Overseer must use 
 * real messaging (e.g. Apache Kafka, ActiveMQ) , instead or in addition to CDI events.  
 * 
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
@RunAs("system") 
public class ShoutOutProcessor {
	private static final int DRIVER_MAX_RADIUS_METERS = 50000;
	private static final int DRIVER_NEIGHBOURING_RADIUS_METERS = 20000;
	
    @Inject
    private PublisherService publisherService;

    @Inject
    private DelegationProcessor delegationProcessor; 

    @Inject
    private ProfileManager profileManager;

    @Inject
    private RideManager rideManager;
    
    @Inject
    private BookingManager bookingManager;

    @Inject
    private TripPlanManager tripPlanManager;

    @Resource
    private SessionContext context;

    @Inject
    private TextHelper textHelper;
    
//    @Asynchronous
    public void onShoutOutRequested(@Observes(during = TransactionPhase.IN_PROGRESS) TripPlan shoutOutPlan) throws BusinessException {
    	// We have a shout-out request
    	// Message context is the shout-out (trip plan), recipient's context too.
    	// Conversation will be new directed at drivers, there is at most one shout-out (trip plan) in a conversation.
		List<Profile> profiles = profileManager.searchShoutOutProfiles(shoutOutPlan.getTraveller().getManagedIdentity(), 
				shoutOutPlan.getFrom(), shoutOutPlan.getTo(), DRIVER_MAX_RADIUS_METERS, DRIVER_NEIGHBOURING_RADIUS_METERS);
		if (! profiles.isEmpty()) {
			String topic = textHelper.createDriverShoutOutTopic(shoutOutPlan);
			Message.MessageBuilder mb = Message.create()
					.withBody(textHelper.createDriverShoutOutMessage(shoutOutPlan))
					.withContext(shoutOutPlan.getPlanRef());
			for (Profile rcp : profiles) {
				mb.addEnvelope()
					.withRecipient(rcp)
	    			.withConversationContext(shoutOutPlan.getPlanRef())
	    			.withUserRole(UserRole.DRIVER)
	    			.withTopic(topic)
					.buildConversation();
			}
			Message msg = mb.buildMessage();
			publisherService.publish(msg);
		}
    }

    /**
     * Handles the TravelOfferEvent. A driver has found an trip plan for himself in which a traveller can take part. 
     * The handler creates a ride for the driver according the trip plan calculated before, adds a booking for the traveller.
     * When is the traveller's itinerary saved?
     * Note there might not yet be a driver's conversation started, e.g., when driver is reacting to shout-out outside the normal driver's shout-out range.
     * @param event
     * @throws BusinessException
     */
    public void onTravelOfferProposed(@Observes(during = TransactionPhase.IN_PROGRESS) TravelOfferEvent event) 
    		throws BusinessException  {
    	if (! event.getProposedPlan().getTraverseModes().contains(TraverseMode.RIDESHARE)) {
    		throw new BadRequestException("Only RIDESHARE modality is supported");
    	}
    	// We know only one rideshare provider and that's us.
    	if (event.getProposedPlan().getItineraries().size() != 1) {
    		throw new BadRequestException("Proposed plan should contain exactly 1 itinerary");
    	}
    	TripPlan pp = event.getProposedPlan();
    	TripPlan sop = event.getShoutOutPlan();
    	Itinerary soi = event.getShoutOutItinerary();
    	Ride r = new Ride();
    	r.setCarRef(event.getVehicleRef());
    	r.setDriverRef(event.getDriverRef());
    	r.setFrom(pp.getFrom());
    	r.setTo(pp.getTo());
    	if (pp.isUseAsArrivalTime()) {
    		r.setArrivalTime(pp.getTravelTime());
    	} else {
    		r.setDepartureTime(pp.getTravelTime());
    	}
    	rideManager.createRide(r);

    	Booking b = new Booking();
    	b.setDepartureTime(soi.getDepartureTime());
    	b.setArrivalTime(soi.getArrivalTime());
    	b.setPickup(soi.getFrom());
    	b.setDropOff(soi.getTo());
    	b.setNrSeats(pp.getNrSeats());
    	b.setState(BookingState.PROPOSED);
		// Which reference do we give to the booking? We do not have a trip yet. We do have a plan and an itinerary.
		// The reference is only needed by the transport provider to inform the planner on a cancel of the ride,
		// or to update some details like the car. Implicitly the reference is used to find the planner (in case of an
		// external service). So the use from the perspective of the transport provider is twofold: 
		// Find the service that booked a ride, and find the specific trip or trip plan within that service.
    	
		// As a principle we should not use an itinerary as key. Each change will create a new itinerary. Therefore, we 
		// use a trip (refers to an itinerary) or the shout-out trip plan. The latter is only used in case of proposals.
    	// The booking will keep track of both references. First we don't like the use of a field to point to objects of 
    	// different type, depending on the state. Secondly, we want to report on the use of the field as an measure 
    	// for usage of the shout-out feature by the rideshare drivers.
    	b.setPassengerTripPlanRef(sop.getPlanRef());
		String bookingRef = bookingManager.createBooking(r.getUrn(), sop.getTraveller(), b);
		tripPlanManager.assignBookingProposalReference(RideManager.AGENCY_ID, soi, r, bookingRef);

		// The driver is informed when something is done with bookings

		// Inform the passenger about the new offer
		// The message context is the booking
		// Passenger's context is the plan
    	Message passengerMsg = Message.create()
    			.withBody(textHelper.createPassengerTravelOfferMessageBody(r))
    			.withContext(bookingRef)
    			.addEnvelope(sop.getPlanRef())
	    			.withRecipient(b.getPassenger())
	    			.withConversationContext(sop.getPlanRef())
	    			.withUserRole(UserRole.PASSENGER)
	    			.withTopic(textHelper.createPassengerShoutOutTopic(sop))
	    			.buildConversation()
    			.buildMessage();
		publisherService.publish(passengerMsg);
		// Inform the delegates, if any. They receive limited information only. The delegate can switch to the delegator view and see the normal messages.
		delegationProcessor.informDelegates(b.getPassenger(), textHelper.informDelegateNewTravelOfferText(r), DeliveryMode.ALL);
    }

    /**
     * Handler on the event for resolving an shout-out into an itinerary for a trip.
     * The conversation for the shout-out trip plan is connected to the new trip. 
     * @param event
     * @throws BusinessException
     */
    public void onShoutOutResolved(@Observes(during = TransactionPhase.IN_PROGRESS) ShoutOutResolvedEvent event) throws BusinessException {
    	TripPlan shoutOutPlan = event.getTrip().getItinerary().getTripPlan();
    	tripPlanManager.resolveShoutOut(shoutOutPlan, event.getTrip().getItinerary());
    	Optional<Leg> rsleg = event.getTrip().getItinerary().findLegByTraverseMode(TraverseMode.RIDESHARE);
    	String driverName = rsleg.isPresent() ? rsleg.get().getDriverName() : null;
    	// Add the new trip to the conversation of the passenger
    	// The conversation context is the shout-out plan
    	// The message context is the new trip
    	// The recipient's context is the new trip too
    	Message msg = Message.create()
    			.withBody(textHelper.createPassengerShoutOutResolvedBody(driverName))
    			.withContext(event.getTrip().getTripRef())
    			.withDeliveryMode(DeliveryMode.MESSAGE)		// No notification needed for myself
    			.addEnvelope()
	    			.withRecipient(shoutOutPlan.getTraveller())
	    			.withConversationContext(shoutOutPlan.getPlanRef())
	    			.withUserRole(UserRole.PASSENGER)
	    			.withTopic(textHelper.createPassengerTripTopic(event.getTrip()))
	    			.buildConversation()
    			.buildMessage();
    	publisherService.publish(msg);
    }

    /**
     * The communicator has received a message for a certain conversation, but the conversation has apparently not started yet.
     * Try to start a conversation, but only if this is about a shout-out (i.e., a trip plan). 
     * @param event
     * @throws BusinessException
     */
    public void onRequestConversation(@Observes(during = TransactionPhase.IN_PROGRESS) RequestConversationEvent event) throws BusinessException {
    	if (UrnHelper.getPrefix(event.getContext()).equals(TripPlan.URN_PREFIX)) {
    		// Ok, it is about a trip plan (shout-out). Get information about this trip plan.
        	Long sid = UrnHelper.getId(TripPlan.URN_PREFIX, event.getContext());
        	TripPlan shoutOutPlan = tripPlanManager.getShoutOutPlan(sid);
        	Conversation c = new Conversation(new CommunicatorUser(event.getUser()), event.getContext());
        	if (shoutOutPlan.getTraveller().equals(event.getUser())) {
        		// It is my own shoutout!
        		c.setOwnerRole(UserRole.PASSENGER);
    			c.setTopic(textHelper.createPassengerShoutOutTopic(shoutOutPlan));
        		
        	} else {
        		// It is somebody else's shout-out, I am a driver, apparently
        		c.setOwnerRole(UserRole.DRIVER);
        		c.setTopic(textHelper.createDriverShoutOutTopic(shoutOutPlan));
        	}
        	publisherService.createConversation(c);
    	}
    }
}
