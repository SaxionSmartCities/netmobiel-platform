package eu.netmobiel.profile.api.mapping;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Review;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { GeometryMapper.class, JavaTimeMapper.class })
public abstract class ReviewMapper {

	public abstract List<eu.netmobiel.profile.api.model.Review> map(List<Review> source);
//	public abstract eu.netmobiel.profile.api.model.Page map(PagedResult<Review> source);

	// Domain --> API
	@Mapping(target = "id", source = "managedIdentity")
	@Mapping(target = "firstName", source = "givenName")
	@Mapping(target = "lastName", source = "familyName")
	public abstract eu.netmobiel.profile.api.model.UserRef map(Profile source);

	@BeanMapping(ignoreByDefault = true)
	@InheritInverseConfiguration
	public abstract Profile map(eu.netmobiel.profile.api.model.UserRef source);

	
	// Domain --> API
	public abstract eu.netmobiel.profile.api.model.Review map(Review source);

	// API --> Domain
	@InheritInverseConfiguration
	public abstract Review map(eu.netmobiel.profile.api.model.Review source);

}
