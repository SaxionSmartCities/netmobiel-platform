package eu.netmobiel.overseer.processor;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Ride;

/**
 * Helper class for matting all message posted to the inbox.
 *  
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
public class TextHelper {
	public static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";
	public static final String DEFAULT_LOCALE = "nl-NL";

	private Locale locale;
	private ZoneId timeZone;

	public TextHelper() {
		this(DEFAULT_LOCALE, DEFAULT_TIME_ZONE);
	}
	
	public TextHelper(String localeName, String zoneName) {
    	this.locale = Locale.forLanguageTag(localeName);
    	this.timeZone = ZoneId.of(zoneName);
	}
	
    public String formatDate(Instant instant) {
    	return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(instant.atZone(timeZone));
    }

    public String formatTime(Instant instant) {
    	return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(instant.atZone(timeZone));
    }

    /***************  SHOUT-OUT  *************/
    
	public String createPassengerShoutOutTopic(TripPlan plan) {
		return MessageFormat.format("Gezocht: Meerijden op {0} van {1} naar {2}", 
				formatDate(plan.getTravelTime()),
				plan.getFrom().getLabel(), 
				plan.getTo().getLabel()
		);
	}

	public String createDriverShoutOutTopic(TripPlan plan) {
		return createPassengerShoutOutTopic(plan);
	}

	public String createPassengerTravelOfferMessageBody(Ride ride) {
	    return MessageFormat.format("Je kunt meerijden met {0}. Bevestig je keuze.",  
	    		ride.getDriver().getGivenName()
	    		);
	}
	
	public String createDriverShoutOutMessage(TripPlan plan) {
		return MessageFormat.format("{0} zoekt vervoer ({1} rond {2}). Wie kan helpen?",
					plan.getTraveller().getGivenName(),
					plan.isUseAsArrivalTime() ? "aankomst" : "vertrek",
					formatTime(plan.getTravelTime())
				);
	}
	
    /***************  TRIP  *************/
	
	public String createPassengerTripTopic(Trip trip) {
		return MessageFormat.format("Reis op {0} van {1} naar {2}", 
				formatDate(trip.getItinerary().getDepartureTime()),
				trip.getFrom().getLabel(), 
				trip.getTo().getLabel()
		);
	}
	
    public String travelsWith(Set<String> agencies) {
    	// FIXME Als er geen agency is, dan moet het per voet zijn
    	String desc = "te voet";
    	List<String> ags = new ArrayList<>(agencies);
    	if (ags.size() == 1) {
    		desc = "met " + ags.get(0);
    	} else if (ags.size() > 1) {
    		String last = ags.remove(ags.size() - 1);
    		desc = "met " + String.join( " en ", String.join(", ", ags), last);
    	}
    	return desc;
    }

    public String createTripDepartureText(Trip trip ) {
    	return MessageFormat.format("Vertrek om {0} uur. Je reist {1}.", 
				formatTime(trip.getItinerary().getDepartureTime()),
				travelsWith(trip.getAgencies())
		);
    }
	
    public String informDelegateTripDepartureText(Trip trip ) {
    	return MessageFormat.format("{0} gaat bijna op pad!", trip.getTraveller().getName()); 
    }

    public String createTripReviewRequestText(Trip trip) {
		return "Heb je de reis gemaakt? Geef jouw waardering en beoordeel deze reis."; 
    }

    public String informDelegateTripReviewRequestText(Trip trip ) {
    	return MessageFormat.format("De reis van {0} zit erop, geef een beoordeling.", 
    			trip.getTraveller().getName()
    	);
    }

    public String createTripReviewRequestReminderText(Trip trip) {
		return "Je reis zit erop! Geef jouw waardering en beoordeel deze reis."; 
    }

    public String informDelegateTripReviewReminderText(Trip trip ) {
    	return MessageFormat.format("Herinnering: De reis van {0} zit erop, geef een beoordeling.", 
    			trip.getTraveller().getName()
    	);
    }

    /***************  RIDE & BOOKING  *************/
	
    public String createDescriptionText(Leg leg) {
    	String prefix = null;
    	if (leg.getTraverseMode() == TraverseMode.RIDESHARE) {
    		prefix = String.format("Meerijden met %s", leg.getDriverName()); 
    	} else if (leg.getTraverseMode().isTransit()) {
    		prefix = String.format("Reizen met %s", leg.getAgencyName()); 
    	} else {
    		throw new IllegalStateException("TraverseMode is not supported: " + leg.getTraverseMode());
    	}
		return MessageFormat.format("{0} van {1} naar {2} op {3}", 
				prefix, leg.getFrom().getLabel(), 
				leg.getTo().getLabel(), formatDate(leg.getStartTime()));
    }

	public String createRideTopic(Ride r) {
		return MessageFormat.format("Rit op {0} van {1} naar {2}", 
				formatDate(r.getDepartureTime()),
				r.getFrom().getLabel(), 
				r.getTo().getLabel()
		);
	}

	public String createBookingCreatedText(Booking booking) {
		String text = null;
		if (booking.getState() == BookingState.PROPOSED) {
			text = MessageFormat.format("Je hebt een aanbod gedaan om {0} te laten meerijden.", 
					booking.getPassenger().getGivenName());
		} else if (booking.getState() == BookingState.REQUESTED) {
			text = MessageFormat.format("{0} rijdt graag met je mee vanaf {1}.",
					booking.getPassenger().getGivenName(), 
					booking.getPickup().getLabel());
		} else if (booking.getState() == BookingState.CONFIRMED) {
			text = MessageFormat.format("{0} rijdt graag met je mee vanaf {1} naar {2}.",
					booking.getPassenger().getGivenName(), 
					booking.getPickup().getLabel(), 
					booking.getDropOff().getLabel());
		} else {
			throw new IllegalStateException("Unexpected booking state with booking " + booking.toString());
		}
		return text; 
	}
	
	public String informDelegateNewTravelOfferText(Ride ride) {
		return MessageFormat.format("Nieuwe reisaanbieding van {0}.", ride.getDriver().getName()); 
	}

	public String createBookingCancelledByPassengerText(Booking booking) {
		String text = null;
		if (booking.getState() == BookingState.PROPOSED) {
			text = MessageFormat.format("{0} heeft een andere oplossing gevonden. Bedankt voor je aanbod!", 
					booking.getPassenger().getGivenName());
		} else {
			String reason = ".";
			if (booking.getCancelReason() != null && !booking.getCancelReason().isEmpty()) {
				reason = ": " + booking.getCancelReason().trim();
			}
			return MessageFormat.format("{0} rijdt niet meer met je mee{1}", 
					booking.getPassenger().getGivenName(), reason
				);
		}
		return text; 
	}

	public String createDriverCancelledBookingText(Booking booking) {
		String reason = ".";
		if (booking.getCancelReason() != null && !booking.getCancelReason().isEmpty()) {
			reason = ": " + booking.getCancelReason().trim();
		}
		return MessageFormat.format("Je kunt helaas niet meer met {0} meerijden{1}", 
			booking.getRide().getDriver().getGivenName(), reason
		);
	}
	
	public String informDelegateCancelledBookingText(Booking booking) {
		return MessageFormat.format("Chauffeur {0} heeft geannuleerd.", booking.getRide().getDriver().getName()); 
	}

    public String createRideDepartureText(Ride ride, Booking booking) {
    	return MessageFormat.format("Vertrek om {0} uur naar {1}. Je wordt verwacht door {2}.",
				formatTime(ride.getDepartureTime()),
				booking.getPickup().getLabel(), 
				booking.getPassenger().getGivenName()

		);
    }

    public String createRideReviewRequestText(Ride ride, Booking booking) {
		return MessageFormat.format("Heb je {0} meegenomen naar {1}? Claim je credits en beoordeel je passagier!", 
				booking.getPassenger().getGivenName(),
				booking.getPickup().getLabel() 
		);
    }
	
    public String createRideReviewRequestReminderText(Ride ride, Booking booking) {
		return "Claim je credits en beoordeel je passagier!"; 
    }

    
    /***************  DELEGATION  *************/
	
	public String createDelegationTopic(Delegation d) {
		return MessageFormat.format("Beheer van reizen van {0}", d.getDelegator().getName());
	}

	public String createDelegatorAccountCreatedText(Profile delegator, Profile initiator) {
		return MessageFormat.format("U hebt een account bij NetMobiel: {0}. Uw registratie is uitgevoerd door {1}.", 
				delegator.getNameAndEmail(), 
				initiator.getNameEmailPhone()
		);
	}

	public String createDelegationActivationText(Delegation delegation) {
		return MessageFormat.format("{0} biedt aan om uw reizen met NetMobiel voor u te beheren. "
				+ "Uw instemming geeft u door deze persoon desgevraagd de volgende verificatiecode te geven: {1}.", 
				delegation.getDelegate().getName(), delegation.getActivationCode());
	}
	
	public String createDelegationConfirmedToDelegatorText(Delegation delegation) {
		return MessageFormat.format("{0} beheert vanaf nu uw reizen met NetMobiel.", 
				delegation.getDelegate().getName()
		);
	}
	
	public String createDelegationConfirmedToDelegateText(Delegation delegation) {
		return MessageFormat.format("U beheert vanaf nu de reizen van {0} met NetMobiel.", 
				delegation.getDelegator().getName()
		);
	}
	
	public String createTransferDelegationToText(Delegation fromDelegation) {
		return MessageFormat.format("{0} vraagt u om het beheer van de reizen in Netmobiel namens {1} over te nemen. " 
				+ "Vraag {2} om de via SMS ontvangen activeringscode en vul deze in op het overdrachtsformulier in de app.", 
				fromDelegation.getDelegate().getName(), 
				fromDelegation.getDelegator().getName(),
				fromDelegation.getDelegator().getGivenName()
		);
	}

	public String createTransferDelegationCompletedText(Delegation fromDelegation, Delegation toDelegation) {
		return MessageFormat.format("Uw beheer van de reizen in Netmobiel namens {0} is overgedragen aan {1}.", 
				fromDelegation.getDelegator().getName(), toDelegation.getDelegate().getName()
		);
	}
}
