package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.UserOnlyDetails;
import eu.netmobiel.banker.api.mapping.annotation.WithdrawalRequestMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.WithdrawalRequestPaymentBatch;
import eu.netmobiel.banker.api.mapping.annotation.WithdrawalRequestShallow;
import eu.netmobiel.banker.model.WithdrawalRequest;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
uses = { JavaTimeMapper.class, UserMapper.class })
@WithdrawalRequestMapperQualifier
public interface WithdrawalRequestMapper {

	// Domain --> API
	@Mapping(target = "createdBy", source = "createdBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "settledBy", source = "settledBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "accountName", source = "account.name")
	@Mapping(target = "paymentBatchRef", ignore = true)
	@WithdrawalRequestShallow
	eu.netmobiel.banker.api.model.WithdrawalRequest mapShallow(WithdrawalRequest source);

	@Mapping(target = "createdBy", source = "createdBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "settledBy", source = "settledBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "accountName", source = "account.name")
	@Mapping(target = "paymentBatchRef", source = "paymentBatch.reference")
	@WithdrawalRequestPaymentBatch
	eu.netmobiel.banker.api.model.WithdrawalRequest mapWithPaymentBatch(WithdrawalRequest source);

}
