package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.RewardDetails;
import eu.netmobiel.banker.api.mapping.annotation.RewardMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.RewardShallow;
import eu.netmobiel.banker.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.UserOnlyDetails;
import eu.netmobiel.banker.model.Reward;

/**
 * This mapper defines the mapping from the domain Account to the API Account as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class, UserMapper.class })
@RewardMapperQualifier
public interface RewardMapper {

	// Domain --> API
	@Mapping(target = "recipient", source = "recipient", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@RewardDetails
	eu.netmobiel.banker.api.model.Reward mapWithDetails(Reward rwd);

	// Incentive is included
	@Mapping(target = "recipient", ignore = true)
	@RewardShallow
	eu.netmobiel.banker.api.model.Reward mapShallow(Reward rwd);
}
