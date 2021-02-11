package eu.netmobiel.profile.filter;

public class ComplimentFilter extends BiPartyFilter {
	
	public ComplimentFilter() {
	}

	public ComplimentFilter(String receiver, String sender) {
		super(receiver, sender);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ComplimentFilter [");
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}

}
