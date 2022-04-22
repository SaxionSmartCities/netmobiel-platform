package eu.netmobiel.profile.filter;

import eu.netmobiel.commons.filter.BaseFilter;
import eu.netmobiel.profile.model.UserRole;

public class ProfileFilter extends BaseFilter {
	private String text;
	private UserRole userRole;
	
	public ProfileFilter() {
		// Constructor
	}

	public ProfileFilter(String text, UserRole userRole) {
		this.text = text;
		this.userRole = userRole;
	}
	
	public UserRole getUserRole() {
		return userRole;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setUserRole(UserRole userRole) {
		this.userRole = userRole;
	}

	public void setUserRole(String role) {
		UserRole ur = role == null ? null : UserRole.valueOf(role);
		this.userRole = ur;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (text != null) {
			builder.append("t=");
			builder.append(text);
			builder.append(" ");
		}
		if (userRole != null) {
			builder.append("ur=");
			builder.append(userRole);
			builder.append(" ");
		}
		builder.append(super.toString());
		return builder.toString();
	}
}
