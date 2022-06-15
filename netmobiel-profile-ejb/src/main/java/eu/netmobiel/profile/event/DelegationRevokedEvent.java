package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationRevokedEvent extends DelegationEvent {
	
	public DelegationRevokedEvent(Delegation delegation) {
		super(delegation);
	}
}
