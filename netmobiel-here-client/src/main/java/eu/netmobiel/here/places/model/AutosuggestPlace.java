package eu.netmobiel.here.places.model;

import java.util.Arrays;

/**
 * The media type <code>urn:nlp-types:autosuggest</code> returned by the Places Api call <code>autosuggest</code>.
 * 
 * @see https://developer.here.com/documentation/places/dev_guide/topics_api/media-type-autosuggest.html
 * @author Jaap Reitsma
 *
 */
public class AutosuggestPlace {
	/**
	 * A title for this place to be displayed to the user.
	 */
	public String title;
	/**
	 * The same content as the 'title' field. However, it contains markup highlighting the parts of the string that were matched.
	 */
	public String highlightedTitle;
	/**
	 * Optional. The textual description of the location of the place; usually derived from the address of the place, 
	 * but may also contain any other description that helps a user understand where the place is located.
	 */
	public String vicinity;
	/**
	 * Optional. The same content as the 'vicinity' field. However, it contains markup highlighting the parts of the string that were matched.
	 */
	public String highlightedVicinity;
	/**
	 * The latitude and longitude of the place, for example [37.785141,-122.4047775]. 
	 * This latitude and longitude is suitable for displaying the place's position on a map.
	 */
	public Double[] position;
	/**
	 * Optional. A category id or name for this place.
	 */
	public String category;
	/**
	 * Optional. The (localized) display name of the category. For example, Eat & Drink.
	 */
	public String categoryTitle;
	/**
	 * A bounding box that is associated with this place, e.g. for moving the map view.
	 */
	public Double[] bbox;
	/**
	 * A hyperlink that refers to the resource with details for this place.
	 */
	public String href;
	/**
	 * All Autosuggest place and address results have type 'urn:nlp-types:place''
	 * All Autosuggest query completion results have type 'urn:nlp-types:autosuggest'
	 */
	public String type;
	/**
	 * The type of Autosuggest result item which can be 'place' for a Place and 'address' for an Address.
	 */
	public String resultType;
	/**
	 * The unique identifier of the place.
	 */
	public String id;
	/**
	 * Optional. Distance to the destination in meters calculated as described in Search Location and Distance Calculation.
	 */
	public Integer distance;
	/**
	 * Optional. List of IDs of the chains that this place belongs to. Chains are large brands with multiple locations.
	 */
	public String[] chainIds;
	/**
	 * The suggested query completion
	 */
	public String completion;
	
	@Override
	public String toString() {
		return String.format("AutoSuggestion ['%s' %s %s distance %s]", title, Arrays.toString(position), resultType, 
				distance != null ? distance.toString() : "?");
	}
	
	
}
