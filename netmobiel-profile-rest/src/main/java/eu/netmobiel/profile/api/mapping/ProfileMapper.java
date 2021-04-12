package eu.netmobiel.profile.api.mapping;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.api.mapping.annotation.ProfileComplete;
import eu.netmobiel.profile.api.mapping.annotation.ProfileMapperQualifier;
import eu.netmobiel.profile.api.mapping.annotation.Secondary;
import eu.netmobiel.profile.api.mapping.annotation.Shallow;
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
@ProfileMapperQualifier
public abstract class ProfileMapper {

	@Mapping(target = "data", source = "data", qualifiedBy = { Shallow.class } )
	public abstract eu.netmobiel.profile.api.model.Page map(PagedResult<Profile> source);

	// Domain --> API
	@Mapping(target = "firstName", source = "givenName")
	@Mapping(target = "image", source = "imagePath")
	@Mapping(target = "lastName", source = "familyName")
	@Mapping(target = "address", source = "homeAddress")
	@Mapping(target = "ridePlanOptions", source = "ridesharePreferences")
	@Mapping(target = "favoriteLocations", ignore = true)
	@Mapping(target = "interests", ignore = true)
	// The id is defined as the keycloak identity.
	@Mapping(target = "id", source = "managedIdentity")
	public abstract eu.netmobiel.profile.api.model.Profile commonMap(Profile source);

	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "interests", ignore = true)
	@Mapping(target = "favoriteLocations", ignore = true)
	@Mapping(target = "ridePlanOptions", ignore = true)
	@Mapping(target = "searchPreferences", ignore = true)
	@Shallow
	public abstract eu.netmobiel.profile.api.model.Profile mapShallow(Profile source);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "firstName", source = "givenName")
	@Mapping(target = "lastName", source = "familyName")
	@Mapping(target = "email", source = "email")
	// The id is defined as the keycloak identity.
	@Mapping(target = "id", source = "managedIdentity")
	// Timothy wil ook de profiel foto en het adres (minimaal woonplaats)
	@Mapping(target = "image", source = "imagePath")
	@Mapping(target = "address", source = "homeAddress")
	@Secondary
	public abstract eu.netmobiel.profile.api.model.Profile mapSecondary(Profile source);

	@InheritConfiguration(name = "commonMap")
	@ProfileComplete
	public abstract eu.netmobiel.profile.api.model.Profile mapComplete(Profile source);

	@InheritInverseConfiguration(name = "commonMap")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "places", ignore = true)
	public abstract Profile map(eu.netmobiel.profile.api.model.Profile source);

	@Mapping(target = "country", source ="countryCode")
	@Mapping(target = "label", source ="location.label")
	public abstract eu.netmobiel.profile.api.model.Address  map(Address source);

	@Mapping(target = "countryCode", source ="country")
	public abstract Address map(eu.netmobiel.profile.api.model.Address source);

	// Use this construction for the label, otherwise the conversion cannot do both coordinates and label.
	@AfterMapping
    protected void addLabel(eu.netmobiel.profile.api.model.Address source, @MappingTarget Address target) {
		if (source.getLabel() != null) {
			if (target.getLocation() == null) {
				target.setLocation(new GeoLocation());
			}
			target.getLocation().setLabel(source.getLabel());
		}
    }

	@Mapping(target = "numPassengers", source = "maxPassengers")
	@Mapping(target = "selectedCarId", source = "defaultCarRef")
	public abstract eu.netmobiel.profile.api.model.RidePlanOptions map(RidesharePreferences source);

	@InheritInverseConfiguration
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
	@Mapping(target = "id", ignore = true)
	public abstract SearchPreferences map(eu.netmobiel.profile.api.model.SearchPreferences source);
}
