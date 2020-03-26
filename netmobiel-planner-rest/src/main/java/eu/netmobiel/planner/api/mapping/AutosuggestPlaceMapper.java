package eu.netmobiel.planner.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.here.places.model.AutosuggestPlace;

/**
 * This mapper defines the mapping from the HERE Places API to out GeocodeSuggestion API as defined by NetMObiel in OpenAPI.
 * One way only.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface AutosuggestPlaceMapper {
	// HERE Places --> Api suggestions
	eu.netmobiel.planner.api.model.GeocodeSuggestion map(AutosuggestPlace source);

}
