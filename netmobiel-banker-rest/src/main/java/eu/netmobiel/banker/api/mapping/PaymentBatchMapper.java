package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.PaymentBatchMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.PaymentBatchShallow;
import eu.netmobiel.banker.api.mapping.annotation.PaymentBatchWithdrawals;
import eu.netmobiel.banker.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.UserOnlyDetails;
import eu.netmobiel.banker.api.mapping.annotation.WithdrawalRequestMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.WithdrawalRequestShallow;
import eu.netmobiel.banker.model.PaymentBatch;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
uses = { JavaTimeMapper.class, UserMapper.class, WithdrawalRequestMapper.class })
@PaymentBatchMapperQualifier
public interface PaymentBatchMapper {

	// Domain --> API
	@Mapping(target = "createdBy", source = "createdBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "modifiedBy", source = "modifiedBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "withdrawalRequests", ignore = true)
	@PaymentBatchShallow
	eu.netmobiel.banker.api.model.PaymentBatch mapShallow(PaymentBatch source);

	@Mapping(target = "createdBy", source = "createdBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "modifiedBy", source = "modifiedBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "withdrawalRequests", source = "withdrawalRequests", qualifiedBy = { WithdrawalRequestMapperQualifier.class, WithdrawalRequestShallow.class })
	@PaymentBatchWithdrawals
	eu.netmobiel.banker.api.model.PaymentBatch mapWithWithdrawals(PaymentBatch source);

}
