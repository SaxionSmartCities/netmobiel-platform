package eu.netmobiel.profile.api.mapping;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.api.mapping.annotation.ProfileComplete;
import eu.netmobiel.profile.api.mapping.annotation.ProfileMapperQualifier;
import eu.netmobiel.profile.api.mapping.annotation.PublicProfile;
import eu.netmobiel.profile.api.mapping.annotation.Shallow;
import eu.netmobiel.profile.api.model.Profile.ActingRoleEnum;
import eu.netmobiel.profile.model.UserEvent;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.RidesharePreferences;
import eu.netmobiel.profile.model.SearchPreferences;
import eu.netmobiel.profile.model.UserRole;
import eu.netmobiel.profile.model.UserSession;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { GeometryMapper.class, JavaTimeMapper.class })
@ProfileMapperQualifier
public abstract class ProfileMapper {
	@Mapping(target = "data", source = "data", qualifiedBy = { Shallow.class } )
	@Mapping(target = "removeDataItem", ignore = true)
	public abstract eu.netmobiel.profile.api.model.Page mapShallow(PagedResult<Profile> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { PublicProfile.class } )
	@Mapping(target = "removeDataItem", ignore = true)
	public abstract eu.netmobiel.profile.api.model.Page mapSecondary(PagedResult<Profile> source);

	// Translation of a faulty acting role enum to a safe value,
    @ValueMapping(target = "PASSENGER", source = "BOTH")
    public abstract ActingRoleEnum map(UserRole source);

	// Domain --> API
	@Mapping(target = "id", source = "managedIdentity")
	@Mapping(target = "firstName", source = "givenName")
	@Mapping(target = "lastName", source = "familyName")
	public abstract eu.netmobiel.profile.api.model.UserRef mapToUserRef(Profile source);

	@BeanMapping(ignoreByDefault = true)
	@InheritInverseConfiguration
	public abstract Profile mapUserRefToProfile(eu.netmobiel.profile.api.model.UserRef source);

	// Domain --> API
	@Mapping(target = "firstName", source = "givenName")
	@Mapping(target = "image", source = "imagePath")
	@Mapping(target = "lastName", source = "familyName")
	@Mapping(target = "address", source = "homeAddress")
	@Mapping(target = "address.location", source = "homeLocation")
//	@Mapping(target = "address.label", source = "homeLocation.label")
//	Do not try to map the home location label here, does not work correctly
	@Mapping(target = "ridePlanOptions", source = "ridesharePreferences")
	// The id is defined as the keycloak identity.
	@Mapping(target = "id", source = "managedIdentity")
	@Mapping(target = "interests", ignore = true)
	@Mapping(target = "removeInterestsItem", ignore = true)
	public abstract eu.netmobiel.profile.api.model.Profile commonMap(Profile source);

	@PublicProfile
	// Just to be sure that only the specified attributes are exported
	@BeanMapping(ignoreByDefault = true)
	// The id is defined as the keycloak identity.
	@Mapping(target = "id", source = "managedIdentity")
	@Mapping(target = "firstName", source = "givenName")
	@Mapping(target = "image", source = "imagePath")
	@Mapping(target = "lastName", source = "familyName")
	@Mapping(target = "age", source = "age")
	@Mapping(target = "address.street", ignore = true)
	@Mapping(target = "address.houseNumber", ignore = true)
	@Mapping(target = "address.postalCode", ignore = true)
	@Mapping(target = "address.locality", source = "homeAddress.locality")
	@Mapping(target = "address.countryCode", source = "homeAddress.countryCode")
	@Mapping(target = "address.label", ignore = true)
	@Mapping(target = "address.location", ignore = true)
	@Mapping(target = "address.id", ignore = true)
	@Mapping(target = "address.category", ignore = true)
	@Mapping(target = "address.ref", ignore = true)
	@Mapping(target = "address.name", ignore = true)
	public abstract eu.netmobiel.profile.api.model.Profile mapPublicProfile(Profile source);

	@Shallow
	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "ridePlanOptions", ignore = true)
	@Mapping(target = "searchPreferences", ignore = true)
	@BeanMapping(qualifiedBy = Shallow.class)
	public abstract eu.netmobiel.profile.api.model.Profile mapShallow(Profile source);

	@ProfileComplete
	@InheritConfiguration(name = "commonMap")
	@BeanMapping(qualifiedBy = ProfileComplete.class)
	public abstract eu.netmobiel.profile.api.model.Profile mapComplete(Profile source);

	// API --> Domain 
	@InheritInverseConfiguration(name = "commonMap")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "creationTime", ignore = true)
	@Mapping(target = "places", ignore = true)
	public abstract Profile map(eu.netmobiel.profile.api.model.Profile source);

	// Use this construction for the label, otherwise the conversion cannot do both coordinates and label.
	@AfterMapping
	@ProfileComplete
	@Shallow
    protected void postProcessApiProfile(Profile source, @MappingTarget eu.netmobiel.profile.api.model.Profile target) {
		if (source.getHomeLocation() != null && source.getHomeLocation().getLabel() != null) {
			if (target.getAddress() == null) {
				target.setAddress(new eu.netmobiel.profile.api.model.Place());
			}
			target.getAddress().setLabel(source.getHomeLocation().getLabel());
		}
    }

	@AfterMapping
	protected void postProcessDomainProfile(eu.netmobiel.profile.api.model.Profile source, @MappingTarget Profile target) {
		if (source.getAddress() != null &&  source.getAddress().getLabel() != null) {
			if (target.getHomeLocation() == null) {
				target.setHomeLocation(new GeoLocation());
			}
			target.getHomeLocation().setLabel(source.getAddress().getLabel());
		}
		target.addAddressIfNotExists();
	}

	@Mapping(target = "selectedCarRef", source = "defaultCarRef")
	@Mapping(target = "removeLuggageOptionsItem", ignore = true)
	public abstract eu.netmobiel.profile.api.model.RidePlanOptions map(RidesharePreferences source);

	@InheritInverseConfiguration
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "profile", ignore = true)
	public abstract RidesharePreferences map(eu.netmobiel.profile.api.model.RidePlanOptions source);

	@Mapping(target = "numPassengers", source = "numberOfPassengers")
	@Mapping(target = "allowedTravelModes", source = "allowedTraverseModes")
	@Mapping(target = "removeAllowedTravelModesItem", ignore = true)
	@Mapping(target = "removeLuggageOptionsItem", ignore = true)
	public abstract eu.netmobiel.profile.api.model.SearchPreferences map(SearchPreferences source);

	@InheritInverseConfiguration
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "profile", ignore = true)
	public abstract SearchPreferences map(eu.netmobiel.profile.api.model.SearchPreferences source);

	// API --> Domain 
	@Mapping(target = "realUser", ignore = true)
	public abstract UserSession map(eu.netmobiel.profile.api.model.UserSession source);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "userSession", ignore = true)
	@Mapping(target = "onBehalfOf", ignore = true)
	public abstract UserEvent map(eu.netmobiel.profile.api.model.UserEvent source);
}
