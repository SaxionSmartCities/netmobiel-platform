package eu.netmobiel.here.search.model;

/**
 * The type of the address field.
 * 
 * @see https://developer.here.com/documentation/geocoding-search-api/api-reference-swagger.html
 * 
 * @author Jaap Reitsma
 * 
 */
public class AddressType {
	/**
	 * A title for this place to be displayed to the user.
	 */
	public String label;
	public String countryCode;
	public String countryName;
	public String stateCode;
	public String state;
	public String county;
	public String city;
	public String street;
	public String postalCode;
	public String houseNumber;
	
	public AddressType() {
		
	}

	@Override
	public String toString() {
		return String.format("Address [%s]", label);
	}
	
	public String getPostalCodeNoSpace() {
		return postalCode == null ? null : postalCode.replaceAll(" ", ""); 
	}
}
