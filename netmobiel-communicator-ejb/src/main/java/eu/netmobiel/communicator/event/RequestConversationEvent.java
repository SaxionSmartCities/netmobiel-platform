package eu.netmobiel.communicator.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * This event is issued when a conversation is looked up but not found. 
 * The event listener is requested to create the conversation.
 * 
 * @author Jaap Reitsma
 *
 */
public class RequestConversationEvent implements Serializable {

	private static final long serialVersionUID = 7122249211097974597L;

	@NotNull
    private String context;

    @NotNull
    private NetMobielUser user;

    public RequestConversationEvent(String aContext, NetMobielUser aUser) {
    	this.context = aContext;
    	this.user = aUser;
    }

	public String getContext() {
		return context;
	}

	public NetMobielUser getUser() {
		return user;
	}

}
