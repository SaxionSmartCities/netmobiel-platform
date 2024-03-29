package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Profile;

public class DelegatorAccountEvent {
	private Profile initiator;
	private Profile delegator;
	
	public DelegatorAccountEvent(Profile initiator, Profile delegator) {
		this.initiator = initiator;
		this.delegator = delegator;
	}

	public Profile getInitiator() {
		return initiator;
	}

	public Profile getDelegator() {
		return delegator;
	}


}
