package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationActivationRequestedEvent {
	private Delegation delegation;
	
	public DelegationActivationRequestedEvent(Delegation delegation) {
		this.delegation= delegation;
	}

	public Delegation getDelegation() {
		return delegation;
	}

}
