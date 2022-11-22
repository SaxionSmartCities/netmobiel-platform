package eu.netmobiel.to.rideshare.api.mapping;

import org.mapstruct.BeanMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.tomp.api.model.Place;

/**
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class GeometryMapper {

	// Domain --> API
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "name", source = "label")
	@Mapping(target = "coordinates.lat", source = "latitude")
	@Mapping(target = "coordinates.lng", source = "longitude")
	public abstract Place map(GeoLocation location);

	// API --> Domain
	@InheritInverseConfiguration
	@Mapping(target = "point", ignore = true)
	public abstract GeoLocation map(Place source);
}
