package eu.netmobiel.profile.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.api.mapping.annotation.ProfileMapperQualifier;
import eu.netmobiel.profile.api.mapping.annotation.PublicProfile;
import eu.netmobiel.profile.api.mapping.annotation.Shallow;
import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.SurveyInteraction;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class, ProfileMapper.class })
public abstract class SurveyMapper {

	// Domain --> API
	public abstract eu.netmobiel.profile.api.model.Survey map(Survey source);

	// Domain[] --> API[]
//	@IterableMapping(qualifiedBy = Shallow.class)
//	public abstract List<eu.netmobiel.profile.api.model.SurveyInteraction> mapWithoutUser(List<SurveyInteraction> source);
//	@IterableMapping(qualifiedBy = PublicProfile.class)
//	public abstract List<eu.netmobiel.profile.api.model.SurveyInteraction> mapWithUser(List<SurveyInteraction> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { Shallow.class } )
	public abstract eu.netmobiel.profile.api.model.Page mapWithoutUser(PagedResult<SurveyInteraction> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { PublicProfile.class } )
	public abstract eu.netmobiel.profile.api.model.Page mapWithUser(PagedResult<SurveyInteraction> source);

	
	// Domain --> API
	@Mapping(target = "owner", ignore = true)
	@Shallow
	public abstract eu.netmobiel.profile.api.model.SurveyInteraction mapWithoutUser(SurveyInteraction source);

	// Domain --> API
	@Mapping(target = "owner", source = "profile", qualifiedBy = { ProfileMapperQualifier.class })
	@PublicProfile
	public abstract eu.netmobiel.profile.api.model.SurveyInteraction mapWithUser(SurveyInteraction source);
}
