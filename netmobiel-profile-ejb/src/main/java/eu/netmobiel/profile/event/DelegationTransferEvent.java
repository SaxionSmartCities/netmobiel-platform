package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationTransferEvent {
	private Delegation from;
	private Delegation to;
	private boolean immediate;
	
	public DelegationTransferEvent(Delegation sourceDelegation, Delegation targetDelegation, boolean immediateTransfer) {
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
