package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.TripPlan;

/**
 * This event is issued when a passenger confirms a booking proposal for a leg. Assumption: The leg is already persisted.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingProposalRejectedEvent implements Serializable {
	private static final long serialVersionUID = -8453852001191953795L;
	
	/**
     * The plan involved in the booking proposal.
     */
    @NotNull
    private TripPlan plan;
	/**
     * The leg involved in the booking.
     */
    @NotNull
    private Leg leg;
    
	/**
     * The reason for cancelling.
     */
    @Size(max = 256)
    private String cancelReason;

    public BookingProposalRejectedEvent(TripPlan aPlan, Leg aLeg, String aReason) {
    	this.plan = aPlan;
    	this.leg = aLeg;
    	this.cancelReason = aReason;
    }

    public TripPlan getPlan() {
		return plan;
	}

    public Leg getLeg() {
		return leg;
	}

    public String getCancelReason() {
		return cancelReason;
	}

	@Override
	public String toString() {
		return String.format("%s [%s %s]", this.getClass().getSimpleName(), getPlan().getId(), getLeg().getId());
	}

}
