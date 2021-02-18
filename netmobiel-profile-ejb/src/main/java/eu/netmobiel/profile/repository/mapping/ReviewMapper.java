package eu.netmobiel.profile.repository.mapping;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Review;

/**
 * This mapper defines the mapping of the Reviews from the API to the domain for migration of the profiles.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class })
public abstract class ReviewMapper {

	// API --> Domain
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "managedIdentity", source = "id")
	@Mapping(target = "givenName", source = "firstName")
	@Mapping(target = "familyName", source = "lastName")
	public abstract Profile map(eu.netmobiel.profile.api.model.UserRef source);

	// API --> Domain
	public abstract Review map(eu.netmobiel.profile.api.model.Review source);

}
