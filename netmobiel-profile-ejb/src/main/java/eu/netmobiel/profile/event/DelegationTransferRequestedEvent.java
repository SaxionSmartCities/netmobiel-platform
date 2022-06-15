package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationTransferRequestedEvent extends DelegationTransferEvent {
	
	public DelegationTransferRequestedEvent(Delegation sourceDelegation, Delegation targetDelegation, boolean immediateTransfer) {
		super(sourceDelegation, targetDelegation, immediateTransfer);
	}

}
