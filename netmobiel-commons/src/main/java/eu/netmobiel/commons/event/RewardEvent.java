package eu.netmobiel.commons.event;

import java.io.Serializable;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Event issued on successful completion of an activity.  
 * 
 * @author Jaap Reitsma
 *
 */
public class RewardEvent extends RewardBaseEvent implements Serializable {
	private static final long serialVersionUID = -7420478775206484689L;

	/**
	 * The original yield on which to base the reward, in case of a relative reward.
	 */
	private Integer yield;
	
	public RewardEvent(String aCode, NetMobielUser aRecipient, String aContext) {
		this(aCode, aRecipient, aContext, null);
	}

	public RewardEvent(String aCode, NetMobielUser aRecipient, String aContext, Integer aYield) {
		super(aCode, aRecipient, aContext);
		this.yield = aYield;
	}

	public Integer getYield() {
		return yield;
	}
}
