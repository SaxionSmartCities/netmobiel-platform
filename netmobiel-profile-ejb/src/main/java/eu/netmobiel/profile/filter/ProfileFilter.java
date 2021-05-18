package eu.netmobiel.profile.filter;

import eu.netmobiel.commons.filter.BaseFilter;
import eu.netmobiel.profile.model.UserRole;

public class ProfileFilter extends BaseFilter {
	private String text;
	private UserRole userRole;
	
	public ProfileFilter() {
	}

	public ProfileFilter(String text, UserRole userRole) {
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
		UserRole userRole = role == null ? null : UserRole.valueOf(role);
		this.userRole = userRole;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (text != null) {
			builder.append("text=");
			builder.append(text);
			builder.append(", ");
		}
		if (userRole != null) {
			builder.append("userRole=");
			builder.append(userRole);
			builder.append(", ");
		}
		builder.append(super.toString());
		return builder.toString();
	}
}
