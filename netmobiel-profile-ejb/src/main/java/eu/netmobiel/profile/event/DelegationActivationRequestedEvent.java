package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationActivationRequestedEvent extends DelegationEvent {
	
	public DelegationActivationRequestedEvent(Delegation delegation) {
		super(delegation);
	}
}
