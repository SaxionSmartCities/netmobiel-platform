package eu.netmobiel.commons.event;

import java.io.Serializable;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Event issued on successful completion of some activity. this event will cause some kind of evaluation,
 * leading to a decision to reward or to rollback (or do nothing). 
 * 
 * @author Jaap Reitsma
 *
 */
public class RewardEvaluationEvent extends RewardBaseEvent implements Serializable {
	private static final long serialVersionUID = -7420478775206484689L;

	public RewardEvaluationEvent(String aCode, NetMobielUser aRecipient, String aContext) {
		super(aCode, aRecipient, aContext);
	}

}
