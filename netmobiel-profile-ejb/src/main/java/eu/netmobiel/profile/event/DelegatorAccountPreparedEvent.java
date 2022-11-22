package eu.netmobiel.profile.event;

import eu.netmobiel.profile.model.Profile;

public class DelegatorAccountPreparedEvent extends DelegatorAccountEvent {
	
	public DelegatorAccountPreparedEvent(Profile initiator, Profile delegator) {
		super(initiator, delegator);
	}

}
