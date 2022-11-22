package eu.netmobiel.planner.api.mapping;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;

import eu.netmobiel.planner.api.mapping.annotation.UserMapperQualifier;
import eu.netmobiel.planner.api.model.ModalityCount.ModalityEnum;
import eu.netmobiel.planner.model.ModalityUsage;
import eu.netmobiel.planner.model.TraverseMode;

/**
 * This mapper defines the mapping from the domain User to the API User as defined by OpenAPI
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN) 
@UserMapperQualifier
public interface UserMapper {

	List<eu.netmobiel.planner.api.model.ModalityCount> map(List<ModalityUsage> source);
	
	@ValueMappings({
        @ValueMapping(target = MappingConstants.NULL, source = MappingConstants.ANY_REMAINING),
    })
    public abstract ModalityEnum map(TraverseMode source);

}
