package eu.netmobiel.geoservice.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.here.search.api.model.AccessResponseCoordinate;
import eu.netmobiel.here.search.api.model.DisplayResponseCoordinate;

/**
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class GeometryMapper {

    // AccessResponseCoordinate --> Location
	@Mapping(target = "latitude", source = "lat")
	@Mapping(target = "longitude", source = "lng")
	public abstract eu.netmobiel.geoservice.api.model.Location map(AccessResponseCoordinate source);

    // DisplayResponseCoordinate --> Location
	@Mapping(target = "latitude", source = "lat")
	@Mapping(target = "longitude", source = "lng")
	public abstract eu.netmobiel.geoservice.api.model.Location map(DisplayResponseCoordinate source);
}
