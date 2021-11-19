package eu.netmobiel.profile.filter;

public class ComplimentsFilter extends BiPartyFilter {

	private String context;
	
	public ComplimentsFilter() {
	}

	public ComplimentsFilter(String receiver, String sender) {
		super(receiver, sender);
	}

	public ComplimentsFilter(String receiver, String sender, String context) {
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
		builder.append("ComplimentFilter [");
		builder.append(super.toString());
		if (this.context != null) {
			builder.append(", ");
			builder.append("context");
		}
		builder.append("]");
		return builder.toString();
	}

}
