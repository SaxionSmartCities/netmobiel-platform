package eu.netmobiel.commons.model;

public class NetMobielUserImpl implements NetMobielUser {
	private String managedIdentity;
	private String givenName;
	private String familyName;
	private String email;
	
	public NetMobielUserImpl() {
		
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
	
}
