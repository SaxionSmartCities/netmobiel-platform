package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationTransferCompletedEvent {
	private Delegation from;
	private Delegation to;
	private boolean immediate;
	
	public DelegationTransferCompletedEvent(Delegation sourceDelegation, Delegation targetDelegation, boolean immediateTransfer) {
		this.from = sourceDelegation;
		this.to = targetDelegation;
		this.immediate = immediateTransfer;
	}

	public Delegation getFrom() {
		return from;
	}

	public Delegation getTo() {
		return to;
	}

	public boolean isImmediate() {
		return immediate;
	}
	
	
}
