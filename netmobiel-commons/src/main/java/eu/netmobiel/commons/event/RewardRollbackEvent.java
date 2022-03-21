package eu.netmobiel.commons.event;

import java.io.Serializable;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * This event is issued when a reward is rolled-back and subsequently the payment is withdrawn/reversed. 
 * The event is primarily intended to ease the testing of multiple scenarios going forward and backward in the process.
 * 
 * @author Jaap Reitsma
 *
 */
public class RewardRollbackEvent extends RewardBaseEvent implements Serializable {
	private static final long serialVersionUID = -207888621286042374L;
	private boolean paymentOnly = false;
	
	public RewardRollbackEvent(String aCode, NetMobielUser aRecipient, String aFactContext) {
		this(aCode, aRecipient, aFactContext, false);
    }

	public RewardRollbackEvent(String aCode, NetMobielUser aRecipient, String aFactContext, boolean onlyThePayment) {
		super(aCode, aRecipient, aFactContext);
    	this.paymentOnly = onlyThePayment;
    }

	public boolean isPaymentOnly() {
		return paymentOnly;
	}

	@Override
	public String toString() {
		return String.format("%s %s", super.toString(), paymentOnly);
	}

}
