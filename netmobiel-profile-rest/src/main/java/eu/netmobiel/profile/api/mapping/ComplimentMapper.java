package eu.netmobiel.profile.api.mapping;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;

import eu.netmobiel.profile.model.Compliment;
import eu.netmobiel.profile.model.ComplimentType;
import eu.netmobiel.profile.model.Profile;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { GeometryMapper.class, JavaTimeMapper.class })
public abstract class ComplimentMapper {

//	public abstract List<Object> map(List<Compliment> source);
	public abstract List<eu.netmobiel.profile.api.model.Compliment> map(List<Compliment> source);
//	public abstract eu.netmobiel.profile.api.model.Page map(PagedResult<Compliment> source);

	// Domain --> API
	@Mapping(target = "id", source = "managedIdentity")
	@Mapping(target = "firstName", source = "givenName")
	@Mapping(target = "lastName", source = "familyName")
	public abstract eu.netmobiel.profile.api.model.UserRef map(Profile source);

	@BeanMapping(ignoreByDefault = true)
	@InheritInverseConfiguration
	public abstract Profile map(eu.netmobiel.profile.api.model.UserRef source);

	
	// Domain --> API
	
	@Mapping(target = "complimentType", source = "compliment")
	public abstract eu.netmobiel.profile.api.model.Compliment map(Compliment source);

	// API --> Domain
	@InheritInverseConfiguration
	public abstract Compliment map(eu.netmobiel.profile.api.model.Compliment source);

	@ValueMappings({
        @ValueMapping(target = "ZELFDE_INTERESSES", source = "SAME_INTERESTS"),
        @ValueMapping(target = "OP_TIJD", source = "ON_TIME"),
        @ValueMapping(target = "SOEPELE_COMMUNICATIE", source = "TALKS_EASILY"),
        @ValueMapping(target = "GEZELLIG", source = "SOCIABLE"),
        @ValueMapping(target = "NETJES", source = "NEATLY"),
        @ValueMapping(target = "GOEDE_AUTO", source = "NICE_CAR"),
    })
	public abstract eu.netmobiel.profile.api.model.Compliment.ComplimentTypeEnum map(ComplimentType source);

	@InheritInverseConfiguration
	public abstract ComplimentType map(eu.netmobiel.profile.api.model.Compliment.ComplimentTypeEnum source);

}
