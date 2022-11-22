package eu.netmobiel.commons.model;

import java.io.Serializable;
import java.util.Objects;

public class NetMobielUserImpl implements NetMobielUser, Serializable {
	private static final long serialVersionUID = -635795360808038387L;
	private String managedIdentity;
	private String givenName;
	private String familyName;
	private String email;
	
	public NetMobielUserImpl() {
		
	}

	public NetMobielUserImpl(String id) {
		this(id, null, null, null);
	}

	public NetMobielUserImpl(String id, String givenName, String familyName, String email) {
		this.managedIdentity = id;
		this.givenName = givenName;
		this.familyName = familyName;
		this.email = email;
	}

	@Override
	public String getManagedIdentity() {
		return managedIdentity;
	}

	@Override
	public String getGivenName() {
		return givenName;
	}

	@Override
	public String getFamilyName() {
		return familyName;
	}

	@Override
	public String getEmail() {
		return email;
	}

	public void setManagedIdentity(String managedIdentity) {
		this.managedIdentity = managedIdentity;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(managedIdentity);
	}
	
	@Override
	public boolean isSame(NetMobielUser other) {
		return equals(other) && 
				Objects.equals(getEmail(), other.getEmail()) && 
				Objects.equals(getFamilyName(), other.getFamilyName()) && 
				Objects.equals(getGivenName(), other.getGivenName()) 
		;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NetMobielUserImpl)) {
			return false;
		}
		NetMobielUserImpl other = (NetMobielUserImpl) obj;
		return Objects.equals(managedIdentity, other.managedIdentity);
	}
	@Override
	public String toString() {
		return String.format("%s %s %s %s", managedIdentity, givenName, familyName, email);
	}

}
