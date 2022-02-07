package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountAll;
import eu.netmobiel.banker.api.mapping.annotation.AccountMapperQualifier;
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
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { AccountMapper.class })
@UserMapperQualifier
public interface UserMapper {

	@UserCreditDetails
	@Mapping(target = "personalAccount", source = "personalAccount", qualifiedBy = { AccountMapperQualifier.class, AccountAll.class })
	@Mapping(target = "premiumAccount", source = "premiumAccount", qualifiedBy = { AccountMapperQualifier.class, AccountAll.class })
	eu.netmobiel.banker.api.model.User map(BankerUser source);

	@Mapping(target = "personalAccount", ignore = true)
	@Mapping(target = "premiumAccount", ignore = true)
	@UserOnlyDetails
	eu.netmobiel.banker.api.model.User mapUserOnly(BankerUser source);
}
