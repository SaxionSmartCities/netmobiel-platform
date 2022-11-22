package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationTransferCompletedEvent extends DelegationTransferEvent {
	
	public DelegationTransferCompletedEvent(Delegation sourceDelegation, Delegation targetDelegation, boolean immediateTransfer) {
		super(sourceDelegation, targetDelegation, immediateTransfer);
	}

}
