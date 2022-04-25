package eu.netmobiel.geoservice.api.mapping;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;

import eu.netmobiel.geoservice.api.model.Suggestion.CategoryEnum;
import eu.netmobiel.here.search.api.model.Address;
import eu.netmobiel.here.search.api.model.AutosuggestEntityResultItem;
import eu.netmobiel.here.search.api.model.AutosuggestEntityResultItem.ResultTypeEnum;
import eu.netmobiel.here.search.api.model.Category;
import eu.netmobiel.here.search.api.model.OpenSearchAutosuggestResponse;

/**
 * This mapper defines the mapping from the HERE autosuggest service OpenSearchAutosuggestResponse to the API Suggestion as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, uses = { GeometryMapper.class } )
public abstract class SuggestionMapper {

	@Mapping(target = "data", source = "items")
	@Mapping(target = "totalCount", ignore = true)
	@Mapping(target = "count", ignore = true)
	@Mapping(target = "offset", ignore = true)
	public abstract eu.netmobiel.geoservice.api.model.Page map(OpenSearchAutosuggestResponse source);

	@AfterMapping
    protected void addCategory(OpenSearchAutosuggestResponse source, @MappingTarget eu.netmobiel.geoservice.api.model.Page target) {
		target.setOffset(0);
		target.setCount(target.getData().size());
		target.setTotalCount(target.getCount());
	}

	public List<Object> map(List<AutosuggestEntityResultItem> source) {
		// Only return results with an acceptable result type.
		return source.stream()
				.map(item -> map(item))
				.filter(sug -> sug.getResultType() != null)
				.collect(Collectors.toList());
	}

	// HERE Suggestion --> API Suggestion
	@Mapping(target = "titleHighlights", source = "highlights.title")
	@Mapping(target = "category", ignore = true)	// use after mapping for category
	public abstract eu.netmobiel.geoservice.api.model.Suggestion map(AutosuggestEntityResultItem source);

	@ValueMappings({
        @ValueMapping(target = MappingConstants.NULL, source = MappingConstants.ANY_REMAINING),
    })
	public abstract eu.netmobiel.geoservice.api.model.Suggestion.ResultTypeEnum map(AutosuggestEntityResultItem.ResultTypeEnum source);

	@Mapping(target = "locality", source = "city")
	public abstract eu.netmobiel.geoservice.api.model.Address map(Address source);

	@AfterMapping
    protected void addCategory(AutosuggestEntityResultItem source, @MappingTarget eu.netmobiel.geoservice.api.model.Suggestion target) {
		Category cat = source.getCategories().stream()
				.filter(c -> c.getPrimary().equals(Boolean.TRUE))
				.findFirst()
				.orElse(null);
		if (source.getResultType() == ResultTypeEnum.PLACE && cat != null) {
			switch (cat.getId()) {
			// See documentation HERE client
			case "400-4000-4581":
				target.setCategory(CategoryEnum.AIRPORT);
				break;
			case "400-4100-0036":
				target.setCategory(CategoryEnum.BUS_STATION);
				break;
			case "400-4100-0035":
				target.setCategory(CategoryEnum.RAILWAY_STATION);
				break;
			case "400-4100-0047 ":
				target.setCategory(CategoryEnum.TAXI_STAND);
				break;
			default:
				if (cat.getId().startsWith("500-5000-")) {
					target.setCategory(CategoryEnum.HOTEL);
				} else if (cat.getId().startsWith("100-1000-")) {
					target.setCategory(CategoryEnum.RESTAURANT);
				} else if (cat.getId().startsWith("300-")) {
					target.setCategory(CategoryEnum.SIGHTS_MUSEUMS);
				} else if (cat.getId().startsWith("800-8600-")) {
					target.setCategory(CategoryEnum.SPORTS_FACILITY_VENUE);
				} else if (cat.getId().startsWith("200-2200-")) {
					target.setCategory(CategoryEnum.THEATRE_MUSIC_CULTURE);
				}
			}
		}
    }
}
