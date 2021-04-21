package eu.netmobiel.profile.api.mapping;

import org.mapstruct.AfterMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.api.mapping.annotation.ProfileMapperQualifier;
import eu.netmobiel.profile.model.Place;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { GeometryMapper.class })
@ProfileMapperQualifier
public abstract class PlaceMapper {
	@Mapping(target = "data", source = "data")
	public abstract eu.netmobiel.profile.api.model.Page mapPlacesPage(PagedResult<Place> source);

	@Mapping(target = "countryCode", source ="address.countryCode")
	@Mapping(target = "stateCode", source ="address.stateCode")
	@Mapping(target = "locality", source ="address.locality")
	@Mapping(target = "street", source ="address.street")
	@Mapping(target = "postalCode", source ="address.postalCode")
	@Mapping(target = "houseNumber", source ="address.houseNumber")
	@Mapping(target = "location", source ="location")
	@Mapping(target = "label", ignore = true)
	@Mapping(target = "ref", source ="reference")
	public abstract eu.netmobiel.profile.api.model.Place mapPlace(Place source);

	@InheritInverseConfiguration
	@Mapping(target = "profile", ignore = true)
	// The id is set in the service call, to assure we use the right id, not somebody else's.
	@Mapping(target = "id", ignore = true)
	public abstract Place mapApiPlace(eu.netmobiel.profile.api.model.Place source);

	// Use this construction for the label, otherwise the conversion cannot do both coordinates and label.
	@AfterMapping
    protected void addLabel(eu.netmobiel.profile.api.model.Place source, @MappingTarget Place target) {
		if (source.getLabel() != null) {
			if (target.getLocation() == null) {
				target.setLocation(new GeoLocation());
			}
			target.getLocation().setLabel(source.getLabel());
		}
    }

	// Use this construction for the label, otherwise the conversion cannot do both coordinates and label.
	@AfterMapping
    protected void addLabel(Place source, @MappingTarget eu.netmobiel.profile.api.model.Place target) {
		if (source.getLocation() != null && source.getLocation().getLabel() != null) {
			target.setLabel(source.getLocation().getLabel());
		}
    }

}
