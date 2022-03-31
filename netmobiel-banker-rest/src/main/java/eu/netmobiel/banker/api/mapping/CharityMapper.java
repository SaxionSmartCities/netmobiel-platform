package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountAll;
import eu.netmobiel.banker.api.mapping.annotation.AccountMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.CharityDetails;
import eu.netmobiel.banker.api.mapping.annotation.CharityMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.CharityWithRoleAndAccountDetails;
import eu.netmobiel.banker.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.UserOnlyDetails;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharityUserRole;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
uses = { JavaTimeMapper.class, GeometryMapper.class, DonationMapper.class, UserMapper.class, AccountMapper.class  })
@CharityMapperQualifier
public interface CharityMapper {

	// Domain --> API
	@CharityWithRoleAndAccountDetails
	@Mapping(target = "account", source = "account", qualifiedBy = { AccountMapperQualifier.class, AccountAll.class })
	eu.netmobiel.banker.api.model.Charity mapWithRolesAndAccount(Charity source);

	// Domain --> API
	@Mapping(target = "roles", ignore = true)
	@Mapping(target = "account", ignore = true)
	@CharityDetails
	eu.netmobiel.banker.api.model.Charity mapWithoutRoles(Charity source);

	// Domain --> API
	@Mapping(target = "user", source = "user", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	eu.netmobiel.banker.api.model.CharityUserRole map(CharityUserRole source);
	
	// API --> Domain
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "roles", ignore = true)
	@Mapping(target = "donatedAmount", ignore = true)
	@Mapping(target = "account", ignore = true)
	@Mapping(target = "deleted", ignore = true)
	Charity map(eu.netmobiel.banker.api.model.Charity source);

}
