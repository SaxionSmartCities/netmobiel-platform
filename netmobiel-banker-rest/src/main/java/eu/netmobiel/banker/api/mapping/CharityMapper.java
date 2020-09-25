package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.CharityMapperQualifier;
import eu.netmobiel.banker.api.model.CharityUserRole;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.Donation;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
uses = { JavaTimeMapper.class, GeometryMapper.class })
@CharityMapperQualifier
public interface CharityMapper {

	@Mapping(target = "balanceAmount", source = "account.actualBalance.endAmount")
	@Mapping(target = "name", source = "account.name")
	eu.netmobiel.banker.api.model.Charity map(Charity source);

	@Mapping(target = "user", source = "user")
	eu.netmobiel.banker.api.model.CharityUserRole map(CharityUserRole source);
	
	@Mapping(target = "credits", ignore = true)
	eu.netmobiel.banker.api.model.User mapUserOnly(BankerUser source);

	// API --> Domain
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "reference", ignore = true)
	@Mapping(target = "roles", ignore = true)
	@Mapping(target = "donatedAmount", ignore = true)
	@Mapping(target = "account.name", source = "name")
//	@Mapping(target = "account.createdTime", ignore = true)
//	@Mapping(target = "account.closedTime", ignore = true)
//	@Mapping(target = "account.actualBalance", ignore = true)
	Charity map(eu.netmobiel.banker.api.model.Charity source);

	// Domain --> API
	eu.netmobiel.banker.api.model.Donation map(Donation source);
	
	// API --> Domain
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "charity", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "donationTime", ignore = true)
	Donation map(eu.netmobiel.banker.api.model.Donation source); 
}
