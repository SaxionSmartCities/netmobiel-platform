package eu.netmobiel.commons.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * This is a base class for incentive events. 
 * 
 * @author Jaap Reitsma
 *
 */
public abstract class RewardBaseEvent implements Serializable {
	private static final long serialVersionUID = 3292781642972855209L;

	/**
	 * A unique code designating the incentive definition, in fact the activity. 
	 */
    @NotNull
    private String incentiveCode;

    /**
     * The user being rewarded for the activity. 
     */
    @NotNull
    private NetMobielUser recipient;

    /**
     * A URN referring to the activity object in the system: A booking, a survey interaction etc.
     */
    @NotNull
    private String factContext;
    
	protected RewardBaseEvent(String aCode, NetMobielUser aRecipient, String aFactContext) {
    	this.incentiveCode = aCode;
    	this.recipient = aRecipient;
    	this.factContext = aFactContext;
    }

	public String getIncentiveCode() {
		return incentiveCode;
	}

	public NetMobielUser getRecipient() {
		return recipient;
	}

	public String getFactContext() {
		return factContext;
	}

	@Override
	public String toString() {
		return String.format("%s %s %s %s", this.getClass().getSimpleName(), incentiveCode, recipient.getManagedIdentity(), factContext);
	}
}
