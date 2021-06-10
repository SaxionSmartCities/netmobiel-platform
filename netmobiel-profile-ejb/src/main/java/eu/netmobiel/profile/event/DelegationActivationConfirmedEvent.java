package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationActivationConfirmedEvent {
	private Delegation delegation;
	
	public DelegationActivationConfirmedEvent(Delegation delegation) {
		this.delegation= delegation;
	}

	public Delegation getDelegation() {
		return delegation;
	}

}
