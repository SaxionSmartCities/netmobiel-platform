package eu.netmobiel.profile.api.mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.model.Address;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.RidesharePreferences;
import eu.netmobiel.profile.model.SearchPreferences;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { GeometryMapper.class })
public abstract class ProfileMapper {

	public abstract eu.netmobiel.profile.api.model.Page map(PagedResult<Profile> source);

	// Domain --> API
	@Mapping(target = "firstName", source = "givenName")
	@Mapping(target = "image", source = "imagePath")
	@Mapping(target = "interests", ignore = true)
	@Mapping(target = "lastName", source = "familyName")
	@Mapping(target = "address", source = "homeAddress")
	@Mapping(target = "ridePlanOptions", source = "ridesharePreferences")
	@Mapping(target = "favoriteLocations", source = "addresses")
	// The id is defined as the keycloak identity.
	@Mapping(target = "id", source = "managedIdentity")
	public abstract eu.netmobiel.profile.api.model.Profile map(Profile source);

	@InheritInverseConfiguration
	public abstract Profile map(eu.netmobiel.profile.api.model.Profile source);

	public Set<Address> map(List<Object> source) {
		return source.stream()
			.map(obj -> map((eu.netmobiel.profile.api.model.Address) obj))
			.collect(Collectors.toSet());
	}

	@Mapping(target = "country", source ="countryCode")
	@Mapping(target = "label", source ="location.label")
	public abstract eu.netmobiel.profile.api.model.Address  map(Address source);

	@InheritInverseConfiguration
	@Mapping(target = "profile", ignore = true)
	public abstract Address map(eu.netmobiel.profile.api.model.Address source);

	@Mapping(target = "numPassengers", source = "maxPassengers")
	@Mapping(target = "selectedCarId", source = "defaultCarRef")
	public abstract eu.netmobiel.profile.api.model.RidePlanOptions map(RidesharePreferences source);

	@InheritInverseConfiguration
//	@Mapping(target = "profile", ignore = true)
	@Mapping(target = "id", ignore = true)
	public abstract RidesharePreferences map(eu.netmobiel.profile.api.model.RidePlanOptions source);

	@Mapping(target = "allowFirstLegTransfer", source = "allowFirstLegRideshare")
	@Mapping(target = "allowLastLegTransfer", source = "allowLastLegRideshare")
	@Mapping(target = "allowTransfer", source = "allowTransfers")
	@Mapping(target = "numPassengers", source = "numberOfPassengers")
	@Mapping(target = "maximumTransferTime", source = "maxTransferTime")
	@Mapping(target = "allowedTravelModes", source = "allowedTraverseModes")
	public abstract eu.netmobiel.profile.api.model.SearchPreferences map(SearchPreferences source);

	@InheritInverseConfiguration
//	@Mapping(target = "profile", ignore = true)
	@Mapping(target = "id", ignore = true)
	public abstract SearchPreferences map(eu.netmobiel.profile.api.model.SearchPreferences source);
}
