package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.IncentiveMapperQualifier;
import eu.netmobiel.banker.model.Incentive;

/**
 * This mapper defines the mapping from the domain Account to the API Account as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class })
@IncentiveMapperQualifier
public interface IncentiveMapper {

	// Domain --> API
	eu.netmobiel.banker.api.model.Incentive map(Incentive inc);

}
