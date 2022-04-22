package eu.netmobiel.profile.filter;

public class ReviewFilter extends BiPartyFilter {
	
	private String context;
	
	public ReviewFilter() {
	}

	public ReviewFilter(String receiver, String sender) {
		super(receiver, sender);
	}
	
	public ReviewFilter(String receiver, String sender, String context) {
		super(receiver, sender);
		this.context = context;
	}
	
	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ReviewFilter [");
		if (this.context != null) {
			builder.append("context");
			builder.append(" ");
		}
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}
}
