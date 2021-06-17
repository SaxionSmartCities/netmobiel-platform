package eu.netmobiel.profile.api.mapping;

import javax.inject.Inject;

import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.messagebird.MessageBird;
import eu.netmobiel.profile.api.mapping.annotation.ProfileComplete;
import eu.netmobiel.profile.api.mapping.annotation.ProfileMapperQualifier;
import eu.netmobiel.profile.api.mapping.annotation.Secondary;
import eu.netmobiel.profile.api.mapping.annotation.Shallow;
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
    @Inject
    private MessageBird	messageBirdClient;

	@Mapping(target = "data", source = "data", qualifiedBy = { Shallow.class } )
	public abstract eu.netmobiel.profile.api.model.Page mapShallow(PagedResult<Profile> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { Secondary.class } )
	public abstract eu.netmobiel.profile.api.model.Page mapSecondary(PagedResult<Profile> source);

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
	public abstract eu.netmobiel.profile.api.model.Profile commonMap(Profile source);

	@InheritConfiguration(name = "commonMap")
	@Mapping(target = "ridePlanOptions", ignore = true)
	@Mapping(target = "searchPreferences", ignore = true)
	@Shallow
	public abstract eu.netmobiel.profile.api.model.Profile mapShallow(Profile source);

	@InheritConfiguration(name = "commonMap")
	// Timothy wil ook de profiel foto en het adres (minimaal woonplaats)
	@Mapping(target = "ridePlanOptions", ignore = true)
	@Mapping(target = "searchPreferences", ignore = true)
	@Mapping(target = "consent", ignore = true)
	@Mapping(target = "fcmToken", ignore = true)
	@Mapping(target = "phoneNumber", ignore = true)
	@Mapping(target = "dateOfBirth", ignore = true)
	@Mapping(target = "userRole", ignore = true)
	@Secondary
	@Mapping(target = "notificationOptions", ignore = true)
	public abstract eu.netmobiel.profile.api.model.Profile mapSecondary(Profile source);

	@InheritConfiguration(name = "commonMap")
	@ProfileComplete
	public abstract eu.netmobiel.profile.api.model.Profile mapComplete(Profile source);

	@InheritInverseConfiguration(name = "commonMap")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "creationTime", ignore = true)
	@Mapping(target = "places", ignore = true)
	public abstract Profile map(eu.netmobiel.profile.api.model.Profile source);

	// Use this construction for the label, otherwise the conversion cannot do both coordinates and label.
	@AfterMapping
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
		if (target.getPhoneNumber() != null) {
			target.setPhoneNumber(messageBirdClient.formatPhoneNumberNational(target.getPhoneNumber(), target.getHomeAddress().getCountryCode()));
		}
	}

	@Mapping(target = "numPassengers", source = "maxPassengers")
	@Mapping(target = "selectedCarId", source = "defaultCarRef")
	public abstract eu.netmobiel.profile.api.model.RidePlanOptions map(RidesharePreferences source);

	@InheritInverseConfiguration
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "profile", ignore = true)
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
	@Mapping(target = "profile", ignore = true)
	public abstract SearchPreferences map(eu.netmobiel.profile.api.model.SearchPreferences source);
}
