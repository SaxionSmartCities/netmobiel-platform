package eu.netmobiel.profile.repository.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;

/**
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class GeometryMapper {
    // Location --> GeoLocation
	public  GeoLocation map(eu.netmobiel.profile.api.model.Location source) {
		if (source == null) {
			return null;
		}
    	return new GeoLocation(source.getCoordinates().get(1), source.getCoordinates().get(0));
    }

}
