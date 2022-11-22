package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.banker.api.mapping.annotation.CharityDetails;
import eu.netmobiel.banker.api.mapping.annotation.CharityMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.DonationDetails;
import eu.netmobiel.banker.api.mapping.annotation.DonationMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.DonationWithCharity;
import eu.netmobiel.banker.api.mapping.annotation.DonationWithUser;
import eu.netmobiel.banker.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.banker.api.mapping.annotation.UserOnlyDetails;
import eu.netmobiel.banker.model.Donation;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class, GeometryMapper.class, CharityMapper.class, UserMapper.class })
@DonationMapperQualifier
public interface DonationMapper {
	// Domain --> API
	@Mapping(target = "donor", ignore = true)
	@Mapping(target = "charity", ignore = true)
	@DonationDetails
	eu.netmobiel.banker.api.model.Donation mapPlain(Donation source);
	
	@Mapping(target = "donor", ignore = true)
	@Mapping(target = "charity", source = "charity", qualifiedBy = { CharityMapperQualifier.class, CharityDetails.class})
	@Mapping(target = "donorRef", ignore = true)
	@DonationWithCharity
	eu.netmobiel.banker.api.model.Donation mapWithCharity(Donation source);

	@Mapping(target = "donor", source = "user", qualifiedBy = { UserMapperQualifier.class, UserOnlyDetails.class})
	@Mapping(target = "charity", ignore = true)
	@Mapping(target = "charityRef", ignore = true)
	@DonationWithUser
	eu.netmobiel.banker.api.model.Donation mapWithUser(Donation source);

	// API --> Domain
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "charity", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "donationTime", ignore = true)
	Donation map(eu.netmobiel.banker.api.model.Donation source); 
}
