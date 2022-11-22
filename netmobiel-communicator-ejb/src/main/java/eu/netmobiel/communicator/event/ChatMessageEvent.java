package eu.netmobiel.communicator.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.communicator.model.Message;

/**
 * This event is issued when a trip execution is confirmed (either affirmative or denied) by the passenger. 
 * 
 * @author Jaap Reitsma
 *
 */
public class ChatMessageEvent implements Serializable {
	private static final long serialVersionUID = 2270398465038857971L;

    @NotNull
    private Message chatMessage;

    public ChatMessageEvent(Message aMessage) {
    	this.chatMessage = aMessage;
    }

	public Message getChatMessage() {
		return chatMessage;
	}
    
}
