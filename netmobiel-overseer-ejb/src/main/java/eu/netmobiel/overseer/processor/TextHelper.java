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

import eu.netmobiel.banker.model.Reward;
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
	
	public String createPassengerShoutOutResolvedBody(String driverName) {
		return MessageFormat.format("Je hebt het aanbod van {0} geaccepteerd. Eventuele andere aanbieders zijn automatisch geïnformeerd.",
				driverName != null ? driverName : "iemand");
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

	public String createTravelOfferTopic(Ride r) {
		return MessageFormat.format("Aangeboden rit op {0} van {1} naar {2}", 
				formatDate(r.getDepartureTime()),
				r.getFrom().getLabel(), 
				r.getTo().getLabel()
		);
	}

	public String createBookingTextForPassenger(Booking booking) {
		String text = null;
		if (booking.getState() == BookingState.REQUESTED) {
			text = MessageFormat.format("Je hebt {0} gevraagd of je kunt meerijden.",
					booking.getRide().getDriver().getGivenName());
		} else if (booking.getState() == BookingState.CONFIRMED) {
			text = MessageFormat.format("Je kunt meerijden met {0}.",
					booking.getRide().getDriver().getGivenName());
		} else {
			throw new IllegalStateException("Unexpected booking state (passenger view) with booking " + booking.toString());
		}
		return text; 
	}

	public String createBookingCreatedTextForDriver(Booking booking) {
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
			throw new IllegalStateException("Unexpected booking state (driver view) with booking " + booking.toString());
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
		return MessageFormat.format("Je hebt een account bij Netmobiel: {0}. Uw registratie is uitgevoerd door {1}.", 
				delegator.getNameAndEmail(), 
				initiator.getNameEmailPhone()
		);
	}

	public String createDelegationActivationText(Delegation delegation) {
		return MessageFormat.format("{0} biedt aan om je reizen met Netmobiel voor je te beheren. "
				+ "Je instemming geef je door deze persoon desgevraagd de volgende verificatiecode te geven: {1}.", 
				delegation.getDelegate().getName(), delegation.getActivationCode());
	}
	
	public String createDelegationConfirmedToDelegatorText(Delegation delegation) {
		return MessageFormat.format("{0} beheert vanaf nu je reizen met Netmobiel.", 
				delegation.getDelegate().getName()
		);
	}
	
	public String createDelegationConfirmedToDelegateText(Delegation delegation) {
		return MessageFormat.format("Je beheert vanaf nu de reizen van {0} met Netmobiel.", 
				delegation.getDelegator().getName()
		);
	}
	
	public String createTransferDelegationToText(Delegation fromDelegation) {
		return MessageFormat.format("{0} vraagt je om het beheer van de reizen in Netmobiel namens {1} over te nemen. " 
				+ "Vraag {2} om de via SMS ontvangen activeringscode en vul deze in op het overdrachtsformulier in de app.", 
				fromDelegation.getDelegate().getName(), 
				fromDelegation.getDelegator().getName(),
				fromDelegation.getDelegator().getGivenName()
		);
	}

	public String createTransferDelegationCompletedText(Delegation fromDelegation, Delegation toDelegation) {
		return MessageFormat.format("Het beheer van de reizen in Netmobiel namens {0} is overgedragen aan {1}.", 
				fromDelegation.getDelegator().getName(), toDelegation.getDelegate().getName()
		);
	}
	
	public String createDelegationRevokedText(Delegation delegation) {
		return MessageFormat.format("Je beheert niet langer de reizen van {0}.", 
				delegation.getDelegator().getName()
		);
	}
	
    /***************  REWARDS  *************/

	public String createPremiumRewardText(Reward reward) {
		return MessageFormat.format("Je hebt {0} premiecredits verdiend met: {1}", 
				reward.getAmount(), reward.getIncentive().getDescription() 
		);
	}
	
	public String createRedemptionRewardText(Reward reward) {
		return MessageFormat.format("Je hebt {0} credits verzilverd met: {1}", 
				reward.getAmount(), reward.getIncentive().getDescription() 
		);
	}

	public String createRewardText(Reward reward) {
		String text = null;
		if (reward.getIncentive().isRedemption()) {
			text = createRedemptionRewardText(reward);
		} else {
			text = createPremiumRewardText(reward);
		}
		return text;
	}

	/***************  GENERIC  *************/
	public String createPersonalGenericTopic() {
		return "Persoonlijke berichten"; 
	}
}
