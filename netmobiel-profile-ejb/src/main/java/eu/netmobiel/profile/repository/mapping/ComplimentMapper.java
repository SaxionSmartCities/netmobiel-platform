package eu.netmobiel.profile.repository.mapping;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;

import eu.netmobiel.profile.model.Compliment;
import eu.netmobiel.profile.model.ComplimentType;
import eu.netmobiel.profile.model.Profile;

/**
 * This mapper defines the mapping of the Compliments from the API to the domain for migration of the profiles.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class })
public abstract class ComplimentMapper {

	// API --> Domain
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "managedIdentity", source = "id")
	@Mapping(target = "givenName", source = "firstName")
	@Mapping(target = "familyName", source = "lastName")
	public abstract Profile map(eu.netmobiel.profile.api.model.UserRef source);
	
	// API --> Domain
	@Mapping(target = "compliment", source = "complimentType")
	public abstract Compliment map(eu.netmobiel.profile.api.model.Compliment source);


	// API --> Domain
	@ValueMappings({
        @ValueMapping(target = "SAME_INTERESTS", source = "ZELFDE_INTERESSES"),
        @ValueMapping(target = "ON_TIME", source = "OP_TIJD"),
        @ValueMapping(target = "TALKS_EASILY", source = "SOEPELE_COMMUNICATIE"),
        @ValueMapping(target = "SOCIABLE", source = "GEZELLIG"),
        @ValueMapping(target = "NEATLY", source = "NETJES"),
        @ValueMapping(target = "NICE_CAR", source = "GOEDE_AUTO"),
    })
	public abstract ComplimentType map(eu.netmobiel.profile.api.model.Compliment.ComplimentTypeEnum source);

}
