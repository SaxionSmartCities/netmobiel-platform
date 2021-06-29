package eu.netmobiel.here.search.model;

/**
 * The type retuned by the reverse geocoding method.
 * 
 * @see https://developer.here.com/documentation/geocoding-search-api/api-reference-swagger.html
 * @author Jaap Reitsma
 * 
 * Example: <pre>
   {
        "title": "Winterswijkseweg 88, 7142 JK Groenlo, Nederland",
        "id": "here:af:streetsection:T.4VQF7OkoICHnSQe.jTlC:CggIBCCPl7imAhABGgI4OA",
        "resultType": "houseNumber",
        "houseNumberType": "PA",
        "address": {
            "label": "Winterswijkseweg 88, 7142 JK Groenlo, Nederland",
            "countryCode": "NLD",
            "countryName": "Nederland",
            "stateCode": "GE",
            "state": "Gelderland",
            "county": "Oost Gelre",
            "city": "Groenlo",
            "street": "Winterswijkseweg",
            "postalCode": "7142 JK",
            "houseNumber": "88"
        },
        "position": {
            "lat": 52.00315,
            "lng": 6.6542
        },
        "access": [
            {
                "lat": 52.00291,
                "lng": 6.65427
            }
        ],
        "distance": 24,
        "mapView": {
            "west": 6.63989,
            "south": 52.0029,
            "east": 6.65524,
            "north": 52.01807
        }
    }</pre>
 *
 */
public class GeocodeResultItem {
	// NOTE: Some attributes are omitted, we don't need them.
    public String title;
    public String id;
    public ResultType resultType;
    public AddressType address;
    public CoordinateType position;
    public CoordinateType[] access;
    public Integer distance;
    public MapViewType mapView;
	
	@Override
	public String toString() {
		return String.format("GeocodeResult[%s' %s %s distance %s]", title, position, resultType, 
				distance != null ? distance.toString() : "?");
	}
	
	
}
