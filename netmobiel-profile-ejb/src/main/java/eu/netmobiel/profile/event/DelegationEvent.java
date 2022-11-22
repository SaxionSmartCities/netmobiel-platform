package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Delegation;

public class DelegationEvent {
	private Delegation delegation;
	
	public DelegationEvent(Delegation delegation) {
		this.delegation= delegation;
	}

	public Delegation getDelegation() {
		return delegation;
	}

}
