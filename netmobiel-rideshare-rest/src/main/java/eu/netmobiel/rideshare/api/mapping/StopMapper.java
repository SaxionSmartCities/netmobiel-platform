package eu.netmobiel.rideshare.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.rideshare.model.Stop;

/**
 * This mapper defines the mapping from the domain Stop to the API Stop as defined by OpenAPI.
 * 
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class })
public abstract class StopMapper {

	// Domain Stop --> API Stop
	public abstract eu.netmobiel.rideshare.api.model.Stop map(Stop source);

}
