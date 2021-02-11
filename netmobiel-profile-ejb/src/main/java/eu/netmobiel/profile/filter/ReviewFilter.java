package eu.netmobiel.profile.filter;

public class ReviewFilter extends BiPartyFilter {
	
	public ReviewFilter() {
	}

	public ReviewFilter(String receiver, String sender) {
		super(receiver, sender);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ReviewFilter [");
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}
}
