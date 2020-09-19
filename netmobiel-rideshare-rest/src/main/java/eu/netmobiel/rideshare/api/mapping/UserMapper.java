package eu.netmobiel.rideshare.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.rideshare.api.mapping.annotation.UserSomeDetails;
import eu.netmobiel.rideshare.model.RideshareUser;

/**
 * This mapper defines the mapping from the domain User to the API User as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
@UserMapperQualifier
public interface UserMapper {

	// User <--> User
	eu.netmobiel.rideshare.api.model.User map(RideshareUser source);

	// Domain User --> Api User: Only family name and given name
	@Mapping(target = "managedIdentity", ignore = true)
	@Mapping(target = "gender", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "email", ignore = true)
	@Mapping(target = "yearOfBirth", ignore = true)
	@UserSomeDetails
	eu.netmobiel.rideshare.api.model.User mapName(RideshareUser source);

	@Mapping(target = "carsInUse", ignore = true)
	RideshareUser map(eu.netmobiel.rideshare.api.model.User source);

}
