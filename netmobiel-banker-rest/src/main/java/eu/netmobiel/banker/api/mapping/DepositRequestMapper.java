package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.model.DepositRequest;

/**
 * This mapper defines the mapping from the domain DepositRequest to the API DepositStatus as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class DepositRequestMapper {

	public abstract eu.netmobiel.banker.api.model.DepositStatus map(DepositRequest source);
    
}
