package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Profile;

public class DelegatorAccountCreatedEvent extends DelegatorAccountEvent {
	
	public DelegatorAccountCreatedEvent(Profile initiator, Profile delegator) {
		super(initiator, delegator);
	}
}
