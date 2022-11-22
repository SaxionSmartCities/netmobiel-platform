package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationActivationConfirmedEvent extends DelegationEvent {
	
	public DelegationActivationConfirmedEvent(Delegation delegation) {
		super(delegation);
	}

}
