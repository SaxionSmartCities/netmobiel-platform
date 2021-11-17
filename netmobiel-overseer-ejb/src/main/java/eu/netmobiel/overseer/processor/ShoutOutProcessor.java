package eu.netmobiel.overseer.processor;

import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.event.ShoutOutResolvedEvent;
import eu.netmobiel.planner.event.TravelOfferEvent;
import eu.netmobiel.planner.model.Itinerary;
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
    private ProfileManager profileManager;

    @Inject
    private RideManager rideManager;
    @Inject
    private IdentityHelper identityHelper;
    
    @Inject
    private BookingManager bookingManager;

    @Inject
    private TripPlanManager tripPlanManager;

    @Resource
    private SessionContext context;

    @Inject
    private TextHelper textHelper;
    
//    @Asynchronous
    public void onShoutOutRequested(@Observes(during = TransactionPhase.IN_PROGRESS) TripPlan event) throws BusinessException {
    	// We have a shout-out request
		List<Profile> profiles = profileManager.searchShoutOutProfiles(event.getFrom(), event.getTo(), DRIVER_MAX_RADIUS_METERS, DRIVER_NEIGHBOURING_RADIUS_METERS);
		if (! profiles.isEmpty()) {
			String topic = textHelper.createDriverShoutOutTopic(event);
			Message msg = new Message();
			msg.setContext(event.getPlanRef());
			msg.setBody(textHelper.createDriverShoutOutMessage(event)); 
			msg.setDeliveryMode(DeliveryMode.ALL);
			// Start or continue conversation for all recipients with planRef context
			// The recipients are by definition Drivers (in the conversation)
			publisherService.lookupOrCreateConversations(profiles, UserRole.DRIVER, event.getPlanRef(), topic, true)
				.forEach(conversation -> msg.addRecipient(conversation, event.getPlanRef()));
			// And send the message
			publisherService.publish(null, msg);
		}
    }

    /**
     * Handles the TravelOfferEvent. A driver has found an trip plan for himself in which a traveller can take part. 
     * The handler creates a ride for the driver according the trip plan calculated before, adds a booking for the traveller.
     * When is the traveller's itinerary saved?
     * Note tthat might not yet be a driver's conversation started, because that would be the case only if the driver live's 
     * close enough near the traveller.
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

    	// Assign additional contexts to the driver's conversation: Ride
		// This is later on used to bundle messages related to the trip plan
		NetMobielUser nbUser = identityHelper.resolveUserUrn(event.getDriverRef())
				.orElseThrow(() -> new IllegalStateException("Unknown driver: " + event.getDriverRef()));
		// At this moment there might already be conversation already started with the tripplan. If not, then create one.
		Conversation driverConv = publisherService.lookupOrCreateConversation(nbUser, UserRole.DRIVER, sop.getPlanRef(), textHelper.createDriverShoutOutTopic(sop), true);
		publisherService.addConversationContext(driverConv, r.getUrn(), textHelper.createRideTopic(r), true);
    	
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
		// Add the booking also to the driver's context. This might also be done by the rideshare
		// TODO Check who is responsible
		// Add the booking also as context of the driver
		publisherService.addConversationContext(driverConv, b.getUrn());

		// Find the conversation of the passenger. There might not yet be a conversation started
		String passengerTopic = textHelper.createPassengerShoutOutTopic(sop);
		Conversation passengerConv = publisherService.lookupOrCreateConversation(b.getPassenger(), 
				UserRole.PASSENGER, sop.getPlanRef(), passengerTopic, true);
		Message msg = new Message();
		// The message context is the sender's context, in this case the system
		msg.setContext(b.getUrn());
		msg.setDeliveryMode(DeliveryMode.ALL);
		// The context of the passenger is the plan. The passenger can find the leg by looking for the message context in the plan
		msg.addRecipient(passengerConv, sop.getPlanRef());
		msg.setBody(textHelper.createPassengerTravelOfferMessageBody(r));
		publisherService.publish(null, msg);
		// Inform the delegates, if any. They receive limited information only. The delegate can switch to the delegator view and see the normal messages.
		publisherService.informDelegates(b.getPassenger(), textHelper.informDelegateNewTravelOfferText(r), DeliveryMode.ALL);
    }

    /**
     * Handler on the event for resolving an shout-out into an itinerary.
     * @param event
     * @throws BusinessException
     */
    public void onShoutOutResolved(@Observes(during = TransactionPhase.IN_PROGRESS) ShoutOutResolvedEvent event) throws BusinessException {
    	TripPlan shoutOutPlan = event.getTrip().getItinerary().getTripPlan();
    	tripPlanManager.resolveShoutOut(shoutOutPlan, event.getTrip().getItinerary());
    	// Add the new trip to the conversation of the passenger
    	// Just to be sure, create the conversation if not already there.
		Conversation passengerConv = publisherService.lookupOrCreateConversation(shoutOutPlan.getTraveller(),  
				UserRole.PASSENGER, shoutOutPlan.getPlanRef(), textHelper.createPassengerShoutOutTopic(shoutOutPlan), true);
		// Add the trip and adapt the conversation topic
		publisherService.addConversationContext(passengerConv, event.getTrip().getTripRef(), textHelper.createPassengerTripTopic(event.getTrip()), true);
    }


}
