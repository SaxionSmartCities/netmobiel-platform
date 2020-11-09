package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.AccountAll;
import eu.netmobiel.banker.api.mapping.annotation.AccountMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.AccountMinimal;
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
uses = { JavaTimeMapper.class, UserMapper.class, AccountMapper.class })
@WithdrawalRequestMapperQualifier
public interface WithdrawalRequestMapper {

	// Domain --> API
	@Mapping(target = "createdBy", source = "createdBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "modifiedBy", source = "modifiedBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "paymentBatchRef", ignore = true)
	@Mapping(target = "account", source = "account", qualifiedBy = { AccountMapperQualifier.class, AccountMinimal.class })
	@WithdrawalRequestShallow
	eu.netmobiel.banker.api.model.WithdrawalRequest mapShallow(WithdrawalRequest source);

	@Mapping(target = "createdBy", source = "createdBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "modifiedBy", source = "modifiedBy", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class })
	@Mapping(target = "account", source = "account", qualifiedBy = { AccountMapperQualifier.class, AccountMinimal.class })
	@WithdrawalRequestPaymentBatch
	eu.netmobiel.banker.api.model.WithdrawalRequest mapWithPaymentBatch(WithdrawalRequest source);

}
