package eu.netmobiel.banker.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;

/**
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface GeometryMapper {
    // Location <--> GeoLocation
    @Mapping(target = "point", ignore = true)
    GeoLocation map(eu.netmobiel.banker.api.model.Location source);

}
