package eu.netmobiel.profile.filter;

import eu.netmobiel.commons.filter.BaseFilter;

public class BiPartyFilter extends BaseFilter {
	/** ============================== 
	 * Selection of reviews or compliments
	 */
	private String receiver;
	private String sender;
	
	public BiPartyFilter() {
	}

	public BiPartyFilter(String receiver, String sender) {
		this.receiver = receiver;
		this.sender = sender;
	}
	
	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (receiver != null) {
			builder.append("receiver=");
			builder.append(receiver);
			builder.append(" ");
		}
		if (sender!= null) {
			builder.append("sender=");
			builder.append(sender);
			builder.append(" ");
		}
		builder.append(super.toString());
		return builder.toString();
	}
}
