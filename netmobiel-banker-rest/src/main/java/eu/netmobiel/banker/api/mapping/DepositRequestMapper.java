package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.model.DepositRequest;

/**
 * This mapper defines the mapping from the domain DepositRequest to the API DepositStatus as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface DepositRequestMapper {

	@Mapping(target="status", source="status")
	eu.netmobiel.banker.api.model.PaymentStatus map(DepositRequest source);
    
}
