package eu.netmobiel.profile.api.mapping;

import org.mapstruct.BeanMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.model.ComplimentType;
import eu.netmobiel.profile.model.Compliments;
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

	@Mapping(target = "removeDataItem", ignore = true)
	public abstract eu.netmobiel.profile.api.model.Page map(PagedResult<Compliments> source);

	// Domain --> API
	@Mapping(target = "id", source = "managedIdentity")
	@Mapping(target = "firstName", source = "givenName")
	@Mapping(target = "lastName", source = "familyName")
	public abstract eu.netmobiel.profile.api.model.UserRef map(Profile source);

	@BeanMapping(ignoreByDefault = true)
	@InheritInverseConfiguration
	public abstract Profile map(eu.netmobiel.profile.api.model.UserRef source);

	
	// Domain --> API
	
//	@Mapping(target = "complimentType", source = "compliment")
	@Mapping(target = "removeComplimentsItem", ignore = true)
	public abstract eu.netmobiel.profile.api.model.Compliments map(Compliments source);

	// API --> Domain
	@InheritInverseConfiguration
	public abstract Compliments map(eu.netmobiel.profile.api.model.Compliments source);

//	@ValueMappings({
//        @ValueMapping(target = "ZELFDE_INTERESSES", source = "SAME_INTERESTS"),
//        @ValueMapping(target = "OP_TIJD", source = "ON_TIME"),
//        @ValueMapping(target = "SOEPELE_COMMUNICATIE", source = "TALKS_EASILY"),
//        @ValueMapping(target = "GEZELLIG", source = "SOCIABLE"),
//        @ValueMapping(target = "NETJES", source = "NEATLY"),
//        @ValueMapping(target = "GOEDE_AUTO", source = "NICE_CAR"),
//    })
	public abstract eu.netmobiel.profile.api.model.ComplimentType map(ComplimentType source);

//	@InheritInverseConfiguration
	public abstract ComplimentType map(eu.netmobiel.profile.api.model.ComplimentType source);

}
