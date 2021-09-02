package eu.netmobiel.profile.api.mapping;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.profile.model.SurveyInteraction;
import eu.netmobiel.profile.model.Survey;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class })
public abstract class SurveyMapper {

	public abstract List<eu.netmobiel.profile.api.model.SurveyInteraction> map(List<SurveyInteraction> source);

	// Domain --> API
	public abstract eu.netmobiel.profile.api.model.Survey map(Survey source);

	// Domain --> API
	@Mapping(target = "survey", source = "id.survey")
	public abstract eu.netmobiel.profile.api.model.SurveyInteraction map(SurveyInteraction source);

}
