package eu.netmobiel.profile.repository.mapping;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.profile.model.Address;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.RidesharePreferences;
import eu.netmobiel.profile.model.SearchPreferences;
import eu.netmobiel.profile.model.TraverseMode;

/**
 * This mapper defines the mapping of the Profiles from the API to the domain for migration of the profiles.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { GeometryMapper.class })
public abstract class ProfileMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "givenName", source = "firstName")
	@Mapping(target = "imagePath", source = "image")
	@Mapping(target = "familyName", source = "lastName")
	@Mapping(target = "homeAddress", source = "address")
	@Mapping(target = "ridesharePreferences", source = "ridePlanOptions")
	// The id is defined as the keycloak identity.
	@Mapping(target = "managedIdentity", source = "id")
	@Mapping(target = "places", ignore = true)
	public abstract Profile map(eu.netmobiel.profile.api.model.Profile source);

	@AfterMapping
	public Profile fixImagePath(eu.netmobiel.profile.api.model.Profile source, @MappingTarget Profile profile) {
		// Remove the prefix from the image path, it should not be there.
		if (profile.getImagePath() != null && profile.getImagePath().startsWith("/images/")) {
			profile.setImagePath(profile.getImagePath().substring("/images/".length()));
		}
		return profile;
	}
	
	@Mapping(target = "maxPassengers", source = "numPassengers")
	@Mapping(target = "defaultCarRef", source = "selectedCarId")
	@Mapping(target = "profile", ignore = true)
	@Mapping(target = "id", ignore = true)
	public abstract RidesharePreferences map(eu.netmobiel.profile.api.model.RidePlanOptions source);

	@Mapping(target = "allowFirstLegRideshare", source = "allowFirstLegTransfer")
	@Mapping(target = "allowLastLegRideshare", source = "allowLastLegTransfer")
	@Mapping(target = "allowTransfers", source = "allowTransfer")
	@Mapping(target = "numberOfPassengers", source = "numPassengers")
	@Mapping(target = "maxTransferTime", source = "maximumTransferTime")
	@Mapping(target = "allowedTraverseModes", source = "allowedTravelModes")
	@Mapping(target = "profile", ignore = true)
	@Mapping(target = "id", ignore = true)
	public abstract SearchPreferences map(eu.netmobiel.profile.api.model.SearchPreferences source);
	
	public TraverseMode map(String source) {
		if (source == null) {
			return null;
		}
		if ("BIKE".equals(source)) {
			return TraverseMode.BICYCLE;
		}
		if ("NETMOBIEL".equals(source)) {
			return TraverseMode.RIDESHARE;
		}
		if ("TRAIN".equals(source)) {
			return TraverseMode.RAIL;
		}
		return TraverseMode.valueOf(source);
	}
	
	@Mapping(target = "countryCode", source = "country")
	@Mapping(target = "location.label", source ="label")
	public abstract Address map(eu.netmobiel.profile.api.model.Address source);
}
