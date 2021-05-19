package eu.netmobiel.profile.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.profile.api.mapping.annotation.DelegationMapperQualifier;
import eu.netmobiel.profile.api.mapping.annotation.Secondary;
import eu.netmobiel.profile.api.mapping.annotation.Shallow;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.model.Profile;

/**
 * This mapper defines the mapping from the domain Booking to the API Booking as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class, ProfileMapper.class })
@DelegationMapperQualifier
public abstract class DelegationMapper {

	@Mapping(target = "data", source = "data", qualifiedBy = { Shallow.class } )
	public abstract eu.netmobiel.profile.api.model.Page map(PagedResult<Delegation> source);

	@Mapping(target = "data", source = "data", qualifiedBy = { Secondary.class } )
	public abstract eu.netmobiel.profile.api.model.Page mapWithShallowProfiles(PagedResult<Delegation> source);

	// Domain --> API
	@Mapping(target = "delegate", ignore = true)
	@Mapping(target = "delegator", ignore = true)
	@Shallow
	public abstract eu.netmobiel.profile.api.model.Delegation map(Delegation source);

	@Mapping(target = "delegate", source = "delegate", qualifiedBy = { Secondary.class })
	@Mapping(target = "delegator", source = "delegator", qualifiedBy = { Secondary.class })
	@Secondary
	public abstract eu.netmobiel.profile.api.model.Delegation mapWithShallowProfiles(Delegation source);

	// API --> Domain
	@Mapping(target = "delegate", source = "delegateRef")
	@Mapping(target = "delegator", source = "delegatorRef")
	public abstract Delegation mapApi(eu.netmobiel.profile.api.model.Delegation source);

	/**
	 * Maps the delegateRef and delegatorRef to a profile instance. Supported are a plain profile id, keycloak managed identity urn and a profile urn.
	 * @param profileRef
	 * @return
	 */
	public Profile mapProfileRef(String profileRef) {
		if (profileRef == null) {
			return null;
		}
		Profile profile = new Profile();
    	if (UrnHelper.isUrn(profileRef)) {
        	NetMobielModule module = NetMobielModule.getEnum(UrnHelper.getService(profileRef));
        	if (module == NetMobielModule.PROFILE) {
    			Long id = UrnHelper.getId(Profile.URN_PREFIX, profileRef);
    			profile.setId(id);
        	} else if (module == NetMobielModule.KEYCLOAK) {
        		String managedIdentity = UrnHelper.getSuffix(profileRef);
    			profile.setManagedIdentity(managedIdentity);
        	}
    	} else if (UrnHelper.isKeycloakManagedIdentity(profileRef)) {
			profile.setManagedIdentity(profileRef);
    	} else {
			Long id = UrnHelper.getId(profileRef);
			profile.setId(id);
    	}
		return profile;
	}
}
