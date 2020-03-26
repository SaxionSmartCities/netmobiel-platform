package eu.netmobiel.here.places.model;

/**
 * The media type <code>urn:nlp-types:autosuggest</code> returned by the Places Api call <code>autosuggest</code>.
 * 
 * @see https://developer.here.com/documentation/places/dev_guide/topics_api/media-type-autosuggest.html
 * @author Jaap Reitsma
 *
 */
public class AutosuggestMediaType {
	/**
	 * A list of autosuggestions;
	 */
	public AutosuggestPlace[] results;
}
