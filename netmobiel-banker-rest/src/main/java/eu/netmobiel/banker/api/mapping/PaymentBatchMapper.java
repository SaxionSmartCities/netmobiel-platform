package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.CharityMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.PaymentBatchShallow;
import eu.netmobiel.banker.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.UserOnlyDetails;
import eu.netmobiel.banker.model.PaymentBatch;
import eu.netmobiel.banker.model.WithdrawalRequest;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
uses = { JavaTimeMapper.class, UserMapper.class })
@CharityMapperQualifier
public interface PaymentBatchMapper {

	// Domain --> API
	@Mapping(target = "createdBy", source = "createdBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "settledBy", source = "settledBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@PaymentBatchShallow
	eu.netmobiel.banker.api.model.PaymentBatch mapShallow(PaymentBatch source);

	@Mapping(target = "createdBy", source = "createdBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "settledBy", source = "settledBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "accountName", source = "account.name")
	eu.netmobiel.banker.api.model.WithdrawalRequest map(WithdrawalRequest source);

}
