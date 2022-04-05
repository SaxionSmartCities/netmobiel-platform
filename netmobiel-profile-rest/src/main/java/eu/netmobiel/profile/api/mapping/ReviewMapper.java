package eu.netmobiel.profile.api.mapping;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.api.mapping.annotation.ProfileMapperQualifier;
import eu.netmobiel.profile.model.Review;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { GeometryMapper.class, JavaTimeMapper.class, ProfileMapper.class })
public abstract class ReviewMapper {

	public abstract eu.netmobiel.profile.api.model.Page map(PagedResult<Review> source);
	
	// Domain --> API
	@Mapping(target = "receiver", source = "receiver", qualifiedBy = { ProfileMapperQualifier.class } )
	@Mapping(target = "sender", source = "sender", qualifiedBy = { ProfileMapperQualifier.class } )
	public abstract eu.netmobiel.profile.api.model.Review map(Review source);

	// API --> Domain
	@InheritInverseConfiguration
	public abstract Review map(eu.netmobiel.profile.api.model.Review source);

}
