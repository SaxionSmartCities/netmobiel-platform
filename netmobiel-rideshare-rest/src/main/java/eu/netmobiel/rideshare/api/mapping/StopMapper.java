package eu.netmobiel.rideshare.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.model.Stop;

/**
 * This mapper defines the mapping from the domain Stop to the API Stop as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface StopMapper {

	// Stop <--> Stop
	eu.netmobiel.rideshare.api.model.Stop map(Stop source);

	@Mapping(target = "ride", ignore = true)
	@Mapping(target = "location", ignore = true)
	@Mapping(target = "id", ignore = true)
	Stop map(eu.netmobiel.rideshare.api.model.Stop source);

}
