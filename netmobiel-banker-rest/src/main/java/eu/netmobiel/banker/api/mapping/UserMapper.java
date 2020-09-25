package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.UserCreditDetails;
import eu.netmobiel.banker.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.UserOnlyDetails;
import eu.netmobiel.banker.model.BankerUser;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
@UserMapperQualifier
public abstract class UserMapper {

	@Mapping(target = "credits", source = "personalAccount.actualBalance.endAmount")
	@UserCreditDetails
	public abstract eu.netmobiel.banker.api.model.User map(BankerUser source);

	@Mapping(target = "credits", ignore = true)
	@UserOnlyDetails
	public abstract eu.netmobiel.banker.api.model.User mapUserOnly(BankerUser source);
}
